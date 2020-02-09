package com.github.dfauth.scrub.node;

import com.github.dfauth.scrub.Utils;

import java.util.Optional;

public interface ScrubbedNode<T> extends Node<T> {

    default Optional<T> getValueFor(T t) {
        return Utils.stream(Optional.ofNullable(payload())).filter(v -> isVisibleTo(t)).findFirst();
    }
}
