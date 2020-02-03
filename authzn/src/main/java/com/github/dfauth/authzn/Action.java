package com.github.dfauth.authzn;

public interface Action<E extends Enum<E>> extends Implicable<E> {

    String name();

    @Override
    default boolean implies(E e) {
        return this.equals(e);
    }

}
