package com.github.dfauth.authzn.avro;

import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class MetadataEnvelope<T> {

    public static final String TIMEOUT = "timeoutDuration";
    private static final String AUTHORIZATION = "Authorization";
    private static final String CORRELATION_ID = "correlationId";
    private static final String DIRECTION = "direction";

    private final T payload;
    private final Map<String, String> metadata;

    public MetadataEnvelope(T t) {
        this(t, Collections.emptyMap());
    }

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

    public static Map<String, String> withCorrelationIdFrom(MetadataEnvelope<?> envelope) {
        return envelope.correlationId().map(i -> {
            return (Map<String, String>)ImmutableMap.of(CORRELATION_ID, i);
        }).orElse(Collections.emptyMap());
    }

    public MetadataEnvelope<T> withCorrelationId(String id) {
        return new MetadataEnvelope<>(payload, transformMetadata(CORRELATION_ID, id));
    }

    public MetadataEnvelope<T> inbound() {
        return new MetadataEnvelope<>(payload, transformMetadata(DIRECTION, Direction.inbound.name()));
    }

    public MetadataEnvelope<T> outbound() {
        return new MetadataEnvelope<>(payload, transformMetadata(DIRECTION, Direction.outbound.name()));
    }

    private Map<String, String> transformMetadata(String key, String value) {
        Map<String, String> tmp = new HashMap<>(metadata);
        tmp.put(key, value);
        return tmp;
    }

    public boolean isInbound() {
        return Direction.inbound.isPresent(metadata);
    }

    public boolean isOutbound() {
        return Direction.outbound.isPresent(metadata);
    }

    private enum Direction {
        inbound, outbound;

        public boolean isPresent(Map<String, String> metadata) {
            return metadata.containsKey(DIRECTION) && metadata.get(DIRECTION).equalsIgnoreCase(name());
        }
    }
}
