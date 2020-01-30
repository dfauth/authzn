package com.github.dfauth.authzn;

import com.github.dfauth.authzn.config.PolicyConfig;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import static com.github.dfauth.authzn.PrincipalType.ROLE;
import static com.github.dfauth.authzn.PrincipalType.USER;
import static com.github.dfauth.authzn.TestUtils.*;
import static org.testng.Assert.assertTrue;

public class ConfigTest {

    private static final Logger logger = LoggerFactory.getLogger(ConfigTest.class);

    @Test(groups = {"hocon"})
    public void testIt() {

        String TEST = "authzn {\n" +
                "  policy {\n" +
                "    directives = [allow-all]\n" +
                "    directive {\n" +
                "      allow-all {\n" +
                "        roles = [user]\n" +
                "        resource = /blah\n" +
                "        actions = [write]\n" +
                "        decision = allow\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        PolicyConfig config = new PolicyConfig("authzn", ConfigFactory.parseString(TEST));
        AuthorizationPolicy policy = config.getPolicy();
        assertTrue(policy.permit(ROLE.of("user").with(USER.of("fred")), new BasePermission("/blah", TestAction.WRITE)).isAllowed());
    }

}
