package com.github.dfauth.scrub;

import java.util.function.Function;

public interface ScrubberBuilder<O>  extends Function<UserModel,Scrubber<O>> {

    Scrubber<O> build(UserModel u);

    default Scrubber<O> apply(UserModel u) {
        return build(u);
    }
}
