package com.github.dfauth.authzn.kafka;

import com.github.dfauth.authzn.User;
import com.github.dfauth.jwt.JWTBuilder;
import com.github.dfauth.jwt.KeyPairFactory;

import java.security.KeyPair;

import static com.github.dfauth.authzn.Role.role;


public class DummyAuthenticationService {


    private KeyPair testKeyPair = KeyPairFactory.createKeyPair("RSA", 2048);
    private String issuer = "me";
    private JWTBuilder jwtBuilder = new JWTBuilder(issuer, testKeyPair.getPrivate());

    public LoginResponse serviceCall(LoginRequest request) {

        // generate an user token for updating the directives
        User adminUser = User.of(request.getUsername(), "flintstone", role("test:user"));
        String token = jwtBuilder.forSubject(adminUser.getUserId()).withClaim("roles", adminUser.getRoles()).build();
        return new LoginResponse(token);
    }
}
