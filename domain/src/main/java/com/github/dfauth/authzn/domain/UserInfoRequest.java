package com.github.dfauth.authzn.domain;

public class UserInfoRequest {

    private final String userId;

    public UserInfoRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
