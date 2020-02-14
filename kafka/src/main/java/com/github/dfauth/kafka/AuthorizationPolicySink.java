package com.github.dfauth.kafka;

import akka.stream.javadsl.Sink;
import com.github.dfauth.authzn.AuthorizationPolicyImpl;
import com.github.dfauth.authzn.Directive;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizationPolicySink extends AuthorizationPolicyImpl implements Subscriber<Directive> {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationPolicySink.class);

    private Subscription subscription;

    public Sink<Directive, ?> asSink() {
        return Sink.fromSubscriber(this);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(Directive directive) {
        add(directive);
        subscription.request(1);
    }

    @Override
    public void onError(Throwable t) {
        logger.error(t.getMessage(), t);
    }

    @Override
    public void onComplete() {
        logger.error("complete");
    }
}
