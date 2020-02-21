package com.github.dfauth.authzn.kafka;

import com.github.dfauth.authzn.ImmutableSubject;
import com.github.dfauth.authzn.Subject;
import com.github.dfauth.authzn.User;
import com.github.dfauth.authzn.avro.AvroSerialization;
import com.github.dfauth.authzn.avro.MetadataEnvelope;
import com.github.dfauth.authzn.domain.*;
import com.github.dfauth.authzn.kafka.proxy.templates.UserAdminTemplate;
import com.github.dfauth.authzn.kafka.proxy.templates.UserInfoTemplate;
import com.github.dfauth.authzn.utils.AbstractProcessor;
import com.github.dfauth.authzn.utils.CloseableProcessor;
import com.github.dfauth.jwt.JWTBuilder;
import com.github.dfauth.jwt.JWTVerifier;
import com.github.dfauth.jwt.KeyPairFactory;
import com.github.dfauth.kafka.AuthenticationEnvelope;
import com.github.dfauth.kafka.EmbeddedKafkaTest;
import com.github.dfauth.kafka.proxy.AsyncBinding;
import com.github.dfauth.kafka.proxy.AsyncBindingClient;
import com.github.dfauth.kafka.proxy.ServiceProxy;
import com.github.dfauth.kafka.proxy.templates.AuthenticationTemplate;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.vavr.control.Try;
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
    private String userAdminTopic = "userAdmin";

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

            final ServiceProxy.Factory factory = ServiceProxy.newFactory(system(), materializer(), p, brokerList, avroSerialization);
            final ServiceProxy<LoginRequest, LoginResponse, com.github.dfauth.avro.authzn.LoginRequest, com.github.dfauth.avro.authzn.LoginResponse> loginServiceProxy =
                    factory.createServiceProxy(authTopic, new AuthenticationTemplate());
            final ServiceProxy<NoOp, UserInfoResponse, com.github.dfauth.avro.authzn.NoOp, com.github.dfauth.avro.authzn.UserInfoResponse> userInfoServiceProxy =
                    factory.createServiceProxy(userInfoTopic, new UserInfoTemplate());
            final ServiceProxy<UserInfoRequest, UserInfoResponse, com.github.dfauth.avro.authzn.UserInfoRequest, com.github.dfauth.avro.authzn.UserInfoResponse> userAdminServiceProxy =
                    factory.createServiceProxy(userAdminTopic, new UserAdminTemplate());
            final AsyncBinding loginService = loginServiceProxy.createAsyncBinding(asProcessor(dummyAuthSvc), "server");
            final AsyncBinding userInfoService = userInfoServiceProxy.createAsyncBinding(asProcessorUserInfo(dummyAuthSvc), "server");
            final AsyncBinding userAdminService = userAdminServiceProxy.createAsyncBinding(asProcessorUserInfoFor(dummyAuthSvc), "server");

            try {


                AsyncBindingClient<LoginRequest, LoginResponse, com.github.dfauth.avro.authzn.LoginRequest, com.github.dfauth.avro.authzn.LoginResponse> loginClient = null;
                try {
                    loginClient = loginServiceProxy.createAsyncBindingClient();

                    sleep(1000);

                    // make the authentication fail
                    CompletableFuture<MetadataEnvelope<Try<com.github.dfauth.authzn.domain.LoginResponse>>> f = loginClient.call(new MetadataEnvelope(
                            com.github.dfauth.authzn.domain.LoginRequest.builder().withUsername("fred").withPasswordHash("blah").withRandom("blah2").build()));

                    // expect failure
                    try {
                        f.get();
                        fail("Oops. expected exception");
                    } catch (ExecutionException e) {
                        assertEquals(e.getCause().getMessage(), "Authentication failure for user fred");
                    }
                } finally {
                    if(loginClient != null) {
                        loginClient.close();
                    }
                }

                try {
                    loginClient = loginServiceProxy.createAsyncBindingClient();

                    sleep(1000);

                    // try again, make the authentication pass
                    CompletableFuture<MetadataEnvelope<Try<com.github.dfauth.authzn.domain.LoginResponse>>> f = loginClient.call(new MetadataEnvelope(
                            com.github.dfauth.authzn.domain.LoginRequest.builder().withUsername("fred").withPasswordHash("blah").withRandom("blah").build()));

                    MetadataEnvelope<Try<com.github.dfauth.authzn.domain.LoginResponse>> response = f.get();
                    assertNotNull(response);
                    assertTrue(response.getPayload().isSuccess());
                    response.getPayload().onSuccess(r -> assertNotNull(r.getToken())).onFailure(t -> fail(t.getMessage()));
                } finally {
                    if(loginClient != null) {
                        loginClient.close();
                    }
                }

                AsyncBindingClient<NoOp, UserInfoResponse, com.github.dfauth.avro.authzn.NoOp, com.github.dfauth.avro.authzn.UserInfoResponse> userInfoClient = null;
                try {
                    userInfoClient = userInfoServiceProxy.createAsyncBindingClient();

                    // make an unauthenticated call to the DummyService
                    CompletableFuture<MetadataEnvelope<Try<UserInfoResponse>>> f = userInfoClient.call(new MetadataEnvelope(NoOp.noOp));

                    // expect failure
                    try {
                        f.get();
                        fail("Oops. expected exception");
                    } catch (ExecutionException e) {
                        assertEquals(e.getCause().getMessage(), "No authorization token found");
                    }
                } finally {
                    if(userInfoClient != null) {
                        userInfoClient.close();
                    }
                }

                try {
                    userInfoClient = userInfoServiceProxy.createAsyncBindingClient();

                    // make an authenticated call to the DummyService
                    CompletableFuture<MetadataEnvelope<Try<UserInfoResponse>>> f = userInfoClient.call(new MetadataEnvelope(NoOp.noOp, fredTokenMetadata));

                    MetadataEnvelope<Try<UserInfoResponse>> response = f.get();
                    assertNotNull(response);
                    assertTrue(response.getPayload().isSuccess());
                    response.getPayload().onSuccess(r -> assertEquals(r.getUserId(), "fred")).onFailure(t -> fail(t.getMessage()));
                } finally {
                    if(userInfoClient != null) {
                        userInfoClient.close();
                    }
                }

                // final endpoint is authenticated and requires admin role
                AsyncBindingClient<UserInfoRequest, UserInfoResponse, com.github.dfauth.avro.authzn.UserInfoRequest, com.github.dfauth.avro.authzn.UserInfoResponse> userAdminClient = null;
                try {
                    userAdminClient = userAdminServiceProxy.createAsyncBindingClient();

                    // make an authenticated call to the DummyService
                    CompletableFuture<MetadataEnvelope<Try<UserInfoResponse>>> f = userAdminClient.call(new MetadataEnvelope(new UserInfoRequest("fred"), fredTokenMetadata));

                    // expect failure
                    try {
                        f.get();
                        fail("Oops. expected exception");
                    } catch (ExecutionException e) {
                        assertEquals(e.getCause().getMessage(), "ImmutablePrincipal([ImmutablePrincipal(USER,default,fred), ImmutablePrincipal(ROLE,default,user)]) is not authorized to perform actions Optional[VIEW] on resource /users/fred");
                    }
                } finally {
                    if(userAdminClient != null) {
                        userAdminClient.close();
                    }
                }

                // final endpoint is authenticated and requires admin role - wilma should succeed
                try {
                    userAdminClient = userAdminServiceProxy.createAsyncBindingClient();

                    // make an authenticated call to the DummyService
                    CompletableFuture<MetadataEnvelope<Try<UserInfoResponse>>> f = userAdminClient.call(new MetadataEnvelope(new UserInfoRequest("wilma"), wilmaTokenMetadata));

                    MetadataEnvelope<Try<UserInfoResponse>> response = f.get();
                    assertNotNull(response);
                    assertTrue(response.getPayload().isSuccess());
                    response.getPayload().onSuccess(r -> assertEquals(r.getUserId(), "wilma")).onFailure(t -> fail(t.getMessage()));
                } finally {
                    if(userAdminClient != null) {
                        userAdminClient.close();
                    }
                }

            } finally {
                loginService.close();
                userInfoService.close();
                userAdminService.close();
            }

            return null;
        }));

    }

    private CloseableProcessor<MetadataEnvelope<com.github.dfauth.authzn.domain.LoginRequest>, MetadataEnvelope<Try<com.github.dfauth.authzn.domain.LoginResponse>>> asProcessor(MockAuthenticationService svc) {
        return new AbstractProcessor<MetadataEnvelope<com.github.dfauth.authzn.domain.LoginRequest>, MetadataEnvelope<Try<com.github.dfauth.authzn.domain.LoginResponse>>>() {
            @Override
            protected MetadataEnvelope<Try<com.github.dfauth.authzn.domain.LoginResponse>> transform(MetadataEnvelope<com.github.dfauth.authzn.domain.LoginRequest> envelope) {
                return new MetadataEnvelope<>(Try.ofCallable(() ->
                        svc.login(envelope.getPayload())),
                        withCorrelationIdFrom(envelope));
            }
        };
    }

    private CloseableProcessor<MetadataEnvelope<NoOp>, MetadataEnvelope<Try<UserInfoResponse>>> asProcessorUserInfo(MockAuthenticationService svc) {

        Function<MetadataEnvelope<NoOp>, Try<AuthenticationEnvelope<NoOp>>> authenticator = authenticate(jwtVerifier);

        return new AbstractProcessor<MetadataEnvelope<NoOp>, MetadataEnvelope<Try<UserInfoResponse>>>() {
            @Override
            protected MetadataEnvelope<Try<UserInfoResponse>> transform(MetadataEnvelope<NoOp> envelope) {

                return new MetadataEnvelope<>(
                        authenticator.apply(envelope).map(a -> svc.getUserInfo(a)),
                        withCorrelationIdFrom(envelope));
            }
        };

    }

    private CloseableProcessor<MetadataEnvelope<UserInfoRequest>, MetadataEnvelope<Try<UserInfoResponse>>> asProcessorUserInfoFor(MockAuthenticationService svc) {

        Function<MetadataEnvelope<UserInfoRequest>, Try<AuthenticationEnvelope<UserInfoRequest>>> authenticator = authenticate(jwtVerifier);

        return new AbstractProcessor<MetadataEnvelope<UserInfoRequest>, MetadataEnvelope<Try<UserInfoResponse>>>() {
            @Override
            protected MetadataEnvelope<Try<UserInfoResponse>> transform(MetadataEnvelope<UserInfoRequest> envelope) {

                return new MetadataEnvelope<>(
                        authenticator.apply(envelope).map(a -> svc.getUserInfoFor(a)),
                        withCorrelationIdFrom(envelope));
            }
        };

    }
}
