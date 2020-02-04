package com.github.dfauth.authzn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.dfauth.authzn.AuthorizationDecisionEnum.DENY;

public abstract class AuthorizationPolicy {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationPolicy.class);

    public final AuthorizationDecision permit(Subject subject, Permission... permissions) {
        return Stream.of(permissions).map(p -> permit(subject, p)).reduce((d1, d2) -> {
            AuthorizationDecision result = d1.and(d2);
            logger.info("d1: "+d1+" d2: "+d2+" result: "+result);
            return result;
        }).orElse(DENY);
    }

    public final AuthorizationDecision permit(Subject subject, Permission permission) {

        AuthorizationDecision decision =
               directivesFor(permission.getResourcePath()).stream()             // for every directive associated with the given resource, most specific first,
                       .filter(d -> permission.getAction()                      // filter out directives...
                               .map(a -> d.appliesToAction(a))                  // ...where the action is disallowed by the directive...
                               .orElse(true)                              // ...allowing it through where no action is specified
                       )
                       .filter(d -> !subject.getPrincipals().stream()            // for surviving directives, filter out directives where no principal is acceptable...
                                    .filter(p -> d.appliesToPrincipal(p))        // ...by collecting all acceptable principals and...
                                    .collect(Collectors.toList()).isEmpty()      // ...testing that the resulting list is not empty (ie. at least one principal is allowed...
                       )                                                         // ...to perform the requested action)
                       .map(d -> d.getDecision())                                // extract the decision from the directive
                       .findFirst()                                              // the first decision (ie. the evaluation of the nearest surviving directive in the resource hierarchy) has priority
                       .filter(d -> d.isAllowed())                               // if positive...
                       .filter(d -> permission.allows(d))                        // ...a final callback allows the permission to reverse a positive decision allowing the action in order to allow custom logic
                                                                                 //
                       .orElse(DENY);                                            // but  if no directive survived, deny

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

            @Override
            public String toString() {
                return "AuthorizationDecision("+decision+")";
            }
        };
    }

    abstract Collection<Directive> directivesFor(ResourcePath resourcePath);
}
