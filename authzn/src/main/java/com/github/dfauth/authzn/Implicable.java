package com.github.dfauth.authzn;

public interface Implicable<T> {
    boolean implies(T t);
}
