package com.github.dfauth.scrub;

import java.util.function.Function;

public interface VisibilityModel<U, T> extends Function<U,T> {

    T render(U u);

    default T apply(U u) {
        return render(u);
    }

    boolean isVisibleTo(U ctx);
}
