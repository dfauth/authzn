package com.github.dfauth.authzn;

public class RootResourceNode<V> extends ResourceNode<V> {

    public RootResourceNode() {
    }

    public RootResourceNode(Resource<V> resource) {
        this.resource.add(resource);
    }

    @Override
    protected boolean isRoot() {
        return true;
    }
}
