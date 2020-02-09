package com.github.dfauth.authzn;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ImmutableSubject implements Subject {

    private final Set<Principal> principals;

    public static ImmutableSubject of(Principal... principals) {
        return new ImmutableSubject(new HashSet(Arrays.asList(principals)));
    }

    public static ImmutableSubject of(Set<Principal> principals) {
        return new ImmutableSubject(principals);
    }

    ImmutableSubject(Set<Principal> principals) {
        this.principals = Collections.unmodifiableSet(principals);
    }

    @Override
    public Set<Principal> getPrincipals() {
        return principals;
    }

    public ImmutableSubject with(Principal p) {
        return with(Collections.singleton(p));
    }
    public ImmutableSubject with(Set<Principal> p) {
        HashSet<Principal> tmp = new HashSet<>(principals);
        tmp.addAll(p);
        return of(tmp);
    }

    @Override
    public String toString() {
        return String.format("ImmutablePrincipal(%s)",principals);
    }
}
