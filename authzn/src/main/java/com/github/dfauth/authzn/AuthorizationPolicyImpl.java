package com.github.dfauth.authzn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class AuthorizationPolicyImpl extends AuthorizationPolicy {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationPolicyImpl.class);

    ResourceNode<Directive> directiveHierarchy = new RootResourceNode<>();

    public AuthorizationPolicyImpl(Directive directive) {
        this(Collections.singletonList(directive));
    }

    public AuthorizationPolicyImpl(List<Directive> directives) {
        directives.forEach(d -> {
            directiveHierarchy.add(new SimpleResource<>(d.getResourcePath(), d));
        });
    }

    //@Override
//    protected ResourceResolver getResourceResolver() {
//        return resource1 -> (ResourceResolver.ResourceResolverContext) resource2 -> {
//            boolean result = hierarchy.findAllResourcesInPath(resource2.getIterablePath()).stream().filter((Predicate<Resource<String, Directive>>) resource ->
//                    resource1.implies(resource2))
//                    .findFirst()
//                    .isPresent();
//            logger.debug(String.format("%s implies %s: %s",resource1,resource2,result));
//            return result;
//        };
//    }

    @Override
    Collection<Directive> directivesFor(ResourcePath resourcePath) {
        return directiveHierarchy.findAllInPath(resourcePath.getPath());
    }
}
