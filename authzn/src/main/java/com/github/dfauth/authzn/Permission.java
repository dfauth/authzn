package com.github.dfauth.authzn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public abstract class Permission {

    private static final Logger logger = LoggerFactory.getLogger(Permission.class);

    private final ResourcePath resource;
    private final Optional<Action> action;
    

    public Permission() {
        this(ResourcePath.root());
    }

    public Permission(ResourcePath resource) {
        this(resource, Optional.empty());
    }

    public Permission(Action action) {
        this(ResourcePath.root(), action);
    }

    private Permission(ResourcePath resource, Optional<Action> action) {
        this.resource = resource;
        this.action = action;
    }

    public Permission(ResourcePath resource, Action action) {
        this.resource = resource;
        this.action = Optional.of(action);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(obj == this) {
            return true;
        }
        if(getClass().equals(obj.getClass())) {
            Permission other = getClass().cast(obj);
            return this.resource.equals(other.resource) && this.action.equals(other.action);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return resource.hashCode() | action.hashCode();
    }

//    public boolean implies(Permission permission, ResourceResolver resourceResolver) {
//        if(permission == null) {
//            logger.warn("null permission object passed to implies method of permission");
//            return false;
//        }
//        if(permission == this) {
//            logger.debug("identical permission comparison: "+this);
//            return true;
//        }
//        // test if this resource implies the resource in the permission
//        if(resourceResolver.resource(new SimpleResource(this.resource)).implies(new SimpleResource(permission.resource))) {
//            // test if this action implies the action in thr permission
//            // special case - no actions on either side
//            return (this.action.implies(permission.action));
//        } else {
//            logger.debug(String.format("unrelated resources %s and %s",this.resource,permission.resource));
//            return false;
//        }
//    }

    public ResourcePath getResourcePath() {
        return resource;
    }

    public Optional<Action> getAction() {
        return this.action;
    }

    @Override
    public String toString() {
        return String.format("Permission(%s,%s)",resource, action);
    }
}
