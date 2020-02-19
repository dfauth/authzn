package com.github.dfauth.authzn.kafka;

import com.github.dfauth.authzn.ImmutableSubject;
import com.github.dfauth.authzn.Subject;
import com.github.dfauth.authzn.User;
import com.github.dfauth.authzn.avro.AvroSerialization;
import com.github.dfauth.authzn.avro.MetadataEnvelope;
import com.github.dfauth.authzn.domain.NoOp;
import com.github.dfauth.authzn.domain.UserInfoResponse;
import com.github.dfauth.authzn.kafka.proxy.templates.UserInfoTemplate;
import com.github.dfauth.authzn.utils.AbstractProcessor;
import com.github.dfauth.avro.authzn.LoginRequest;
import com.github.dfauth.avro.authzn.LoginResponse;
import com.github.dfauth.jwt.JWTBuilder;
import com.github.dfauth.jwt.JWTVerifier;
import com.github.dfauth.jwt.KeyPairFactory;
import com.github.dfauth.kafka.AuthenticationEnvelope;
import com.github.dfauth.kafka.EmbeddedKafkaTest;
import com.github.dfauth.kafka.proxy.ServiceProxy;
import com.github.dfauth.kafka.proxy.templates.AuthenticationTemplate;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.vavr.control.Try;
import org.reactivestreams.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.security.KeyPair;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static com.github.dfauth.authzn.PrincipalType.ROLE;
import static com.github.dfauth.authzn.PrincipalType.USER;
import static com.github.dfauth.authzn.Role.role;
import static com.github.dfauth.authzn.avro.MetadataEnvelope.withCorrelationIdFrom;
import static com.github.dfauth.authzn.kafka.AuthenticationUtils.authenticate;
import static com.github.dfauth.authzn.utils.TryCatchUtils.tryCatch;
import static java.lang.Thread.sleep;
import static org.testng.Assert.*;

public class PsuedosynchronousTestCase extends EmbeddedKafkaTest {

    private static final Logger logger = LoggerFactory.getLogger(PsuedosynchronousTestCase.class);

