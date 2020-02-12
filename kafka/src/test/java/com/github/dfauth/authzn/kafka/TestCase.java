package com.github.dfauth.authzn.kafka;

import akka.kafka.ConsumerSettings;
import akka.kafka.ProducerSettings;
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
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.Collections;

import static com.github.dfauth.authzn.Assertions.assertDenied;
import static com.github.dfauth.authzn.PrincipalType.ROLE;
import static com.github.dfauth.authzn.PrincipalType.USER;
import static com.github.dfauth.authzn.avro.Transformations.fromAvro;
import static com.github.dfauth.authzn.avro.Transformations.toAvro;

public class TestCase extends EmbeddedKafkaTest {

    private static final Logger logger = LoggerFactory.getLogger(TestCase.class);

    private Permission permission = new Permission(){};
    private Subject adminSubject = ImmutableSubject.of(USER.of("wilma"), ROLE.of("admin"));
    private Subject userSubject = ImmutableSubject.of(USER.of("fred"), ROLE.of("user"));

    @Test
    public void testIt() {

        withEmbeddedKafka(p -> {

            AuthorizationPolicySink policy = new AuthorizationPolicySink();

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
                    .withBootstrapServers(p.getProperty("boostrap.servers"));


            Config consumerConfig = ConfigFactory.load().getConfig("akka.kafka.consumer");
            ConsumerSettings<String, com.github.dfauth.avro.authzn.Directive> consumerSettings = ConsumerSettings.apply(consumerConfig, new StringDeserializer(), deserializer)
                    .withBootstrapServers(p.getProperty("boostrap.servers"));

            Consumer.plainSource(consumerSettings, Subscriptions.topics(Collections.singleton("authzn")))
                    .map(r -> r.value())
                    .map(d -> fromAvro.apply(d))
                    .runWith(policy.asSink(), materializer());

            // and empty policy should not allow anything
            assertDenied(policy.permit(adminSubject, permission));

            // publish a top level directive restricting access to users
            Source.single(Directive.builder().withPrincipal(ROLE.of("user")).build())
                            .map(e -> toAvro.apply(e))
                            .map(v -> new ProducerRecord<String, com.github.dfauth.avro.authzn.Directive>("authzn", v))
                            .runWith(Producer.plainSink(producerSettings), materializer());



            return null;
        });

    }
}
