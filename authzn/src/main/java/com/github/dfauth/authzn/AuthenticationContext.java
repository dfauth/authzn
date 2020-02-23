package com.github.dfauth.authzn;

public interface AuthenticationContext<U> {
    String token();
    String userId();
    U payload();
    Subject getSubject();
}
