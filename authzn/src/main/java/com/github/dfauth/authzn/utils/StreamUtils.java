package com.github.dfauth.authzn.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class StreamUtils {

    private static final Logger logger = LoggerFactory.getLogger(StreamUtils.class);

    public static <T> Consumer<T> logStream(String msg) {
        return a -> {logger.info(String.format("%s payload: %s", msg, a));};
    }

    public static <T> Consumer<T> debugLogStream(String msg) {
        return a -> {logger.debug(String.format("%s payload: %s", msg, a));};
    }
}
