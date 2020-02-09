package com.github.dfauth.scrub;

import java.util.Optional;
import java.util.stream.Stream;

public class Utils {

    // java 8 does not implement Optional.stream() - coming in java 9
    public static <T> Stream<T> stream(Optional<T> o) {
        if(o.isPresent()) {
            return Stream.of(o.get());
        } else {
            return Stream.empty();
        }
    }
}
