package com.github.dfauth.scrub.node;

import java.util.Optional;

public interface Node<T> {
    T payload();

    Optional<Node<T>> parent();

    Optional<Node<T>> child();

    boolean isVisibleTo(T t);
}
