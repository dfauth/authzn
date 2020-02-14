package com.github.dfauth.authzn.avro;

import java.util.Map;
import java.util.function.Function;

public class MetadataEnvelope<T> {

    private final T payload;
    private final Map<String, String> metadata;

    public MetadataEnvelope(T t, Map<String, String> metadata) {
        this.payload = t;
        this.metadata = metadata;
    }

    public <R> MetadataEnvelope<R> map(Function<T,R> f) {
        return new MetadataEnvelope<>(f.apply(payload), this.metadata);
    }

    public T getPayload() {
        return payload;
    }
}
