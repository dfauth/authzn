package com.github.dfauth.authzn.kafka;

import com.github.dfauth.authzn.User;
import com.github.dfauth.authzn.domain.LoginRequest;
import com.github.dfauth.authzn.domain.LoginResponse;
import com.github.dfauth.authzn.domain.UserInfoResponse;
import com.github.dfauth.jwt.JWTBuilder;
import com.github.dfauth.jwt.KeyPairFactory;
import com.github.dfauth.kafka.AuthenticationEnvelope;

import java.security.KeyPair;
import java.util.stream.Collectors;

import static com.github.dfauth.authzn.Role.role;


public class MockAuthenticationService {


    private KeyPair testKeyPair = KeyPairFactory.createKeyPair("RSA", 2048);
    private String issuer = "me";
    private JWTBuilder jwtBuilder = new JWTBuilder(issuer, testKeyPair.getPrivate());

    public LoginResponse login(LoginRequest request) {

        if(request.getPasswordHash().equals(request.getRandom())) {
            // generate an user token for updating the directives
            User adminUser = User.of(request.getUsername(), "flintstone", role("test:user"));
            String token = jwtBuilder.forSubject(adminUser.getUserId()).withClaim("roles", adminUser.getRoles()).build();
            return new LoginResponse(token);
        } else {
            throw new SecurityException("Authentication failure for user "+request.getUsername());
        }

    }

    public <T> UserInfoResponse getUserInfo(AuthenticationEnvelope<T> envelope) {
        return new UserInfoResponse(envelope.getUser().getUserId(), envelope.getUser().getCompanyId(), envelope.getUser().getRoles().stream().map(r -> r.getRolename()).collect(Collectors.toSet()));
    }
}
