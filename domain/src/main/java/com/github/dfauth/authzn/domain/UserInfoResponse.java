package com.github.dfauth.authzn.domain;

import java.util.Set;

public class UserInfoResponse {

    private final String userId;
    private final String companyId;
    private final Set<String> roles;

    public UserInfoResponse(String userId, String companyId, Set<String> roles) {
        this.userId = userId;
        this.companyId = companyId;
        this.roles = roles;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getUserId() {
        return userId;
    }
}
