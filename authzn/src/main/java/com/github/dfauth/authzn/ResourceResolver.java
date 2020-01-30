package com.github.dfauth.authzn;

public interface ResourceResolver {

    ResourceResolverContext resource(Resource resource);

    interface ResourceResolverContext {

        public boolean implies(Resource resource);
    }
}


