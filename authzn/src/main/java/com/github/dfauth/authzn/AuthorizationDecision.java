package com.github.dfauth.authzn;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public interface AuthorizationDecision {

    boolean isAllowed();

    boolean isDenied();

    BinaryOperator<AuthorizationDecision> or = (AuthorizationDecision _this, AuthorizationDecision _that) ->  _this.or(_that);

    BinaryOperator<AuthorizationDecision> and = (AuthorizationDecision _this, AuthorizationDecision _that) ->  _this.and(_that);

    default AuthorizationDecision or(AuthorizationDecision that) {
        return isAllowed() ? this : that;
    }

    default AuthorizationDecision and(AuthorizationDecision that) {
        return isDenied() ? new AuthorizationDecision() {
            @Override
            public boolean isAllowed() {
                return false;
            }

            @Override
            public boolean isDenied() {
                return true;
            }

            @Override
            public <R> R run(Callable<R> callable) throws SecurityException {
                throw new SecurityException("denied");
            }
        } : that;
    }

    default AuthorizationDecision map(UnaryOperator<AuthorizationDecision> f) {
        return f.apply(this);
    }

    default Optional<AuthorizationDecision> filter(Predicate<AuthorizationDecision> p) {
        return Optional.of(this).filter(p);
    }

    <R> R run(Callable<R> callable) throws SecurityException;

}
