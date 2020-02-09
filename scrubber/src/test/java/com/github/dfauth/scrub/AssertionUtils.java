package com.github.dfauth.scrub;

import java.util.Optional;
import java.util.function.Consumer;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class AssertionUtils {

    public static <T> void assertScrubbed(Optional<T> value) {
        assertFalse(value.isPresent());
    }

    public static <T> void assertNotScrubbed(Optional<T> value) {
        assertTrue(value.isPresent());
    }

    public static <T> void assertOptional(Optional<T> o, Consumer<T> c) {
        o.map(e -> {
            c.accept(e);
            return e;
        }).orElseThrow(() -> new AssertionError("Oops"));
    }

}
