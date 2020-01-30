package com.github.dfauth.authzn;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ImmutablePrincipal implements Principal {

    private final PrincipalType principalType;
    private final String source;
    private final String name;

    ImmutablePrincipal(PrincipalType principalType, String name) {
        this(principalType,"default",name);
    }

    ImmutablePrincipal(PrincipalType principalType, String source, String name) {
        this.principalType = principalType;
        this.source = source;
        this.name = name;
    }

    @Override
    public PrincipalType getPrincipalType() {
        return principalType;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return principalType.hashCode() | source.hashCode() | name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(obj == this) {
            return true;
        }
        if(obj instanceof ImmutablePrincipal) {
            ImmutablePrincipal other = (ImmutablePrincipal) obj;
            return principalType.equals(other.principalType) && source.equals(other.source) && name.equals(other.name);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("ImmutablePrincipal(%s,%s,%s)",principalType,source,name);
    }

    public Subject asSubject() {
        return () -> Collections.singleton(this);
    }

    public Subject with(ImmutablePrincipal principal) {
        return with(Collections.singleton(principal));
    }

    public Subject with(Set<ImmutablePrincipal> principals) {
        Set<Principal> tmp = new HashSet<Principal>(principals);
        tmp.add(this);
        return ImmutableSubject.of(tmp);
    }
}
