package com.github.dfauth.authzn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class Permission {

    private static final Logger logger = LoggerFactory.getLogger(Permission.class);

    private static final String ROOT_RESOURCE = "ROOT";

    private final String resource;
    private final ActionSet actions;
    

    public Permission() {
        this(ROOT_RESOURCE, Collections.emptySet());
    }

    public Permission(String resource) {
        this(resource, Collections.emptySet());
    }

    public Permission(Set<Action> actions) {
        this(ROOT_RESOURCE, actions);
    }

    public Permission(String resource, Action action) {
        this(resource, Collections.singleton(action));
    }

    public Permission(String resource, Set<Action> actions) {
        this.resource = resource;
        this.actions = ActionSet.parse(actions.stream().map(a -> a.name()).collect(Collectors.toSet()));
    }

    public Permission(String resource, List<String> actions) {
        this.resource = resource;
        this.actions = ActionSet.parse(new HashSet(actions));
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
            return this.resource.equals(other.resource) && this.actions.equals(other.actions);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return resource.hashCode() | actions.hashCode();
    }

    public boolean implies(Permission permission, ResourceResolver resourceResolver) {
        if(permission == null) {
            logger.warn("null permission object passed to implies method of permission");
            return false;
        }
        if(permission == this) {
            logger.debug("identical permission comparison: "+this);
            return true;
        }
        // test if this resource implies the resource in the permission
        if(resourceResolver.resource(new SimpleResource(this.resource)).implies(new SimpleResource(permission.resource))) {
            // test if this action implies the action in thr permission
            // special case - no actions on either side
            return (this.actions.implies(permission.actions));
        } else {
            logger.debug(String.format("unrelated resources %s and %s",this.resource,permission.resource));
            return false;
        }
    }

    public String getResource() {
        return resource;
    }

    public ActionSet getActions() {
        return this.actions;
    }

    @Override
    public String toString() {
        return String.format("Permission(%s,%s)",resource, actions);
    }
}
