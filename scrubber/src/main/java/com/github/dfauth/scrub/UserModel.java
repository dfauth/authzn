package com.github.dfauth.scrub;

import java.util.function.Supplier;

public interface UserModel extends Supplier<Company> {
    Company company();

    @Override
    default Company get() {
        return company();
    }
}
