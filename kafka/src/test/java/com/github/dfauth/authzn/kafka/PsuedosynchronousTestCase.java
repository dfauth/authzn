package com.github.dfauth.authzn.kafka;

import com.github.dfauth.authzn.ImmutableSubject;
import com.github.dfauth.authzn.Subject;
import com.github.dfauth.authzn.User;
import com.github.dfauth.authzn.avro.AvroSerialization;
import com.github.dfauth.authzn.avro.EnvelopeHandler;
import com.github.dfauth.authzn.avro.MetadataEnvelope;
import com.github.dfauth.authzn.utils.AbstractProcessor;
import com.github.dfauth.jwt.JWTBuilder;
import com.github.dfauth.jwt.JWTVerifier;
import com.github.dfauth.jwt.KeyPairFactory;
import com.github.dfauth.kafka.EmbeddedKafkaTest;
import com.github.dfauth.kafka.proxy.*;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import org.reactivestreams.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.security.KeyPair;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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

        MockAuthenticationService dummyAuthSvc = new MockAuthenticationService();

        DummyService dummySvc = new DummyService();

        withEmbeddedKafka(p -> tryCatch(() -> {

            ServiceProxy serviceProxy = new ServiceProxy(system(), materializer());

            String brokerList = p.getProperty("bootstrap.servers");

            String schemaRegUrl = "http://localhost:8080";

            AvroSerialization avroSerialization = AvroSerialization.of(schemaRegClient, schemaRegUrl);

            Template<LoginRequest, LoginResponse, com.github.dfauth.avro.authzn.LoginRequest, com.github.dfauth.avro.authzn.LoginResponse> template =
                    new Template<LoginRequest, LoginResponse, com.github.dfauth.avro.authzn.LoginRequest, com.github.dfauth.avro.authzn.LoginResponse>() {
                        @Override
                        public EnvelopeHandlers<com.github.dfauth.avro.authzn.LoginRequest, com.github.dfauth.avro.authzn.LoginResponse> envelopeHandlers() {
                            return new EnvelopeHandlers<com.github.dfauth.avro.authzn.LoginRequest, com.github.dfauth.avro.authzn.LoginResponse>(){
                                @Override
                                public EnvelopeHandler<com.github.dfauth.avro.authzn.LoginRequest> inbound(AvroSerialization avroSerialization) {
                                    return EnvelopeHandler.of(avroSerialization, com.github.dfauth.avro.authzn.LoginRequest.class);
                                }

                                @Override
                                public EnvelopeHandler<com.github.dfauth.avro.authzn.LoginResponse> outbound(AvroSerialization avroSerialization) {
                                    return EnvelopeHandler.of(avroSerialization, com.github.dfauth.avro.authzn.LoginResponse.class);
                                }
                            };
                        }

                        @Override
                        public RequestTransformations<LoginRequest, com.github.dfauth.avro.authzn.LoginRequest> requestTransformations() {
                            return new TestTransformations.LoginRequestTransformations();
                        }

                        @Override
                        public ResponseTransformations<com.github.dfauth.avro.authzn.LoginResponse, LoginResponse> responseTransformations() {
                            return new TestTransformations.LoginResponseTransformations();
                        }
                    };

            ServiceProxy.Service service = serviceProxy.createService(asProcessor(dummyAuthSvc), p, brokerList, "client", avroSerialization, authTopicRequest, authTopicResponse);
            service.bindToKafka(template);

            ServiceProxy.Client client = serviceProxy.createClient(p, brokerList, "client", avroSerialization, authTopicRequest, authTopicResponse);
            Function<MetadataEnvelope<LoginRequest>, CompletableFuture<MetadataEnvelope<LoginResponse>>> w = client.asyncProxy(template);

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

    private Processor<MetadataEnvelope<LoginRequest>, MetadataEnvelope<LoginResponse>> asProcessor(MockAuthenticationService svc) {
        return new AbstractProcessor<MetadataEnvelope<LoginRequest>, MetadataEnvelope<LoginResponse>>(){

            @Override
            protected MetadataEnvelope<LoginResponse> transform(MetadataEnvelope<LoginRequest> envelope) {
                return new MetadataEnvelope<>(svc.serviceCall(envelope.getPayload()), withCorrelationIdFrom(envelope));
            }

        };
    }
}
