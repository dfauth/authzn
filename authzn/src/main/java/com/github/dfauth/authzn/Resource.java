package com.github.dfauth.authzn;

import java.util.Iterator;

public class Resource<V> {

    private final ResourcePath path;
    protected final V payload;

    public Resource(ResourcePath path, V payload) {
        this.path = path;
        this.payload = payload;
    }

    public ResourcePath getResourcePath() {
        return path;
    }

    public V getPayload() {
        return payload;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) return false;
        if(obj == this) return true;
        if(obj instanceof Resource) {
            Resource other = (Resource) obj;
            return this.path.equals(other.path) && this.payload.equals(other.payload);
        }
        return false;
    }

    public boolean implies(Resource resource) {
        // one resource implies another if it can be considered a 'parent'
        Iterator<String> parent = getResourcePath().getPath().iterator();
        Iterator<String> child = resource.getResourcePath().getPath().iterator();
        while(parent.hasNext()) {
            String k = parent.next();
            if(child.hasNext()) {
                if(!k.equals(child.next())) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }
}
