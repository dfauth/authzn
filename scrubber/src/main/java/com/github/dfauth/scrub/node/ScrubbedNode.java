package com.github.dfauth.scrub.node;

import com.github.dfauth.scrub.Utils;

import java.util.Optional;
import java.util.function.Supplier;

public interface ScrubbedNode<U extends Supplier<T>,T> extends Node<U,T> {

    default Optional<T> getValueFor(U u) {
        return Utils.stream(Optional.ofNullable(payload())).filter(v -> isVisibleTo(u)).findFirst();
    }
}
