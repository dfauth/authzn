package com.github.dfauth.authzn.utils;

import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;


public abstract class AbstractProcessor<I, O> implements Processor<I,O> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractProcessor.class);

    private Optional<String> name = Optional.empty();
    protected Optional<Subscriber> subscriberOpt = Optional.empty();
    private Optional<Subscription> subscriptionOpt = Optional.empty();

    @Override
    public void subscribe(Subscriber<? super O> s) {
        subscriberOpt = Optional.of(s);
        subscriptionOpt.ifPresent(q -> init());
    }

    @Override
    public void onSubscribe(Subscription s) {
        subscriptionOpt = Optional.of(s);
        subscriberOpt.ifPresent(q -> init());
    }

    @Override
    public void onNext(I i) {
        subscriberOpt.ifPresent(s -> s.onNext(transform(i)));
    }

    protected abstract O transform(I i);

    @Override
    public void onError(Throwable t) {
        subscriberOpt.ifPresent(s -> s.onError(t));
    }

    @Override
    public void onComplete() {
        subscriberOpt.ifPresent(s -> s.onComplete());
    }

    protected void init() {
        subscriberOpt.ifPresent(s -> {
            subscriptionOpt.ifPresent(_s -> {
                s.onSubscribe(_s);
                logger.info(withName("subscribed"));
            });
        });
    }

    protected String withName(String str){
        return name.map(s -> s+" "+str).orElse(str);
    }

}

