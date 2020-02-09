package com.github.dfauth.scrub;

import com.github.dfauth.authzn.Company;

import java.util.function.Function;

public interface ScrubberBuilder<O>  extends Function<Company,Scrubber<O>> {

    Scrubber<O> build(Company c);

    default Scrubber<O> apply(Company c) {
        return build(c);
    }
}
