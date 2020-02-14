package com.github.dfauth.authzn.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.function.Function;

public class TryCatchUtils {

    private static final Logger logger = LoggerFactory.getLogger(TryCatchUtils.class);

    public static <T> T tryCatch(Callable<T> c) {
        return tryCatch(c, e -> {
            throw new RuntimeException();
        });
    }

    public static void tryCatch(Runnable r) {
        try {
            r.run();
        } catch (RuntimeException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static <T> T tryCatchReturningNull(Callable<T> c) {
        return tryCatch(c, e -> null);
    }

    public static <T> T tryCatch(Callable<T> c, Function<Exception, T> exceptionHandler) {
        try {
            return c.call();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return exceptionHandler.apply(e);
        }
    }

}
