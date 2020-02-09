package com.github.dfauth.authzn;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class ScrubbingPermissionTestCase extends FlintstonesTestScenario {

    @Test
    public void testIt() {
        // directives
        List<Directive> directives = new ArrayList();

        AuthorizationPolicy policy = new AuthorizationPolicyImpl(directives);

    }
}
