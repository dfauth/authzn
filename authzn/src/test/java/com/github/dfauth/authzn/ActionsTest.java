package com.github.dfauth.authzn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.testng.Assert.assertTrue;

public class ActionsTest {

    private static final Logger logger = LoggerFactory.getLogger(ActionsTest.class);

    @Test
    public void testParse() {

        {

//            TestPermission perm = new TestPermission("/a/b/c/d/e/f", TestAction.CREATE);
//
//            Stream.of(TestAction.values()).forEach(v -> {
//                assertTrue(perm.getAction().containsAll(ActionSet.parse(v.name())));
//            });

//            perm.getAction().stream().forEach(v -> {
//                assertTrue(Arrays.asList(TestAction.values()).contains(v));
//            });
        }

        {
//            TestPermission perm = new TestPermission("/a/b/c/d/e/f", Actions.using(TestAction.class).parse("creaTE , dElEtE"));
//
//            List<TestAction> actions = Arrays.asList(new TestAction[]{TestAction.DELETE, TestAction.CREATE});
//
//            actions.forEach(v -> {
//                assertTrue(perm.getAction().containsAll(ActionSet.parse(v.name())));
//            });


//            perm.getAction().stream().forEach(v -> {
//                assertTrue(actions.contains(v));
//            });
        }

    }

    private class TestPermission extends Permission<TestAction> {

        public TestPermission(String resource, TestAction action) {
            super(resource, action);
        }

    }

    private enum TestAction implements Action<TestAction> {
        READ, CREATE, UPDATE, DELETE;

        @Override
        public boolean implies(TestAction action) {
            return action.ordinal() < ordinal();
        }
    }
}
