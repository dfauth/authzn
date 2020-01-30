package com.github.dfauth.authzn;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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

    public Subject with(Principal p) {
        HashSet<Principal> tmp = new HashSet<>(principals);
        tmp.add(p);
        return of(tmp);
    }

    @Override
    public String toString() {
        return String.format("ImmutablePrincipal(%s)",principals);
    }
}
