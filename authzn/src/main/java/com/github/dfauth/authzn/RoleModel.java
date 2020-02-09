package com.github.dfauth.authzn;

import com.github.dfauth.authzn.Role;

import java.util.Set;
import java.util.function.Supplier;

public interface RoleModel {

    default Supplier<Set<Role>> asRoleSupplier() {
        return () -> roles();
    }

    Set<Role> roles();
}
