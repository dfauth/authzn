package com.github.dfauth.kafka;

import com.github.dfauth.authzn.Permission;
import com.github.dfauth.authzn.ResourcePath;


public class PolicyPermission extends Permission {
    public PolicyPermission() {
        super(new ResourcePath("authorizationPolicy"));
    }
}
