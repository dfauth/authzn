package com.github.dfauth.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;


public class JWKTestCase {

    private static final Logger logger = LoggerFactory.getLogger(JWKTestCase.class);

    @Test
    public void testIt() {
        try {
            KeyPair testKeyPair = KeyPairFactory.createKeyPair("RSA", 2048);
            RSAPublicKey pubKey = (RSAPublicKey) testKeyPair.getPublic();
            JsonWebKey webKey = new JsonWebKeyBuilder(pubKey, UUID.randomUUID().toString()).build();
            String asString = new ObjectMapper().writeValueAsString(JsonWebKey.keys(webKey));
            logger.info("JWK: "+asString);
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
