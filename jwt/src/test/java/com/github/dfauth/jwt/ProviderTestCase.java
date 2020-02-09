package com.github.dfauth.jwt;

import com.github.dfauth.authzn.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.security.KeyPair;

import static com.github.dfauth.authzn.Role.role;


public class ProviderTestCase {

    private static final Logger logger = LoggerFactory.getLogger(ProviderTestCase.class);

    @Test
    public void testIt() {
        KeyPair testKeyPair = KeyPairFactory.createKeyPair("RSA", 2048);
        String issuer = "me";
        JWTBuilder jwtBuilder = new JWTBuilder(issuer, testKeyPair.getPrivate());
        User user = User.of("fred", "flintstone", role("test:admin"), role("test:user"));
        String token = jwtBuilder.forSubject(user.getUserId()).withClaim("roles", user.getRoles()).build();

        JWTVerifier jwtVerifier = new JWTVerifier(testKeyPair.getPublic(), issuer);
        jwtVerifier.authenticateToken(token, claims -> {
            logger.info("claims: "+claims);
            return null;
        });
    }

}
