package com.github.dfauth.authzn.config;

import com.github.dfauth.authzn.Permission;

import java.util.List;

public class DirectivePermission extends Permission {
    public DirectivePermission(String resource, List<String> actions) {
        super(resource, actions);
    }
}