    private Subject adminSubject = ImmutableSubject.of(USER.of("wilma"), ROLE.of("admin"));
    private Subject userSubject = ImmutableSubject.of(USER.of("fred"), ROLE.of("user"));
    private String authTopic = "auth";
    private String userInfoTopic = "userInfo";

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
            JWTBuilder jwtBuilder = new JWTBuilder(issuer, testKeyPair.getPrivate());
            User adminUser = User.of("fred", "flintstone", role("test:user"));
            String token = jwtBuilder.forSubject(adminUser.getUserId()).withClaim("roles", adminUser.getRoles()).build();
            fredTokenMetadata = MetadataEnvelope.withToken(token);
        }
    }

    @Test
    public void testIt() {

        MockAuthenticationService dummyAuthSvc = new MockAuthenticationService();

        withEmbeddedKafka(p -> tryCatch(() -> {

            String brokerList = p.getProperty("bootstrap.servers");

            String schemaRegUrl = "http://localhost:8080";

            AvroSerialization avroSerialization = AvroSerialization.of(schemaRegClient, schemaRegUrl);

            ServiceProxy serviceProxy = null;
            ServiceProxy.Service loginService = null;
            ServiceProxy.Client loginClient = null;

            try {
                serviceProxy = new ServiceProxy(system(), materializer(), p, brokerList, avroSerialization);
                loginService = serviceProxy.createService(asProcessor(dummyAuthSvc), "server", authTopic);
                loginService.bindToKafka(new AuthenticationTemplate());
                loginClient = serviceProxy.createClient(authTopic);
                Function<MetadataEnvelope<LoginRequest>, CompletableFuture<MetadataEnvelope<Try<LoginResponse>>>> loginProxy = loginClient.asyncProxy(new AuthenticationTemplate());

                sleep(1000);

                // make the authentication fail
                CompletableFuture<MetadataEnvelope<Try<com.github.dfauth.authzn.domain.LoginResponse>>> f = loginProxy.apply(new MetadataEnvelope(
                        com.github.dfauth.authzn.domain.LoginRequest.builder().withUsername("fred").withPasswordHash("blah").withRandom("blah2").build()));

                // expect failure
                try {
                    f.get();
                    fail("Oops. expected exception");
                } catch (ExecutionException e) {
                    assertEquals(e.getCause().getMessage(), "Authentication failure for user fred");
                }
            } finally {
                loginClient.close();
                loginService.close();
            }

            try {
                serviceProxy = new ServiceProxy(system(), materializer(), p, brokerList, avroSerialization);
                loginService = serviceProxy.createService(asProcessor(dummyAuthSvc), "server", authTopic);
                loginService.bindToKafka(new AuthenticationTemplate());
                loginClient = serviceProxy.createClient(authTopic);
                Function<MetadataEnvelope<LoginRequest>, CompletableFuture<MetadataEnvelope<Try<LoginResponse>>>> loginProxy = loginClient.asyncProxy(new AuthenticationTemplate());

                sleep(1000);

                // try again, make the authentication pass
                CompletableFuture<MetadataEnvelope<Try<com.github.dfauth.authzn.domain.LoginResponse>>> f = loginProxy.apply(new MetadataEnvelope(
                        com.github.dfauth.authzn.domain.LoginRequest.builder().withUsername("fred").withPasswordHash("blah").withRandom("blah").build()));

                MetadataEnvelope<Try<com.github.dfauth.authzn.domain.LoginResponse>> response = f.get();
                assertNotNull(response);
                assertTrue(response.getPayload().isSuccess());
                response.getPayload().onSuccess(r -> assertNotNull(r.getToken())).onFailure(t -> fail(t.getMessage()));
            } finally {
                loginClient.close();
                loginService.close();
            }

            ServiceProxy.Service dummyService = null;
            ServiceProxy.Client dummyClient = null;
            try {
                serviceProxy = new ServiceProxy(system(), materializer(), p, brokerList, avroSerialization);
                dummyService = serviceProxy.createService(asProcessorUserInfo(dummyAuthSvc), "server", userInfoTopic);
                dummyService.bindToKafka(new UserInfoTemplate());
                dummyClient = serviceProxy.createClient(userInfoTopic);
                Function<MetadataEnvelope<SampleRequest>, CompletableFuture<MetadataEnvelope<Try<SampleResponse>>>> dummyProxy = dummyClient.asyncProxy(new UserInfoTemplate());

                sleep(1000);

                // make an unauthenticated call to the DummyService
                CompletableFuture<MetadataEnvelope<Try<UserInfoResponse>>> f = dummyProxy.apply(new MetadataEnvelope(NoOp.noOp));

                // expect failure
                try {
                    f.get();
                    fail("Oops. expected exception");
                } catch (ExecutionException e) {
                    assertEquals(e.getCause().getMessage(), "No authorization token found");
                }
            } finally {
                dummyClient.close();
                dummyService.close();
            }

            try {
                serviceProxy = new ServiceProxy(system(), materializer(), p, brokerList, avroSerialization);
                dummyService = serviceProxy.createService(asProcessorUserInfo(dummyAuthSvc), "server", userInfoTopic);
                dummyService.bindToKafka(new UserInfoTemplate());
                dummyClient = serviceProxy.createClient(userInfoTopic);
                Function<MetadataEnvelope<SampleRequest>, CompletableFuture<MetadataEnvelope<Try<SampleResponse>>>> dummyProxy = dummyClient.asyncProxy(new UserInfoTemplate());

                sleep(1000);

                // make an authenticated call to the DummyService
                CompletableFuture<MetadataEnvelope<Try<UserInfoResponse>>> f = dummyProxy.apply(new MetadataEnvelope(NoOp.noOp, fredTokenMetadata));

                MetadataEnvelope<Try<UserInfoResponse>> response = f.get();
                assertNotNull(response);
                assertTrue(response.getPayload().isSuccess());
                response.getPayload().onSuccess(r -> assertEquals(r.getUserId(), "fred")).onFailure(t -> fail(t.getMessage()));
            } finally {
                dummyClient.close();
                dummyService.close();
            }

            // wait on this future
//            MetadataEnvelope<com.github.dfauth.authzn.kafka.SampleResponse> response1 = f1.get();
//            assertTrue(f1.isCompletedExceptionally());

            return null;
        }));

    }

    private Processor<MetadataEnvelope<com.github.dfauth.authzn.domain.LoginRequest>, MetadataEnvelope<Try<com.github.dfauth.authzn.domain.LoginResponse>>> asProcessor(MockAuthenticationService svc) {
        return new AbstractProcessor<MetadataEnvelope<com.github.dfauth.authzn.domain.LoginRequest>, MetadataEnvelope<Try<com.github.dfauth.authzn.domain.LoginResponse>>>() {
            @Override
            protected MetadataEnvelope<Try<com.github.dfauth.authzn.domain.LoginResponse>> transform(MetadataEnvelope<com.github.dfauth.authzn.domain.LoginRequest> envelope) {
                return new MetadataEnvelope<>(Try.ofCallable(() ->
                        svc.login(envelope.getPayload())),
                        withCorrelationIdFrom(envelope));
            }
        };
    }

    private Processor<MetadataEnvelope<Void>, MetadataEnvelope<Try<UserInfoResponse>>> asProcessorUserInfo(MockAuthenticationService svc) {

        Function<MetadataEnvelope<Void>, Try<AuthenticationEnvelope<Void>>> authenticator = authenticate(jwtVerifier);

        return new AbstractProcessor<MetadataEnvelope<Void>, MetadataEnvelope<Try<UserInfoResponse>>>() {
            @Override
            protected MetadataEnvelope<Try<UserInfoResponse>> transform(MetadataEnvelope<Void> envelope) {

                return new MetadataEnvelope<>(
                        authenticator.apply(envelope).map(a -> svc.getUserInfo(a)),
                        withCorrelationIdFrom(envelope));
            }
        };

    }
}
