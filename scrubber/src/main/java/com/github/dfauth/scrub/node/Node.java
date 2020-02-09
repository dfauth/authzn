package com.github.dfauth.scrub.node;

import java.util.Optional;
import java.util.function.Supplier;

public interface Node<U extends Supplier<T>,T> {
    T payload();

    Optional<Node<U,T>> parent();

    Optional<Node<U,T>> child();

    boolean isVisibleTo(U ctx);
}
