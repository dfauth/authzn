package com.github.dfauth.authzn;

public class TestUtils {

    static class RolePermission extends Permission {

        @Override
        protected boolean _equals(Object obj) {
            return true;
        }

        @Override
        protected boolean _implies(Permission permission, ResourceResolver resourceResolver) {
            return true;
        }
    }

    static class TestPermission extends ResourcePermission<TestAction> {

        public TestPermission(String resource, TestAction action) {
            super(resource, action);
        }

        @Override
        protected boolean _equals(Object obj) {
            return false;
        }

        @Override
        protected boolean _implies(Permission permission, ResourceResolver resourceResolver) {
            return false;
        }
    }

    enum TestAction implements Action<TestAction> {
        READ, WRITE;

        @Override
        public boolean implies(TestAction action) {
            return action.ordinal() < ordinal();
        }
    }
}
