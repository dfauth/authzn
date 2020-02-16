package com.github.dfauth.authzn.kafka;

import com.github.dfauth.authzn.ImmutableSubject;
import com.github.dfauth.authzn.Subject;
import com.github.dfauth.authzn.User;
import com.github.dfauth.authzn.avro.AvroSerialization;
import com.github.dfauth.authzn.avro.MetadataEnvelope;
import com.github.dfauth.authzn.utils.AbstractProcessor;
import com.github.dfauth.avro.authzn.LoginRequest;
import com.github.dfauth.avro.authzn.LoginResponse;
import com.github.dfauth.jwt.JWTBuilder;
import com.github.dfauth.jwt.JWTVerifier;
import com.github.dfauth.jwt.KeyPairFactory;
import com.github.dfauth.kafka.EmbeddedKafkaTest;
import com.github.dfauth.kafka.proxy.ServiceProxy;
import com.github.dfauth.kafka.proxy.templates.AuthenticationTemplate;
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

            ServiceProxy.Service service = serviceProxy.createService(asProcessor(dummyAuthSvc), p, brokerList, "client", avroSerialization, authTopicRequest, authTopicResponse);
            service.bindToKafka(new AuthenticationTemplate());

            ServiceProxy.Client client = serviceProxy.createClient(p, brokerList, "client", avroSerialization, authTopicRequest, authTopicResponse);
            Function<MetadataEnvelope<LoginRequest>, CompletableFuture<MetadataEnvelope<LoginResponse>>> w = client.asyncProxy(new AuthenticationTemplate());

            sleep(1000);

            CompletableFuture<MetadataEnvelope<com.github.dfauth.authzn.domain.LoginResponse>> f = w.apply(new MetadataEnvelope(
                    com.github.dfauth.authzn.domain.LoginRequest.builder().withUsername("fred").withPasswordHash("blah").withRandom("blah2").build()));

            f.thenAccept(r -> {
               logger.info("response: "+r);
            });

            // wait on this future
            MetadataEnvelope<com.github.dfauth.authzn.domain.LoginResponse> response = f.get();
            assertNotNull(response);
            assertNotNull(response.getPayload().getToken());

            return null;
        }));

    }

    private Processor<MetadataEnvelope<com.github.dfauth.authzn.domain.LoginRequest>, MetadataEnvelope<com.github.dfauth.authzn.domain.LoginResponse>> asProcessor(MockAuthenticationService svc) {
        return new AbstractProcessor<MetadataEnvelope<com.github.dfauth.authzn.domain.LoginRequest>, MetadataEnvelope<com.github.dfauth.authzn.domain.LoginResponse>>(){

            @Override
            protected MetadataEnvelope<com.github.dfauth.authzn.domain.LoginResponse> transform(MetadataEnvelope<com.github.dfauth.authzn.domain.LoginRequest> envelope) {
                return new MetadataEnvelope<>(svc.serviceCall(envelope.getPayload()), withCorrelationIdFrom(envelope));
            }

        };
    }
}
