package com.github.dfauth.authzn;

public interface Action {
    boolean implies(Action action);

    String name();
}
