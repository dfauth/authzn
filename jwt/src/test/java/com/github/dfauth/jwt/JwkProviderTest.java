package com.github.dfauth.jwt;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.*;


public class JwkProviderTest {

    private static final Logger logger = LoggerFactory.getLogger(JwkProviderTest.class);

    @Test
    public void testIt() {

        try {
            KeyPair testKeyPair = KeyPairFactory.createKeyPair("RSA", 2048);
            RSAPublicKey pubKey = (RSAPublicKey) testKeyPair.getPublic();
            String kid = UUID.randomUUID().toString();
            JsonWebKey webKey = new JsonWebKeyBuilder(pubKey, kid).build();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ObjectMapper().writeValue(baos, JsonWebKey.keys(webKey));
            byte[] jwkBytes = baos.toByteArray();
            File f = new File("target/generated-resources/test.jwk");
            f.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(jwkBytes);
            fos.flush();
            fos.close();
            JwkProvider b = new JwkProviderBuilder(f.toURI().toURL()).build();
            Jwk result = b.get(kid);
            assertNotNull(result);
            PublicKey resultPubKey = result.getPublicKey();
            assertNotNull(resultPubKey);

        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (JwkException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }


    @Test
    public void testJwtVerifier() {

        try {
            KeyPair testKeyPair = KeyPairFactory.createKeyPair("RSA", 2048);
            RSAPublicKey pubKey = (RSAPublicKey) testKeyPair.getPublic();
            String issuer = "me";
            String kid = UUID.randomUUID().toString();
            String fred = "fred";
            List<String> roles = Collections.singletonList("user");
            JWTBuilder builder = new JWTBuilder(issuer, testKeyPair.getPrivate());
            String token = builder.forSubject(fred).withClaim("roles", roles).withClaim("kid", kid).build();

            JsonWebKey webKey = new JsonWebKeyBuilder(pubKey, kid).build();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ObjectMapper().writeValue(baos, JsonWebKey.keys(webKey));
            byte[] jwkBytes = baos.toByteArray();
            File f = new File("target/generated-resources/test.jwk");
            f.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(jwkBytes);
            fos.flush();
            fos.close();
            JwkProvider b = new JwkProviderBuilder(f.toURI().toURL()).build();

            JWTVerifier verifier = new JWTVerifier(b, issuer);
            Try<Claims> result = verifier.authenticateToken(token, a -> {
                logger.info("claim: " + a);
                return a.getBody();
            });
            assertTrue(result.isSuccess());
            assertEquals(result.get().getSubject(), fred);
            assertEquals(result.get().getIssuer(), issuer);
            assertEquals(result.get().get("roles"), roles);
            assertEquals(result.get().get("kid"), kid);

        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
