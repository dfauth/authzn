package com.github.dfauth.authzn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

import static com.github.dfauth.authzn.Assertions.WasRunAssertion.State.NOT_RUN;
import static com.github.dfauth.authzn.Assertions.WasRunAssertion.State.WAS_RUN;
import static org.testng.Assert.assertTrue;

public class Assertions {

    private static final Logger logger = LoggerFactory.getLogger(Assertions.class);

    public static void assertAllowed(AuthorizationDecision decision) {
        org.testng.Assert.assertTrue(decision.isAllowed());
    }

    public static void assertDenied(AuthorizationDecision decision) {
        org.testng.Assert.assertTrue(decision.isDenied());
    }

    public static void assertWasRun(WasRunAssertion a) {
        org.testng.Assert.assertTrue(a.wasRun());
    }

    public static Assertion create(Runnable runnable) {
        return () -> runnable.run();
    }

    public static Assertion assertTrue(Callable<Boolean> callable) {
        return () -> {
            try {
                org.testng.Assert.assertTrue(callable.call());
            } catch (Exception e) {
                logger.info(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        };
    }

    public static Assertion fail(String msg) {
        return () -> fail(msg);
    }

    public static <T> AssertionBuilder<T> assertOptional(Optional<T> o) {
        return AssertionBuilder.of(o);
    }

    static class WasRunAssertion {
        private State state = NOT_RUN;
        public WasRunAssertion run() {
            state = WAS_RUN;
            return this;
        }

        public State state() {
            return state;
        }

        public boolean wasRun() {
            return state.wasRun();
        }

        static enum State {
            NOT_RUN, WAS_RUN;

            public boolean wasRun() {
                return this == WAS_RUN;
            }
        }
    }

    interface Assertion {
        void doAssert();
    }

    static class AssertionBuilder<T> {

        private final Optional<T> o;

        public AssertionBuilder(Optional<T> o) {
            this.o = o;
        }

        public static <T> AssertionBuilder of(Optional<T> o) {
            return new AssertionBuilder(o);
        }

        public Assertion withPredicate(Predicate<T> p) {
            return o.map(t -> (Assertion) () -> Assert.assertTrue(p.test(t))).orElse(Assertions.fail("Oops"));
        }
    }
}
