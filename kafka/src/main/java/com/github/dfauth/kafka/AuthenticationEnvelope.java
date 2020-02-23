package com.github.dfauth.kafka;

import com.github.dfauth.authzn.Subject;
import com.github.dfauth.authzn.User;
import com.github.dfauth.authzn.AuthenticationContext;
import com.github.dfauth.authzn.avro.MetadataEnvelope;

import java.util.Map;


public class AuthenticationEnvelope<T> extends MetadataEnvelope<T> implements AuthenticationContext<T> {

    private final User user;

    public AuthenticationEnvelope(T payload, Map<String, String> metadata, User u) {
        super(payload, metadata);
        this.user = u;
    }

    @Override
    public String token() {
        return super.authorizationToken().orElseThrow(() -> new IllegalStateException("should never be thrown"));
    }

    @Override
    public String userId() {
        return this.user.getUserId();
    }

    @Override
    public T payload() {
        return super.getPayload();
    }

    public Subject getSubject() {
        return this.user.toSubject();
    }

    public User getUser() {
        return this.user;
    }
}
