package com.github.dfauth.authzn.avro;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class MetadataEnvelope<T> {

    private static final String AUTHORIZATION = "Authorization";
    private static final String CORRELATION_ID = "CorrelationId";

    private final T payload;
    private final Map<String, String> metadata;

    public MetadataEnvelope(T t, Map<String, String> metadata) {
        this.payload = t;
        this.metadata = metadata;
    }

    public <R> MetadataEnvelope<R> mapPayload(Function<T,R> f) {
        return new MetadataEnvelope<>(f.apply(payload), this.metadata);
    }

    public <I,O> MetadataEnvelope<T> mapMetadata(String header, UnaryOperator<String> f) {
        return mapMetadata(header, f, header);
    }

    public MetadataEnvelope<T> mapMetadata(String sourceKey, UnaryOperator<String> f, String targetKey) {
        Map<String, String> newMetadata = Optional.of(this.metadata.get(sourceKey)).map(f).map(o -> {
            Map<String, String> tmp = new HashMap<>(metadata);
            tmp.put(targetKey, o);
            return tmp;
        }).orElse(metadata);
        return new MetadataEnvelope<T>(payload,newMetadata);
    }

    public T getPayload() {
        return payload;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public Optional<String> authorizationToken() {
        return Optional.of((String)metadata.get(AUTHORIZATION));
    }

    public Optional<String> correlationId() {
        return Optional.of((String)metadata.get(CORRELATION_ID));
    }
}
