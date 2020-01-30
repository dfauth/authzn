package com.github.dfauth.authzn;

public interface Principal extends java.security.Principal {
    PrincipalType getPrincipalType();
    String getSource();
    String getName();
}