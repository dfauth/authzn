package com.github.dfauth.authzn.kafka;

import akka.kafka.ConsumerSettings;
import akka.kafka.ProducerSettings;
import akka.kafka.Subscription;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.kafka.javadsl.Producer;
import akka.stream.javadsl.Source;
import com.github.dfauth.authzn.*;
import com.github.dfauth.authzn.avro.SpecificRecordDeserializer;
import com.github.dfauth.authzn.avro.SpecificRecordSerializer;
import com.github.dfauth.kafka.AuthorizationPolicySink;
import com.github.dfauth.kafka.EmbeddedKafkaTest;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import static com.github.dfauth.authzn.Assertions.assertAllowed;
import static com.github.dfauth.authzn.Assertions.assertDenied;
import static com.github.dfauth.authzn.PrincipalType.ROLE;
import static com.github.dfauth.authzn.PrincipalType.USER;
import static com.github.dfauth.authzn.avro.Transformations.fromAvro;
import static com.github.dfauth.authzn.avro.Transformations.toAvro;
import static com.github.dfauth.authzn.utils.TryCatchUtils.tryCatch;
import static java.lang.Thread.sleep;

public class TestCase extends EmbeddedKafkaTest {

    private static final Logger logger = LoggerFactory.getLogger(TestCase.class);

    private Permission permission = new TmpPermission();
    private Subject adminSubject = ImmutableSubject.of(USER.of("wilma"), ROLE.of("admin"));
    private Subject userSubject = ImmutableSubject.of(USER.of("fred"), ROLE.of("user"));
    private String topic = "authzn";

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

            SpecificRecordDeserializer<com.github.dfauth.avro.authzn.Directive> deserializer = SpecificRecordDeserializer.Builder.builder()
                    .withSchemaRegistryClient(schemaRegClient)
                    .withSchemaRegistryURL(schemaRegUrl)
                    .build();


            Config producerConfig = ConfigFactory.load().getConfig("akka.kafka.producer");
            ProducerSettings<String, com.github.dfauth.avro.authzn.Directive> producerSettings = ProducerSettings.apply(producerConfig, new StringSerializer(), serializer)
                    .withBootstrapServers(brokerList)
                    .withProperty("group.id", "fred");


            Config consumerConfig = ConfigFactory.load().getConfig("akka.kafka.consumer");
            ConsumerSettings<String, com.github.dfauth.avro.authzn.Directive> consumerSettings = ConsumerSettings.apply(system(), new StringDeserializer(), deserializer)
                    .withBootstrapServers(brokerList)
                    .withGroupId("wilma")
                    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerConfig.getString("auto.offset.reset"));

            Subscription subscription = Subscriptions.assignment(new TopicPartition(topic, 0));
            Consumer.plainSource(consumerSettings, subscription)
                    .wireTap(r -> logger.info("WOOZ record: "+r))
                    .map(r -> r.value())
                    .map(d -> fromAvro.apply(d))
                    .to(policy.asSink()).run(materializer());

            // and empty policy should not allow anything
            assertDenied(policy.permit(adminSubject, permission));
            assertDenied(policy.permit(userSubject, permission));

            // publish a top level directive restricting access to administrators
            Source.single(Directive.builder().withPrincipal(ROLE.of("admin")).build())
                            .map(e -> toAvro.apply(e))
            .to(KafkaSink.createSink(topic, p, serializer)).run(materializer());

            sleep(1000);
            assertAllowed(policy.permit(adminSubject, permission));
            assertDenied(policy.permit(userSubject, permission));

            // publish a top level directive restricting access to administrators
            Source.single(Directive.builder().withPrincipal(ROLE.of("user")).build())
                            .map(e -> toAvro.apply(e))
                            .map(v -> new ProducerRecord<String, com.github.dfauth.avro.authzn.Directive>("authzn", v))
                            .runWith(Producer.plainSink(producerSettings), materializer());

            sleep(1000);
            assertAllowed(policy.permit(adminSubject, permission));
            assertAllowed(policy.permit(userSubject, permission));
            return null;
        }));

    }

    private class TmpPermission extends Permission {
        public TmpPermission() {
            super(new ResourcePath("blah"));
        }
    }
}
