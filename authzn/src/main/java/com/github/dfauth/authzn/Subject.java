package com.github.dfauth.authzn;

import java.util.Set;

public interface Subject {
    Set<Principal> getPrincipals();
}
