package com.github.dfauth.authzn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;

import static com.github.dfauth.authzn.ActionSet.ALL_ACTIONS;
import static com.github.dfauth.authzn.AuthorizationDecisionEnum.*;

public class Directive {

    private static final Logger logger = LoggerFactory.getLogger(Directive.class);

    private final Set<Principal> principals;
    private final ResourcePath resource;
    private final ActionSet actionSet;
    private final AuthorizationDecision decision;

    public Directive(Principal principal) {
        this(Collections.singleton(principal), ResourcePath.root(), ALL_ACTIONS);
    }

    public Directive(Set<Principal> principals) {
        this(principals, ResourcePath.root(), ALL_ACTIONS);
    }

    public Directive(Principal principal, ResourcePath resource) {
        this(Collections.singleton(principal), resource, ALL_ACTIONS);
    }

    public Directive(Set<Principal> principals, ResourcePath resource) {
        this(principals, resource, ALL_ACTIONS);
    }

    public Directive(Principal principal, ResourcePath resource, ActionSet actionSet) {
        this(Collections.singleton(principal), resource, actionSet, ALLOW);
    }

    public Directive(Principal principal, ResourcePath resource, ActionSet actionSet, String action) {
        this(Collections.singleton(principal), resource, actionSet, AuthorizationDecisionEnum.valueOf(action));
    }

    public Directive(Set<Principal> principals, ResourcePath resource, ActionSet actionSet) {
        this(principals, resource, actionSet, ALLOW);
    }
    
    public Directive(Set<Principal> principals, ResourcePath resource, ActionSet actionSet, AuthorizationDecision authznAction) {
        this.principals = principals;
        this.resource = resource;
        this.actionSet = actionSet;
        this.decision = authznAction;
    }

    public Set<Principal> getPrincipals() {
        return principals;
    }

    public AuthorizationDecision getDecision() {
        return decision;
    }

    public ResourcePath getResourcePath() {
        return resource;
    }

    @Override
    public String toString() {
        return String.format("Directive(%s,%s,%s,%s)",principals, resource, actionSet, decision);
    }

//    public DirectiveContext withResolver(ResourceResolver resolver) {
//        return new DirectiveContext() {
//            @Override
//            public PermissionDecisionContext decisionContextFor(ResourcePath resource) {
//                if(Directive.this.resource.implies(Directive.this.resource, resolver)) {
//                    PermissionDecisionContext result = new PermissionDecisionContextImpl(this);
//                    logger.debug(String.format("decisionContextFor: %s on permission %s returns ",this, Directive.this.resource, result));
//                    return result;
//                } else {
//                    logger.debug(String.format("decisionContextFor: %s on permission %s returns NEVER",this, Directive.this.resource));
//                    return NEVER;
//                }
//            }
//
//            @Override
//            public AuthorizationDecision forPrincipal(Principal p) {
//                return principals.contains(p) ? ALLOW : DENY;
//            }
//
//            @Override
//            public String toString() {
//                return String.format("DirectiveContext(%s)",Directive.this);
//            }
//        };
//    }

    interface DirectiveContext {
        PermissionDecisionContext decisionContextFor(ResourcePath resource);

        AuthorizationDecision forPrincipal(Principal p);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Set<Principal> principals;
        private ResourcePath resource;
        private AuthorizationDecision decision = AuthorizationDecisionEnum.ALLOW;
        private ActionSet actionSet;

        public void withPrincipals(Set<Principal> principals) {
            this.principals = principals;
        }

        public void withResource(ResourcePath resource) {
            this.resource = resource;
        }

        public void withActionSet(ActionSet actionSet) {
            this.actionSet = actionSet;
        }

        public void withAuthorizationDecision(AuthorizationDecision decision) {
            this.decision = decision;
        }

        public Directive build() {
            return new Directive(principals, resource, actionSet, decision);
        }
    }
}
