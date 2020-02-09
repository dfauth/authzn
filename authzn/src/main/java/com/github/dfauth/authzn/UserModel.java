package com.github.dfauth.authzn;

import java.util.function.Supplier;

public interface UserModel {
    Company company();

    default Supplier<Company> asCompanySupplier() {
        return () -> company();
    }

}
