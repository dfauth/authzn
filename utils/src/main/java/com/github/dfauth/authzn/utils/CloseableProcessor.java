package com.github.dfauth.authzn.utils;

import org.reactivestreams.Processor;

public interface CloseableProcessor<I, O> extends Processor<I,O> {
    void close();
}
