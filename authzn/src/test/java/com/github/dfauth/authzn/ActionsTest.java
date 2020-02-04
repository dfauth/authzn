package com.github.dfauth.authzn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import static com.github.dfauth.authzn.ActionSet.ALL_ACTIONS;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ActionsTest {

    private static final Logger logger = LoggerFactory.getLogger(ActionsTest.class);

    @Test(groups = {"action"})
    public void testParse() {

        {
            ActionSet actionSet = ActionSet.parse("create, read, update, delete");
            assertTrue(ALL_ACTIONS.implies(TestAction.CREATE));
            assertTrue(actionSet.implies(TestAction.CREATE));
            assertFalse(TestAction.CREATE.implies(ALL_ACTIONS));
            assertFalse(actionSet.implies(ALL_ACTIONS));
        }

    }

    private enum TestAction implements Action {
        CREATE, READ, UPDATE, DELETE;

        @Override
        public boolean implies(Action action) {
            return this == action;
        }
    }
}
