package com.github.dfauth.authzn;

import java.util.Set;

public class UserModelImpl implements UserModel, RoleModel {

    private final Set<Role> roles;
    private String userId;
    private Company company;

    public UserModelImpl(String userId, Company company, Set<Role> roles) {
        this.userId = userId;
        this.company = company;
        this.roles = roles;
    }

    public String getUserId() {
        return userId;
    }

    public Company company() {
        return company;
    }

    @Override
    public Set<Role> roles() {
        return roles;
    }
}
