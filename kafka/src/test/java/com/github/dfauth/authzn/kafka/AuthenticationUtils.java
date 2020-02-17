package com.github.dfauth.authzn.kafka;

import com.github.dfauth.authzn.avro.MetadataEnvelope;
import com.github.dfauth.jwt.JWTVerifier;
import com.github.dfauth.kafka.AuthenticationEnvelope;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class AuthenticationUtils {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationUtils.class);

    public static <T> Function<MetadataEnvelope<T>, Try<AuthenticationEnvelope<T>>> authenticate(JWTVerifier verifier, Class<T> ignore) {
        return authenticate(verifier);
    }

    public static <T> Function<MetadataEnvelope<T>, Try<AuthenticationEnvelope<T>>> authenticate(JWTVerifier verifier) {
        return m -> m.authorizationToken()
                    .map(verifier.tokenAuthenticator(verifier.asUser))
                    .map(ta -> ta.map(u -> new AuthenticationEnvelope<>(m.getPayload(), m.getMetadata(), u)))
                    .orElseGet(() -> Try.failure(new IllegalStateException("No authorization token found")));
    }

}
