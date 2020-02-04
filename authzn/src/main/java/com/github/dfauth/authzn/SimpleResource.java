package com.github.dfauth.authzn;

public class SimpleResource<V> extends Resource<V> {

    public SimpleResource(ResourcePath path, V payload) {
        super(path, payload);
    }
}
