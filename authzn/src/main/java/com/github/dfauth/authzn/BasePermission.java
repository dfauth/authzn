package com.github.dfauth.authzn;

public class BasePermission<E extends Enum<E>> extends ResourcePermission<Action<E>> {
    public BasePermission(String resource, Action<E> action) {
        super(resource, action);
    }
}
