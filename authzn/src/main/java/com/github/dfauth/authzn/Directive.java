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

    public boolean appliesToAction(Action a) {
        return actionSet.implies(a);
    }

    public boolean appliesToPrincipal(Principal p) {
        return principals.contains(p);
    }

    public ActionSet getActionSet() {
        return actionSet;
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
