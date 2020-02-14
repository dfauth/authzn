package com.github.dfauth.authzn;

public interface Principal extends java.security.Principal {
    PrincipalType getPrincipalType();
    String getSource();
    String getName();
    default String asString() {
        return String.format("%s:%s:%s",getPrincipalType().name(), getSource(), getName());
    }
}