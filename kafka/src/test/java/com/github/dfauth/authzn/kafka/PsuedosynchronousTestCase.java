package com.github.dfauth.authzn.kafka;

import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.github.dfauth.authzn.ImmutableSubject;
import com.github.dfauth.authzn.Subject;
import com.github.dfauth.authzn.User;
import com.github.dfauth.authzn.avro.EnvelopeHandler;
import com.github.dfauth.authzn.avro.MetadataEnvelope;
import com.github.dfauth.authzn.avro.SpecificRecordDeserializer;
import com.github.dfauth.authzn.avro.SpecificRecordSerializer;
import com.github.dfauth.authzn.utils.AbstractProcessor;
import com.github.dfauth.avro.authzn.Envelope;
import com.github.dfauth.jwt.JWTBuilder;
import com.github.dfauth.jwt.JWTVerifier;
import com.github.dfauth.jwt.KeyPairFactory;
import com.github.dfauth.kafka.EmbeddedKafkaTest;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.reactivestreams.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.security.KeyPair;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static com.github.dfauth.authzn.PrincipalType.ROLE;
import static com.github.dfauth.authzn.PrincipalType.USER;
import static com.github.dfauth.authzn.Role.role;
import static com.github.dfauth.authzn.avro.MetadataEnvelope.withCorrelationIdFrom;
import static com.github.dfauth.authzn.utils.TryCatchUtils.tryCatch;
import static java.lang.Thread.sleep;
import static org.testng.Assert.assertNotNull;

public class PsuedosynchronousTestCase extends EmbeddedKafkaTest {

    private static final Logger logger = LoggerFactory.getLogger(PsuedosynchronousTestCase.class);

    private Subject adminSubject = ImmutableSubject.of(USER.of("wilma"), ROLE.of("admin"));
    private Subject userSubject = ImmutableSubject.of(USER.of("fred"), ROLE.of("user"));
    private String authTopicRequest = "auth-request";
    private String authTopicResponse = "auth-response";
    private String sampleTopicRequest = "sample-request";
    private String samplTopicResponse = "sample-response";

    private Map<String, String> wilmaTokenMetadata;
    private Map<String, String> fredTokenMetadata;

    private KeyPair testKeyPair = KeyPairFactory.createKeyPair("RSA", 2048);
    private String issuer = "me";
    private JWTVerifier jwtVerifier = new JWTVerifier(testKeyPair.getPublic(), issuer);
    private SchemaRegistryClient schemaRegClient = new MockSchemaRegistryClient();

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

        DummyAuthenticationService dummyAuthSvc = new DummyAuthenticationService();

        DummyService dummySvc = new DummyService();

