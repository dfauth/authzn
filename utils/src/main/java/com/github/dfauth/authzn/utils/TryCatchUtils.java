package com.github.dfauth.authzn.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.function.Function;

public class TryCatchUtils {

    private static final Logger logger = LoggerFactory.getLogger(TryCatchUtils.class);

    public static <T> T tryCatch(Callable<T> c) {
        return tryCatch(c, toRuntimeExceptionHandler());
    }

    public static void tryCatch(Runnable r) {
        try {
            r.run();
        } catch (RuntimeException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static <T> T tryCatch(Callable<T> c, ExceptionHandler<T> handler) {
        try {
            return c.call();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            return handler.handle(t);
        }
    }

    interface ExceptionHandler<T> extends Function<Throwable, T> {

        default T apply(Throwable t) {
            return handle(t);
        }

        T handle(Throwable t);
    }

    private static <T> ExceptionHandler<T> nullHandler(){
      return t -> null;
    }

    private static <T> ExceptionHandler<T> toRuntimeExceptionHandler(){
      return t -> {throw new RuntimeException(t);};
    }

}
