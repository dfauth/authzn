package com.github.dfauth.auth;

import com.github.dfauth.authzn.PrincipalType;
import com.github.dfauth.authzn.Subject;
import com.github.dfauth.authzn.UserContext;

public class UserCtxImpl implements UserContext<Subject> {
    private String token;
    private Subject payload;

    public UserCtxImpl(String token, Subject payload) {
        this.token = token;
        this.payload = payload;
    }

    @Override
    public String token() {
        return token;
    }

    @Override
    public String userId() {
        // should find a user principal
        return payload.getPrincipals().stream()
                .filter(p -> p.getPrincipalType() == PrincipalType.USER)
                .findFirst()
                .map(p -> p.getName())
                .orElseThrow(() -> new IllegalArgumentException("Missing userid"));
    }

    @Override
    public Subject payload() {
        return payload;
    }

    @Override
    public Subject getSubject() {
        return payload();
    }
}
