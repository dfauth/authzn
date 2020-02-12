package com.github.dfauth.authzn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class AuthorizationPolicyImpl extends AuthorizationPolicy {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationPolicyImpl.class);

    ResourceNode<Directive> directiveHierarchy = new RootResourceNode<>();

    public AuthorizationPolicyImpl() {
        this(Collections.emptyList());
    }

    public AuthorizationPolicyImpl(Directive directive) {
        this(Collections.singletonList(directive));
    }

    public AuthorizationPolicyImpl(List<Directive> directives) {
        directives.forEach(d -> {
            add(d);
        });
    }

    protected void add(Directive d) {
        directiveHierarchy.add(new SimpleResource<>(d.getResourcePath(), d));
    }

    @Override
    Collection<Directive> directivesFor(ResourcePath resourcePath) {
        return directiveHierarchy.findAllInPath(resourcePath.getPath());
    }
}
