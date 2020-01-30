package com.github.dfauth.authzn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

interface ActionSet {

    boolean containsAll(ActionSet actions);

    boolean implies(ActionSet actions);

    class ActionSetCollection implements ActionSet {

        private static final Logger logger = LoggerFactory.getLogger(ActionSetCollection.class);

        private final Set<String> names;

        public ActionSetCollection(Set<String> names) {
            this.names = names.stream().collect(Collectors.mapping(n -> n.toUpperCase(), Collectors.toSet()));
        }

        @Override
        public boolean containsAll(ActionSet actions) {
            if(actions instanceof AllActionsSet) {
                logger.debug(String.format("containsAll(%s,%s) returns false",this, actions));
                return false;
            } if(actions instanceof NoActionSet) {
                logger.debug(String.format("containsAll(%s,%s) returns true",this, actions));
                return true;
            } else {
                boolean result = names.containsAll(((ActionSetCollection) actions).names);
                logger.debug(String.format("containsAll(%s,%s) returns %s",this, actions, result));
                return result;
            }
        }

        @Override
        public boolean implies(ActionSet actions) {
            return containsAll(actions);
        }

        @Override
        public String toString() {
            return names.toString();
        }
    }

    class AllActionsSet implements ActionSet {

        private static final Logger logger = LoggerFactory.getLogger(AllActionsSet.class);

        private AllActionsSet() {
            super();
        }

        @Override
        public boolean containsAll(ActionSet actions) {
            logger.debug(String.format("containsAll(%s,%s) returns true",this, actions));
            return true;
        }

        @Override
        public boolean implies(ActionSet actions) {
            return containsAll(actions);
        }

        @Override
        public String toString() {
            return "ALL Actions";
        }
    }

    class NoActionSet implements ActionSet {

        private static final Logger logger = LoggerFactory.getLogger(NoActionSet.class);

        private NoActionSet() {
            super();
        }

        @Override
        public boolean containsAll(ActionSet actions) {
            logger.debug(String.format("containsAll(%s,%s) returns false",this, actions));
            return false;
        }

        @Override
        public boolean implies(ActionSet actions) {
            return containsAll(actions);
        }

        @Override
        public String toString() {
            return "NO Action";
        }
    }

    ActionSet ALL_ACTIONS = new AllActionsSet();

    ActionSet NO_ACTIONS = new AllActionsSet();

    static ActionSet parse(String name) {
        return parse(Collections.singleton(name));
    }

    static ActionSet parse(Set<String> names) {
        return names.stream().filter(n -> Actions.isAllActions(n)).findFirst().map(x -> ALL_ACTIONS).orElseGet(() -> {
            if(names.isEmpty()) {
                return NO_ACTIONS;
            } else {
                return new ActionSetCollection(names);
            }
        });
    }
}


