package com.github.dfauth.authzn.utils;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;


public abstract class AbstractPublisher<T> implements Publisher<T>, Subscription {

    private static final Logger logger = LoggerFactory.getLogger(AbstractPublisher.class);

    protected Optional<Subscriber> subscriberOpt = Optional.empty();

    @Override
    public void subscribe(Subscriber<? super T> s) {
        subscriberOpt = Optional.of(s);
        s.onSubscribe(this);
    }

    @Override
    public abstract void request(long n);

    @Override
    public abstract void cancel();
}

