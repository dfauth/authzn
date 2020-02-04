package com.github.dfauth.authzn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceNodeImpl<V> extends ResourceNode<V> {

    private static final Logger logger = LoggerFactory.getLogger(ResourceNodeImpl.class);

    private String key;

    public ResourceNodeImpl(String key) {
        this.key = key;
    }

    public ResourceNodeImpl(String key, Resource<V> resource) {
        this.key = key;
        this.resource.add(resource);
    }

    public ResourceNodeImpl<V> add(Resource<V> resource) {
        findNearest(resource.getResourcePath().getPath().iterator(), addingConsumer(resource));
        return this;
    }

}
