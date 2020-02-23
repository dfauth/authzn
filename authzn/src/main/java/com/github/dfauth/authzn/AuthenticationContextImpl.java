package com.github.dfauth.authzn;

import java.util.Set;
import java.util.stream.Collectors;

import static com.github.dfauth.authzn.PrincipalType.ROLE;
import static com.github.dfauth.authzn.PrincipalType.USER;

public class AuthenticationContextImpl implements AuthenticationContext<UserModelImpl> {

    private String token;
    private UserModelImpl payload;

    public AuthenticationContextImpl(String token, UserModelImpl payload) {
        this.token = token;
        this.payload = payload;
    }

    @Override
    public String token() {
        return token;
    }

    @Override
    public String userId() {
        return payload.getUserId();
    }

    @Override
    public UserModelImpl payload() {
        return payload;
    }

    @Override
    public Subject getSubject() {
        ImmutableSubject userSubject = ImmutableSubject.of(USER.of(userId()));
        Set<Principal> roles = payload.roles().stream()
                .map(r -> ROLE.of(r.getRolename()))
                .collect(Collectors.toSet());
        return userSubject.with(roles);
    }

}
