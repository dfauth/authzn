package com.github.dfauth.scrub;

public interface UserContext<U> {
    String token();
    String userId();
    U payload();
}
