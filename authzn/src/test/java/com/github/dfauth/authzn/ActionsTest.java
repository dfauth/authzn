package com.github.dfauth.authzn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import static com.github.dfauth.authzn.ActionSet.ALL_ACTIONS;
import static com.github.dfauth.authzn.ActionsTest.TestAction.*;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ActionsTest {

    private static final Logger logger = LoggerFactory.getLogger(ActionsTest.class);

    @Test(groups = {"action"})
    public void testParse() {

        {
            ActionSet actionSet = ActionSet.parse("create, read, update, delete");
            assertTrue(ALL_ACTIONS.implies(CREATE));
            assertTrue(actionSet.implies(CREATE));
            assertFalse(CREATE.implies(ALL_ACTIONS));
            assertFalse(actionSet.implies(ALL_ACTIONS));
            assertFalse(CREATE.implies(READ));
            assertTrue(READ.implies(READ));
        }

        {
            ActionSet actionSet = ActionSet.parse("create, read, update, delete");
            assertTrue(ALL_ACTIONS.implies(TestAction2.CREATE));
            assertTrue(actionSet.implies(TestAction2.CREATE));
            assertFalse(TestAction2.CREATE.implies(ALL_ACTIONS));
            assertFalse(actionSet.implies(ALL_ACTIONS));
            assertFalse(TestAction2.CREATE.implies(TestAction2.READ));
            assertTrue(TestAction2.READ.implies(TestAction2.CREATE));
            assertTrue(TestAction2.READ.implies(TestAction2.READ));
        }

    }

    enum TestAction implements Action {
        CREATE, READ, UPDATE, DELETE;

        @Override
        public boolean implies(Action action) {
            return this == action;
        }
    }

    enum TestAction2 implements Action {
        CREATE, READ, UPDATE, DELETE;

        @Override
        public boolean implies(Action action) {
            if(action instanceof TestAction2) {
                TestAction2 action2 = (TestAction2) action;
                return ordinal() >= action2.ordinal();
            }
            return this == action;
        }
    }
}
