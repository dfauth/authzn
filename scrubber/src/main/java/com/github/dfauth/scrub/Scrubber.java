package com.github.dfauth.scrub;

import java.util.function.Function;

public interface Scrubber<O> extends Function<VisibilityModel,O> {

    O scrub(VisibilityModel model);

    default O apply(VisibilityModel model) {
        return scrub(model);
    }
}
