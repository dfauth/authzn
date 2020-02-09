package com.github.dfauth.scrub;

public class UserModelImpl implements UserModel {

    private String userId;
    private Company company;

    UserModelImpl(String userId, Company company) {
        this.userId = userId;
        this.company = company;
    }

    public String getUserId() {
        return userId;
    }

    public Company company() {
        return company;
    }
}
