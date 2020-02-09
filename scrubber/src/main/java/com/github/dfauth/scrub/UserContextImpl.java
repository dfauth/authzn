package com.github.dfauth.scrub;

public class UserContextImpl implements UserContext<UserModelImpl> {

    private String token;
    private UserModelImpl payload;

    public UserContextImpl(String token, UserModelImpl payload) {
        this.token = token;
        this.payload = payload;
    }

    @Override
    public String token() {
        return token;
    }

    @Override
    public String userId() {
        return payload.getUserId();
    }

    @Override
    public UserModelImpl payload() {
        return payload;
    }
}