        withEmbeddedKafka(p -> tryCatch(() -> {

            String brokerList = p.getProperty("bootstrap.servers");

            String schemaRegUrl = "http://localhost:8080";

            bindToKafka(dummyAuthSvc, p, brokerList, this.getClass().getCanonicalName(), schemaRegUrl, authTopicRequest, authTopicResponse);

            Function<MetadataEnvelope<LoginRequest>, CompletableFuture<MetadataEnvelope<LoginResponse>>> w = asyncProxy(dummyAuthSvc, p, brokerList, this.getClass().getCanonicalName(), schemaRegUrl, authTopicRequest, authTopicResponse);

            sleep(1000);

            CompletableFuture<MetadataEnvelope<LoginResponse>> f = w.apply(new MetadataEnvelope(
                    LoginRequest.builder().withUsername("fred").withPasswordHash("blah").withRandom("blah2").build()));

            f.thenAccept(r -> {
               logger.info("response: "+r);
            });

            // wait on this future
            MetadataEnvelope<LoginResponse> response = f.get();
            assertNotNull(response);
            assertNotNull(response.getPayload().getToken());

            return null;
        }));

    }

    private <T> SpecificRecordSerializer<T> _serializer(String schemaRegUrl, Class<T> _ignored) {
        return _serializer(schemaRegUrl);
    }

    private <T> SpecificRecordSerializer<T> _serializer(String schemaRegUrl) {
        return SpecificRecordSerializer.Builder.builder()
                .withSchemaRegistryClient(schemaRegClient)
                .withSchemaRegistryURL(schemaRegUrl)
                .build();
    }

    private <T> SpecificRecordDeserializer<T> _deserializer(String schemaRegUrl, Class<T> _ignored) {
        return _deserializer(schemaRegUrl);
    }

    private <T> SpecificRecordDeserializer<T> _deserializer(String schemaRegUrl) {
        return SpecificRecordDeserializer.Builder.builder()
                .withSchemaRegistryClient(schemaRegClient)
                .withSchemaRegistryURL(schemaRegUrl)
                .build();
    }

    private Function<MetadataEnvelope<LoginRequest>, CompletableFuture<MetadataEnvelope<LoginResponse>>> asyncProxy(DummyAuthenticationService svc,
                                         Properties props,
                                         String brokerList,
                                         String groupId,
                                         String schemaRegUrl,
                                         String inTopic,
                                         String outTopic) {

        Config consumerConfig = ConfigFactory.load().getConfig("akka.kafka.consumer");

        SpecificRecordDeserializer<Envelope> envelopeDeserializer = _deserializer(schemaRegUrl);
        SpecificRecordSerializer<Envelope> envelopeSerializer = _serializer(schemaRegUrl);

        EnvelopeHandler<com.github.dfauth.avro.authzn.LoginRequest> inEnvelopeHandler = new EnvelopeHandler(
                _serializer(schemaRegUrl, com.github.dfauth.avro.authzn.LoginRequest.class),
                _deserializer(schemaRegUrl, com.github.dfauth.avro.authzn.LoginRequest.class));

        EnvelopeHandler<com.github.dfauth.avro.authzn.LoginResponse> outEnvelopeHandler = new EnvelopeHandler(
                _serializer(schemaRegUrl, com.github.dfauth.avro.authzn.LoginResponse.class),
                _deserializer(schemaRegUrl, com.github.dfauth.avro.authzn.LoginResponse.class));

        ConsumerSettings<String, Envelope> consumerSettings = ConsumerSettings.apply(system(), new StringDeserializer(), envelopeDeserializer)
                .withBootstrapServers(brokerList)
                .withGroupId(groupId)
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerConfig.getString("auto.offset.reset"));

        return i -> {

            AtomicReference<String> correlationId = new AtomicReference<>();

            CompletableFuture<MetadataEnvelope<LoginResponse>> f = new CompletableFuture<>();

            Source.single(i)
                    .wireTap(m -> {
                        TemporalAmount timeout = Optional.ofNullable(m.getMetadata().get(MetadataEnvelope.TIMEOUT))
                                .map(s -> (TemporalAmount) Duration.ofMillis(Long.parseLong(s)))
                                .orElse(consumerConfig.getTemporal("timeout"));
                        Timer t = new Timer();
                        t.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                f.completeExceptionally(new TimeoutException("timed out after " + timeout));
                            }
                        }, timeout.get(ChronoUnit.SECONDS) * 1000);

                    })
                    .map(m -> i.correlationId().map(k -> {
                        correlationId.set(k);
                        return i;
                    }).orElseGet(() -> {
                        correlationId.set(UUID.randomUUID().toString());
                        return m.withCorrelationId(correlationId.get());
                    }))
                    .map(m -> m.mapPayload(TestTransformations.LoginRequestTransformations.toAvro))
                    .map(e -> inEnvelopeHandler.envelope(e))
                    .to(KafkaSink.createSink(inTopic, props, envelopeSerializer))
                    .run(materializer());

            Consumer.plainSource(consumerSettings, Subscriptions.assignment(new TopicPartition(outTopic, 0)))
                    .map(r -> r.value())
                    .map(e -> outEnvelopeHandler.extractRecordWithMetadata(e))
                    .filter(m -> m.correlationId().map(j -> correlationId.get().equals(j)).orElse(false))
                    .map(m -> m.mapPayload(TestTransformations.LoginResponseTransformations.fromAvro))
                    .to(Sink.foreach(r -> {
                        f.complete(r);
                    }))
                    .run(materializer());

            return f;
        };
    }

    private void bindToKafka(DummyAuthenticationService svc,
                                         Properties props,
                                         String brokerList,
                                         String groupId,
                                         String schemaRegUrl,
                                         String inTopic,
                                         String outTopic) {

        Config consumerConfig = ConfigFactory.load().getConfig("akka.kafka.consumer");

        SpecificRecordDeserializer<Envelope> envelopeDeserializer = _deserializer(schemaRegUrl);
        SpecificRecordSerializer<Envelope> envelopeSerializer = _serializer(schemaRegUrl);

        EnvelopeHandler<com.github.dfauth.avro.authzn.LoginRequest> inEnvelopeHandler = new EnvelopeHandler(
                _serializer(schemaRegUrl, com.github.dfauth.avro.authzn.LoginRequest.class),
                _deserializer(schemaRegUrl, com.github.dfauth.avro.authzn.LoginRequest.class));

        EnvelopeHandler<com.github.dfauth.avro.authzn.LoginResponse> outEnvelopeHandler = new EnvelopeHandler(
                _serializer(schemaRegUrl, com.github.dfauth.avro.authzn.LoginResponse.class),
                _deserializer(schemaRegUrl, com.github.dfauth.avro.authzn.LoginResponse.class));

        ConsumerSettings<String, Envelope> consumerSettings = ConsumerSettings.apply(system(), new StringDeserializer(), envelopeDeserializer)
                .withBootstrapServers(brokerList)
                .withGroupId(groupId)
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerConfig.getString("auto.offset.reset"));

        Processor<MetadataEnvelope<LoginRequest>, MetadataEnvelope<LoginResponse>> processor = asProcessor(svc);

        Consumer.plainSource(consumerSettings, Subscriptions.assignment(new TopicPartition(inTopic, 0)))
                .map(r -> r.value())
                .map(e -> inEnvelopeHandler.extractRecordWithMetadata(e))
                .map(m -> m.mapPayload(TestTransformations.LoginRequestTransformations.fromAvro))
                .to(Sink.fromSubscriber(processor))
                .run(materializer());

        Source.fromPublisher(processor)
                .map(m -> m.mapPayload(TestTransformations.LoginResponseTransformations.toAvro))
                .map(e -> outEnvelopeHandler.envelope(e))
                .to(KafkaSink.createSink(outTopic, props, envelopeSerializer))
                .run(materializer());

    }

    private Processor<MetadataEnvelope<LoginRequest>, MetadataEnvelope<LoginResponse>> asProcessor(DummyAuthenticationService svc) {
        return new AbstractProcessor<MetadataEnvelope<LoginRequest>, MetadataEnvelope<LoginResponse>>(){

            @Override
            protected MetadataEnvelope<LoginResponse> transform(MetadataEnvelope<LoginRequest> envelope) {
                return new MetadataEnvelope<>(svc.serviceCall(envelope.getPayload()), withCorrelationIdFrom(envelope));
            }

        };
    }
}
