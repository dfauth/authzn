package com.github.dfauth.authzn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;

import static com.github.dfauth.authzn.AuthorizationDecisionEnum.*;
import static com.github.dfauth.authzn.PermissionDecisionContext.NEVER;

public class Directive {

    private static final Logger logger = LoggerFactory.getLogger(Directive.class);

    private final Set<Principal> principals;
    private final Permission permission;
    private final AuthorizationDecision decision;

    public Directive(Principal principal, Permission permission) {
        this(Collections.singleton(principal), permission, ALLOW);
    }

    public Directive(Principal principal, Permission permission, String action) {
        this(Collections.singleton(principal), permission, AuthorizationDecisionEnum.valueOf(action));
    }

    public Directive(Set<Principal> principals, Permission permission) {
        this(principals, permission, ALLOW);
    }
    
    public Directive(Set<Principal> principals, Permission permission, AuthorizationDecision authznAction) {
        this.principals = principals;
        this.permission = permission;
        this.decision = authznAction;
    }

    public Set<Principal> getPrincipals() {
        return principals;
    }

    public AuthorizationDecision getDecision() {
        return decision;
    }

    public Permission getPermission() {
        return permission;
    }

    @Override
    public String toString() {
        return String.format("Directive(%s,%s,%s)",principals, permission, decision);
    }

    public DirectiveContext withResolver(ResourceResolver resolver) {
        return new DirectiveContext() {
            @Override
            public PermissionDecisionContext decisionContextFor(Permission permission) {
                if(Directive.this.permission.implies(permission, resolver)) {
                    PermissionDecisionContext result = new PermissionDecisionContextImpl(this);
                    logger.debug(String.format("decisionContextFor: %s on permission %s returns ",this, permission, result));
                    return result;
                } else {
                    logger.debug(String.format("decisionContextFor: %s on permission %s returns NEVER",this, permission));
                    return NEVER;
                }
            }

            @Override
            public AuthorizationDecision forPrincipal(Principal p) {
                return principals.contains(p) ? ALLOW : DENY;
            }

            @Override
            public String toString() {
                return String.format("DirectiveContext(%s)",Directive.this);
            }
        };
    }

    interface DirectiveContext {
        PermissionDecisionContext decisionContextFor(Permission permission);

        AuthorizationDecision forPrincipal(Principal p);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Set<Principal> principals;
        private Permission permission;
        private AuthorizationDecision decision = AuthorizationDecisionEnum.ALLOW;

        public void withPrincipals(Set<Principal> principals) {
            this.principals = principals;
        }

        public void withPermission(Permission permission) {
            this.permission = permission;
        }

        public void withAuthorizationDecision(AuthorizationDecision decision) {
            this.decision = decision;
        }

        public Directive build() {
            return new Directive(principals, permission, decision);
        }
    }
}
