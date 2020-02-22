package com.github.dfauth.jwt;

import com.auth0.jwk.JwkProvider;
import io.jsonwebtoken.Claims;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


public class SigningKeyResolverTest {

    private static final Logger logger = LoggerFactory.getLogger(SigningKeyResolverTest.class);

    @Test
    public void testIt() {

        try {
            KeyPair testKeyPair = KeyPairFactory.createKeyPair("RSA", 2048);
            RSAPublicKey pubKey = (RSAPublicKey) testKeyPair.getPublic();
            String issuer = "me";
            String kid = UUID.randomUUID().toString();
            String fred = "fred";
            List<String> roles = Collections.singletonList("user");
            JWTBuilder builder = new JWTBuilder(issuer, testKeyPair.getPrivate());
            String token = builder.forSubject(fred).withClaim("roles", roles).withClaim("kid", kid).build();
            JWTVerifier verifier = new JWTVerifier((JWTVerifier.ClaimsSigningKeyResolver) (jwsHeader, claims) -> pubKey, issuer);
            Try<Claims> result = verifier.authenticateToken(token, a -> {
                logger.info("claim: " + a);
                return a.getBody();
            });
            assertTrue(result.isSuccess());
            assertEquals(result.get().getSubject(), fred);
            assertEquals(result.get().getIssuer(), issuer);
            assertEquals(result.get().get("roles"), roles);
            assertEquals(result.get().get("kid"), kid);

        } finally {

        }


    }

    @Test
    public void testJwkProvider() {

        try {
            KeyPair testKeyPair = KeyPairFactory.createKeyPair("RSA", 2048);
            RSAPublicKey pubKey = (RSAPublicKey) testKeyPair.getPublic();
            String issuer = "me";
            String kid = UUID.randomUUID().toString();
            String fred = "fred";
            List<String> roles = Collections.singletonList("user");
            JWTBuilder builder = new JWTBuilder(issuer, testKeyPair.getPrivate());
            String token = builder.forSubject(fred).withClaim("roles", roles).withClaim("kid", kid).build();

            JwkBuilder jwkBuilder = new JwkBuilder(pubKey, kid);
            JwkProvider jwkProvider = k -> jwkBuilder.build();
            JWTVerifier verifier = new JWTVerifier(jwkProvider, issuer);
            Try<Claims> result = verifier.authenticateToken(token, a -> {
                logger.info("claim: " + a);
                return a.getBody();
            });
            assertTrue(result.isSuccess());
            assertEquals(result.get().getSubject(), fred);
            assertEquals(result.get().getIssuer(), issuer);
            assertEquals(result.get().get("roles"), roles);
            assertEquals(result.get().get("kid"), kid);

        } finally {

        }


    }
}
