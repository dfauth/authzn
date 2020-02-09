package com.github.dfauth.scrub;

import java.util.Optional;

public interface ScrubbedProperty<K,V> {

    boolean isVisibleTo(K k);

    default Optional<V> getValueFor(K k) {
        return Utils.stream(Optional.ofNullable(value())).filter(v -> isVisibleTo(k)).findFirst();
    }

    V value();

}
