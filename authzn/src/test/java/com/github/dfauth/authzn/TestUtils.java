package com.github.dfauth.authzn;

import com.github.dfauth.authzn.Action;
import com.github.dfauth.authzn.Permission;

import java.util.Set;

public class TestUtils {

    static class RolePermission extends Permission {

    }

    static class TestPermission extends Permission {

        public TestPermission(String resource, Set<Action> actions) {
            super(resource, actions);
        }

        public TestPermission(String resource, TestAction action) {
            super(resource, action);
        }
    }

    enum TestAction implements Action {
        READ, WRITE
    }
}
