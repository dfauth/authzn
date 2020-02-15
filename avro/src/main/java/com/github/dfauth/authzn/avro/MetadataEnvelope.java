package com.github.dfauth.authzn.avro;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class MetadataEnvelope<T> {

    private static final String AUTHORIZATION = "Authorization";
    private static final String CORRELATION_ID = "correlationId";

    private final T payload;
    private final Map<String, String> metadata;

    public MetadataEnvelope(T t, Map<String, String> metadata) {
        this.payload = t;
        this.metadata = ImmutableMap.copyOf(metadata);
    }

    public <R> MetadataEnvelope<R> mapPayload(Function<T,R> f) {
        return new MetadataEnvelope<>(f.apply(payload), this.metadata);
    }

    public T getPayload() {
        return payload;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public Optional<String> authorizationToken() {
        return Optional.ofNullable(metadata.get(AUTHORIZATION));
    }

    public Optional<String> correlationId() {
        return Optional.ofNullable(metadata.get(CORRELATION_ID));
    }

    public static Map<String, String> withToken(String token) {
        return ImmutableMap.of(AUTHORIZATION, token);
    }
}
