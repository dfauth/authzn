package com.github.dfauth.authzn;

import static com.github.dfauth.authzn.AuthorizationDecisionEnum.DENY;

public interface PermissionDecisionContext {

    PermissionDecisionContext NEVER = p -> DENY;

    AuthorizationDecision withPrincipal(Principal p);
}
