package com.github.dfauth.authzn;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class User {

    private String userId;
    private Set<Role> roles;
    private Instant expiry;
    private String companyId;

    public User(String userId, String companyId) {
        this(userId, companyId, Collections.emptySet(), defaultExpiry());
    }

    public User(String userId, String companyId, Set<Role> roles) {
        this(userId, companyId, roles, defaultExpiry());
    }

    public User(String userId, String companyId, Set<Role> roles, Instant expiry) {
        this.userId = userId;
        this.companyId = companyId;
        this.roles = roles;
        this.expiry = expiry;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public String getUserId() {
        return userId;
    }

    public Instant getExpiry() {
        return expiry;
    }

    private static Instant defaultExpiry() {
        return Instant.now().plusSeconds(60*60);
    }

    public static User of(String userId, String companyId, Role... roles) {
        return of(userId, companyId, defaultExpiry(), roles);
    }

    public static User of(String userId, String companyId, Instant expiry, Role... roles) {
        return new User(userId, companyId, Stream.of(roles).collect(Collectors.toSet()), expiry);
    }

    public String getCompanyId() {
        return companyId;
    }
}
