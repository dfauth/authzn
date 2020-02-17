package com.github.dfauth.authzn.avro;

import io.vavr.control.Try;

import java.util.function.Function;

public class AvroUtils {

    public static <T,R> Try<T> match(Object payload, Class<R> cls, Function<R, T> f) {
        if(payload.getClass().isAssignableFrom(cls)) {
            return Try.success(f.apply(cls.cast(payload)));
        } else {
            return Try.failure(new RuntimeException(((Exception)payload).getMessage()));
        }
    }

}
