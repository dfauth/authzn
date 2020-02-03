package com.github.dfauth.authzn.config;

import com.github.dfauth.authzn.ActionSet;
import com.github.dfauth.authzn.ResourcePermission;

public class DirectivePermission extends ResourcePermission<ActionSet> {
    public DirectivePermission(String resource, ActionSet actions) {
        super(resource, actions);
    }
}
