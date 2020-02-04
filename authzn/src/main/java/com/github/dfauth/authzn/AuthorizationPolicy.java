package com.github.dfauth.authzn;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static com.github.dfauth.authzn.AuthorizationDecisionEnum.DENY;

public abstract class AuthorizationPolicy {

    public final AuthorizationDecision permit(Subject subject, Permission permission) {

        AuthorizationDecision decision =
               directivesFor(permission.getResourcePath()).stream()             // for every directive associated with the given resource, most specific first
                       .filter(d -> permission.getAction()                      // filter out directives
                               .map(a -> d.appliesToAction(a))                  // where the action is not allowed
                               .orElse(true)                              // allowing it through where no action is available
                       )
                       .filter(d -> !subject.getPrincipals().stream()            // filter out directives where no principal is acceptable
                                    .filter(p -> d.appliesToPrincipal(p))        // by collecting all acceptable principals and
                                    .collect(Collectors.toList()).isEmpty()      // testing that the list is not empty
                       )
                       .map(d -> d.getDecision())                                // extract the decision from the directive
                       .findFirst()                                              // the first decision (ie. nearest in teh resource hierarch) has priority
                       .orElse(DENY);                                            // but  if none is found, deny

        // wrap it in an anonymous class to allow us to log more information
        return new AuthorizationDecision(){
            @Override
            public boolean isAllowed() {
                return decision.isAllowed();
            }

            @Override
            public boolean isDenied() {
                return decision.isDenied();
            }

            @Override
            public <R> R run(Callable<R> callable) throws SecurityException {
                try {
                    return decision.run(callable);
                } catch(SecurityException e) {
                    throw new SecurityException(subject+" is not authorized to perform actions "+permission.getAction()+" on resource "+permission.getResourcePath());
                }
            }
        };
    }

//    protected abstract ResourceResolver getResourceResolver();

    abstract Collection<Directive> directivesFor(ResourcePath resourcePath);
}
