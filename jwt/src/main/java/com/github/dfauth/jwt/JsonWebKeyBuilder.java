package com.github.dfauth.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.interfaces.RSAPublicKey;
import java.util.Base64;


public class JsonWebKeyBuilder {

    private static final Logger logger = LoggerFactory.getLogger(JsonWebKeyBuilder.class);
    private final RSAPublicKey pubKey;
    private String kid;

    public JsonWebKeyBuilder(RSAPublicKey pubKey, String kid) {
        this.pubKey = pubKey;
        this.kid = kid;
    }

    public JsonWebKey build() {
        JsonWebKey.JsonWebKeyBuilder builder = JsonWebKey.builder();
        builder.withKty(pubKey.getAlgorithm())
        .withKid(kid)
        .withN(Base64.getUrlEncoder().encodeToString(pubKey.getModulus().toByteArray()))
        .withE(Base64.getUrlEncoder().encodeToString(pubKey.getPublicExponent().toByteArray()))
        .withAlg("RS256");
        return builder.build();
    }
}
