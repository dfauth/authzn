package com.github.dfauth.authzn.kafka;

import akka.kafka.ConsumerSettings;
import akka.kafka.Subscription;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.stream.javadsl.Source;
import com.github.dfauth.authzn.*;
import com.github.dfauth.authzn.avro.EnvelopeHandler;
import com.github.dfauth.authzn.avro.MetadataEnvelope;
import com.github.dfauth.authzn.avro.SpecificRecordDeserializer;
import com.github.dfauth.authzn.avro.SpecificRecordSerializer;
import com.github.dfauth.avro.authzn.Envelope;
import com.github.dfauth.jwt.JWTBuilder;
import com.github.dfauth.jwt.JWTVerifier;
import com.github.dfauth.jwt.KeyPairFactory;
import com.github.dfauth.kafka.AuthorizationPolicySink;
import com.github.dfauth.kafka.EmbeddedKafkaTest;
import com.github.dfauth.kafka.PolicyPermission;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.security.KeyPair;
import java.util.Collections;
import java.util.Map;

import static com.github.dfauth.authzn.Assertions.assertAllowed;
import static com.github.dfauth.authzn.Assertions.assertDenied;
import static com.github.dfauth.authzn.PrincipalType.ROLE;
import static com.github.dfauth.authzn.PrincipalType.USER;
import static com.github.dfauth.authzn.Role.role;
import static com.github.dfauth.authzn.avro.Transformations.fromAvro;
import static com.github.dfauth.authzn.avro.Transformations.toAvro;
import static com.github.dfauth.authzn.kafka.AuthenticationUtils.authenticate;
import static com.github.dfauth.authzn.utils.TryCatchUtils.tryCatch;
import static java.lang.Thread.sleep;

public class TestCase extends EmbeddedKafkaTest {

    private static final Logger logger = LoggerFactory.getLogger(TestCase.class);

    private Subject adminSubject = ImmutableSubject.of(USER.of("wilma"), ROLE.of("admin"));
    private Subject userSubject = ImmutableSubject.of(USER.of("fred"), ROLE.of("user"));
    private String topic = "authzn";

    private Map<String, String> wilmaTokenMetadata;
    private Map<String, String> fredTokenMetadata;

    private ResourcePath authorizationPolicy = new ResourcePath("/authorizationPolicy");

    private KeyPair testKeyPair = KeyPairFactory.createKeyPair("RSA", 2048);
    private String issuer = "me";
    private JWTVerifier jwtVerifier = new JWTVerifier(testKeyPair.getPublic(), issuer);

    private Permission permission = new PolicyPermission();

    @BeforeTest
    public void setUp() {
        {
            // generate an admin token for updating the directives
            JWTBuilder jwtBuilder = new JWTBuilder(issuer, testKeyPair.getPrivate());
            User adminUser = User.of("wilma", "flintstone", role("test:admin"), role("test:user"));
            String token = jwtBuilder.forSubject(adminUser.getUserId()).withClaim("roles", adminUser.getRoles()).build();
            wilmaTokenMetadata = MetadataEnvelope.withToken(token);
        }
        {
            // generate an user token for updating the directives
            KeyPair testKeyPair = KeyPairFactory.createKeyPair("RSA", 2048);
            String issuer = "me";
            JWTBuilder jwtBuilder = new JWTBuilder(issuer, testKeyPair.getPrivate());
            User adminUser = User.of("fred", "flintstone", role("test:user"));
            String token = jwtBuilder.forSubject(adminUser.getUserId()).withClaim("roles", adminUser.getRoles()).build();
            fredTokenMetadata = MetadataEnvelope.withToken(token);
        }
    }

    @Test
    public void testIt() {

        withEmbeddedKafka(p -> tryCatch(() -> {

            // on startup, policy engine will be empty, so we prime it with an initial directive that will be checked as others
            // are added from kafka
            Directive defaultDirective = Directive.builder().withPrincipal(ROLE.of("admin")).withResource(authorizationPolicy).build();

            AuthorizationPolicySink policy = new AuthorizationPolicySink(defaultDirective);

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

            Config consumerConfig = ConfigFactory.load().getConfig("akka.kafka.consumer");
            ConsumerSettings<String, Envelope> consumerSettings = ConsumerSettings.apply(system(), new StringDeserializer(), envelopeDeserializer)
                    .withBootstrapServers(brokerList)
                    .withGroupId("wilma")
                    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerConfig.getString("auto.offset.reset"));

            Subscription subscription = Subscriptions.assignment(new TopicPartition(topic, 0));
            Consumer.plainSource(consumerSettings, subscription)
                    .map(r -> r.value())
                    .map(e -> envelopeHandler.extractRecordWithMetadata(e))
                    .map(m -> m.mapPayload(fromAvro))
                    .map(m -> authenticate(jwtVerifier, Directive.class).apply(m))
                    .filter(_try -> {
                        if(_try.isSuccess()) {
                            return true;
                        } else {
                            logger.error("authentication failed: "+_try);
                            return false;
                        }
                    })
                    .map(_try -> _try.get())
                    .to(policy.asSink())
                    .run(materializer());

            // the initial directive allowed only admins
            assertAllowed(policy.permit(adminSubject, permission));
            assertDenied(policy.permit(userSubject, permission));

            // publish a top level directive adding access for all users, but supply empty metatdata on publishing (ie. no token in the published envelope)
            Source.single(new MetadataEnvelope(
                            Directive.builder().withPrincipal(ROLE.of("user")).withResource(authorizationPolicy).build(),
                            Collections.emptyMap()
                    ))
                    .map(e -> e.mapPayload(toAvro))
                    .map(e -> envelopeHandler.envelope(e))
                    .to(KafkaSink.createSink(topic, p, envelopeSerializer))
                    .run(materializer());

            sleep(1000);

            // should see no change as directive was not authenticated
            assertAllowed(policy.permit(adminSubject, permission));
            assertDenied(policy.permit(userSubject, permission));

            // repeat, but with the token of an authenticated user, NOT having admin role
            Source.single(new MetadataEnvelope(
                            Directive.builder().withPrincipal(ROLE.of("user")).withResource(authorizationPolicy).build(),
                            fredTokenMetadata
                    ))
                    .map(e -> e.mapPayload(toAvro))
                    .map(e -> envelopeHandler.envelope(e))
                    .to(KafkaSink.createSink(topic, p, envelopeSerializer))
                    .run(materializer());

            sleep(1000);

            // should see no change as directive was authenticated, but not authorized
            assertAllowed(policy.permit(adminSubject, permission));
            assertDenied(policy.permit(userSubject, permission));

            // publish a top level directive allowing the role 'user'
            Source.single(new MetadataEnvelope(
                            Directive.builder().withPrincipal(ROLE.of("user")).withResource(authorizationPolicy).build(),
                            wilmaTokenMetadata // this time we publish with the token of someone with 'admin' role
                    ))
                    .map(e -> e.mapPayload(toAvro))
                    .map(e -> envelopeHandler.envelope(e))
                    .to(KafkaSink.createSink(topic, p, envelopeSerializer))
                    .run(materializer());

            sleep(1000);
            assertAllowed(policy.permit(adminSubject, permission));
            assertAllowed(policy.permit(userSubject, permission));
            return null;
        }));

    }
}
