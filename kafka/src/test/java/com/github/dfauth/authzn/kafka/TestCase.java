package com.github.dfauth.authzn.kafka;

import akka.kafka.ConsumerSettings;
import akka.kafka.ProducerSettings;
import akka.kafka.Subscription;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.stream.javadsl.Source;
import com.github.dfauth.authzn.*;
import com.github.dfauth.authzn.avro.EnvelopeHandler;
import com.github.dfauth.authzn.avro.SpecificRecordDeserializer;
import com.github.dfauth.authzn.avro.SpecificRecordSerializer;
import com.github.dfauth.avro.authzn.Envelope;
import com.github.dfauth.jwt.JWTBuilder;
import com.github.dfauth.jwt.KeyPairFactory;
import com.github.dfauth.kafka.AuthorizationPolicySink;
import com.github.dfauth.kafka.EmbeddedKafkaTest;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.security.KeyPair;

import static com.github.dfauth.authzn.Assertions.assertAllowed;
import static com.github.dfauth.authzn.Assertions.assertDenied;
import static com.github.dfauth.authzn.PrincipalType.ROLE;
import static com.github.dfauth.authzn.PrincipalType.USER;
import static com.github.dfauth.authzn.Role.role;
import static com.github.dfauth.authzn.avro.Transformations.fromAvro;
import static com.github.dfauth.authzn.avro.Transformations.toAvro;
import static com.github.dfauth.authzn.utils.TryCatchUtils.tryCatch;
import static java.lang.Thread.sleep;

public class TestCase extends EmbeddedKafkaTest {

    private static final Logger logger = LoggerFactory.getLogger(TestCase.class);

    private Subject adminSubject = ImmutableSubject.of(USER.of("wilma"), ROLE.of("admin"));
    private Subject userSubject = ImmutableSubject.of(USER.of("fred"), ROLE.of("user"));
    private String topic = "authzn";

    private ImmutableMap<String, String> wilmaTokenMetadata;
    private ImmutableMap<String, String> fredTokenMetadata;

    private ResourcePath authorizationPolicy = new ResourcePath("/authorizationPolicy");
    private ResourcePath blahResourcePath = new ResourcePath("/blah");

    private Permission permission = new TmpPermission();

    @BeforeTest
    public void setUp() {
        {
            // generate an admin token for updating the directives
            KeyPair testKeyPair = KeyPairFactory.createKeyPair("RSA", 2048);
            String issuer = "me";
            JWTBuilder jwtBuilder = new JWTBuilder(issuer, testKeyPair.getPrivate());
            User adminUser = User.of("wilma", "flintstone", role("test:admin"), role("test:user"));
            String token = jwtBuilder.forSubject(adminUser.getUserId()).withClaim("roles", adminUser.getRoles()).build();
            wilmaTokenMetadata = ImmutableMap.of("token", token);
        }
        {
            // generate an user token for updating the directives
            KeyPair testKeyPair = KeyPairFactory.createKeyPair("RSA", 2048);
            String issuer = "me";
            JWTBuilder jwtBuilder = new JWTBuilder(issuer, testKeyPair.getPrivate());
            User adminUser = User.of("fred", "flintstone", role("test:user"));
            String token = jwtBuilder.forSubject(adminUser.getUserId()).withClaim("roles", adminUser.getRoles()).build();
            fredTokenMetadata = ImmutableMap.of("token", token);
        }
    }

    @Test
    public void testIt() {

        withEmbeddedKafka(p -> tryCatch(() -> {

            AuthorizationPolicySink policy = new AuthorizationPolicySink();

            String brokerList = p.getProperty("bootstrap.servers");

            SchemaRegistryClient schemaRegClient = new MockSchemaRegistryClient();
            String schemaRegUrl = "http://localhost:8080";

            SpecificRecordSerializer<com.github.dfauth.avro.authzn.Directive> serializer = SpecificRecordSerializer.Builder.builder()
                    .withSchemaRegistryClient(schemaRegClient)
                    .withSchemaRegistryURL(schemaRegUrl)
                    .build();

            SpecificRecordSerializer<Envelope> envelopeSerializer = SpecificRecordSerializer.Builder.builder()
                    .withSchemaRegistryClient(schemaRegClient)
                    .withSchemaRegistryURL(schemaRegUrl)
                    .build();

            SpecificRecordDeserializer<com.github.dfauth.avro.authzn.Directive> deserializer = SpecificRecordDeserializer.Builder.builder()
                    .withSchemaRegistryClient(schemaRegClient)
                    .withSchemaRegistryURL(schemaRegUrl)
                    .build();

            SpecificRecordDeserializer<Envelope> envelopeDeserializer = SpecificRecordDeserializer.Builder.builder()
                    .withSchemaRegistryClient(schemaRegClient)
                    .withSchemaRegistryURL(schemaRegUrl)
                    .build();


            EnvelopeHandler<com.github.dfauth.avro.authzn.Directive> envelopeHandler = new EnvelopeHandler(serializer, deserializer);

            Config producerConfig = ConfigFactory.load().getConfig("akka.kafka.producer");
            ProducerSettings<String, com.github.dfauth.avro.authzn.Directive> producerSettings = ProducerSettings.apply(producerConfig, new StringSerializer(), serializer)
                    .withBootstrapServers(brokerList)
                    .withProperty("group.id", "fred");


            Config consumerConfig = ConfigFactory.load().getConfig("akka.kafka.consumer");
            ConsumerSettings<String, Envelope> consumerSettings = ConsumerSettings.apply(system(), new StringDeserializer(), envelopeDeserializer)
                    .withBootstrapServers(brokerList)
                    .withGroupId("wilma")
                    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerConfig.getString("auto.offset.reset"));

            Subscription subscription = Subscriptions.assignment(new TopicPartition(topic, 0));
            Consumer.plainSource(consumerSettings, subscription)
                    .map(r -> r.value())
                    .map(e -> envelopeHandler.extractRecordWithMetadata(e))
                    .map(m -> m.map(fromAvro))
                    .to(policy.asSink())
                    .run(materializer());

            // and empty policy should not allow anything
            assertDenied(policy.permit(adminSubject, permission));
            assertDenied(policy.permit(userSubject, permission));

            // publish a top level directive restricting access to administrators
            Source.single(Directive.builder().withPrincipal(ROLE.of("admin")).withResource(blahResourcePath).build())
                    .map(e -> toAvro.apply(e))
                    .map(d -> envelopeHandler.envelope(d, wilmaTokenMetadata))
                    .to(KafkaSink.createSink(topic, p, envelopeSerializer))
                    .run(materializer());

            sleep(1000);
            assertAllowed(policy.permit(adminSubject, permission));
            assertDenied(policy.permit(userSubject, permission));

            // publish a top level directive restricting access to administrators
            Source.single(Directive.builder().withPrincipal(ROLE.of("user")).withResource(blahResourcePath).build())
                    .map(e -> toAvro.apply(e))
                    .map(d -> envelopeHandler.envelope(d, wilmaTokenMetadata))
                    .to(KafkaSink.createSink(topic, p, envelopeSerializer))
                    .run(materializer());

            sleep(1000);
            assertAllowed(policy.permit(adminSubject, permission));
            assertAllowed(policy.permit(userSubject, permission));
            return null;
        }));

    }

    private class PolicyPermission extends Permission {
        public PolicyPermission() {
            super(new ResourcePath("authorizationPolicy"));
        }
    }

    private class TmpPermission extends Permission {
        public TmpPermission() {
            super(blahResourcePath);
        }
    }
}
