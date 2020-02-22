package com.github.dfauth.jwt;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.interfaces.RSAPublicKey;
import java.util.Base64;


public class JwkBuilder implements JwkProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwkBuilder.class);
    private final RSAPublicKey pubKey;
    private String kid;

    public JwkBuilder(RSAPublicKey pubKey, String kid) {
        this.pubKey = pubKey;
        this.kid = kid;
    }

    public Jwk build() {
        JsonWebKey.JsonWebKeyBuilder builder = JsonWebKey.builder();
        builder.withKty(pubKey.getAlgorithm())
        .withKid(kid)
        .withN(Base64.getUrlEncoder().encodeToString(pubKey.getModulus().toByteArray()))
        .withE(Base64.getUrlEncoder().encodeToString(pubKey.getPublicExponent().toByteArray()))
        .withAlg("RS256");
        return Jwk.fromValues(builder.build().asMap());
    }

    @Override
    public Jwk get(String s) throws JwkException {
        return build();
    }
}
