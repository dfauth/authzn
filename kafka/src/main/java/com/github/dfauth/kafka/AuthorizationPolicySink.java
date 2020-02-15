package com.github.dfauth.kafka;

import akka.stream.javadsl.Sink;
import com.github.dfauth.authzn.AuthorizationPolicyImpl;
import com.github.dfauth.authzn.Directive;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizationPolicySink extends AuthorizationPolicyImpl implements Subscriber<AuthenticationEnvelope<Directive>> {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationPolicySink.class);

    private Subscription subscription;

    public AuthorizationPolicySink(Directive initialDirective) {
        add(initialDirective);
    }

    public Sink<AuthenticationEnvelope<Directive>, ?> asSink() {
        return Sink.fromSubscriber(this);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(AuthenticationEnvelope<Directive> envelope) {
        try {
            permit(envelope.getSubject(), new PolicyPermission()).run(() -> {
                add(envelope.payload());
                subscription.request(1);
                return null;
            });
        } catch (SecurityException e) {
            logger.error(e.getMessage(), e);
        }
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
