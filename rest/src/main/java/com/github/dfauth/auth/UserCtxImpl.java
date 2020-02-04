package com.github.dfauth.auth;

import com.github.dfauth.authzn.Subject;
import com.github.dfauth.jwt.UserCtx;

public class UserCtxImpl implements UserCtx<Subject> {
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
        return null;
    }

    @Override
    public Subject payload() {
        return payload;
    }
}