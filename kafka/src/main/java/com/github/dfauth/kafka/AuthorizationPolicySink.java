package com.github.dfauth.kafka;

import akka.stream.javadsl.Sink;
import com.github.dfauth.authzn.AuthorizationPolicyImpl;
import com.github.dfauth.authzn.Directive;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;


public class AuthorizationPolicySink extends AuthorizationPolicyImpl implements Subscriber<Directive> {

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
    public void onError(Throwable throwable) {

    }

    @Override
    public void onComplete() {

    }
}
