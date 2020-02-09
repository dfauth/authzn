package com.github.dfauth.authzn;

public interface UserContext<U> {
    String token();
    String userId();
    U payload();
    Subject getSubject();
}
