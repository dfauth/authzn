package com.github.dfauth.authzn.utils;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;


public abstract class AbstractSubscriber<T> implements Subscriber<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractSubscriber.class);

    private Optional<Subscription> subscriptionOpt = Optional.empty();

    @Override
    public void onSubscribe(Subscription s) {
        subscriptionOpt = Optional.of(s);
        _onSubscribe(s);
    }

    // force the initial request
    protected abstract void _onSubscribe(Subscription s);

    @Override
    public abstract void onNext(T t);

    @Override
    public void onError(Throwable t) {
        logger.error(t.getMessage(), t);
    }

    @Override
    public void onComplete() {
        logger.debug("complete");
    }
}

