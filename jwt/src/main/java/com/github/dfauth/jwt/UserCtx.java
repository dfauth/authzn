package com.github.dfauth.jwt;

public interface UserCtx<U> {

    String token();
    String userId();
    U payload();
}
