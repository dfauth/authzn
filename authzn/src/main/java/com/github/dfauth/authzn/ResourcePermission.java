package com.github.dfauth.authzn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ResourcePermission<A extends Implicable> extends Permission {

    private static final Logger logger = LoggerFactory.getLogger(ResourcePermission.class);

    private final String resource;
    private final A action;


    public ResourcePermission(String resource, A action) {
        this.resource = resource;
        this.action = action;
    }

    @Override
    protected boolean _equals(Object obj) {
        ResourcePermission other = getClass().cast(obj);
        return this.resource.equals(other.resource) && this.action.equals(other.action);
    }

    @Override
    public int hashCode() {
        return resource.hashCode() | action.hashCode();
    }

    protected boolean _implies(Permission p, ResourceResolver resourceResolver) {
        if(p instanceof ResourcePermission) {
            ResourcePermission permission = (ResourcePermission)p;
            // test if this resource implies the resource in the permission
            if(resourceResolver.resource(new SimpleResource(this.resource)).implies(new SimpleResource(permission.resource))) {
                // test if this action implies the action in thr permission
                // special case - no action on either side
                return (this.action.implies(permission.action));
            } else {
                logger.debug(String.format("unrelated resources %s and %s",this.resource,permission.resource));
                return false;
            }
        }
        return false;
    }

    public String getResource() {
        return resource;
    }

    public A getAction() {
        return this.action;
    }

    @Override
    public String toString() {
        return String.format("%s(%s,%s)",getClass().getCanonicalName(), resource, action);
    }
}
