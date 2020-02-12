package com.github.dfauth.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TransformationUtils {

    private static final Logger logger = LoggerFactory.getLogger(TransformationUtils.class);

    public static <T,R> akka.japi.function.Function<T,R> toJapi(java.util.function.Function<T, R> f) {
        return t -> f.apply(t);
    }
}
