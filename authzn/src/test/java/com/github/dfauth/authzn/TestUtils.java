package com.github.dfauth.authzn;

public class TestUtils {

    static class RolePermission extends Permission {
    }

    static class TestPermission extends Permission {

        public TestPermission(String resource, Action action) {
            super(new ResourcePath(resource), action);
        }
    }

    enum TestAction implements Action {
        READ, WRITE;

        @Override
        public boolean implies(Action action) {
            return false;
        }
    }
}
