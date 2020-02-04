package com.github.dfauth.authzn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public interface ActionSet extends Action {

    class ActionSetCollection implements ActionSet {

        private static final Logger logger = LoggerFactory.getLogger(ActionSetCollection.class);

        private final Set<String> names;

        public ActionSetCollection(Set<String> names) {
            this.names = names.stream().collect(Collectors.mapping(n -> n.toUpperCase(), Collectors.toSet()));
        }

        @Override
        public boolean implies(Action action) {
            if(action instanceof AllActionsSet) {
                logger.debug(String.format("containsAll(%s,%s) returns false",this, action));
                return false;
            } else if(action instanceof ActionSetCollection) {
                boolean result = names.containsAll(((ActionSetCollection) action).names);
                logger.debug(String.format("containsAll(%s,%s) returns %s",this, action, result));
                return result;
            } else {
                boolean result = names.contains(action.name());
                logger.debug(String.format("containsAll(%s,%s) returns %s",this, action, result));
                return result;
            }
        }

        @Override
        public String name() {
            return toString();
        }

        @Override
        public String toString() {
            return names.toString();
        }
    }

    class AllActionsSet implements ActionSet {

        private AllActionsSet() {
        }

        @Override
        public boolean implies(Action action) {
            return true;
        }

        @Override
        public String name() {
            return toString();
        }

        @Override
        public String toString() {
            return "ALL Actions";
        }
    }

//    class NoActionSet implements ActionSet {
//
//        private NoActionSet() {
//        }
//
//        @Override
//        public boolean implies(Action action) {
//            return false;
//        }
//
//        @Override
//        public String toString() {
//            return "NO Action";
//        }
//    }
//
//    class AnyActionSet implements ActionSet {
//
//        private AnyActionSet() {
//        }
//
//        @Override
//        public boolean implies(Action action) {
//            return true;
//        }
//
//        @Override
//        public String toString() {
//            return "ANY Action";
//        }
//    }

    ActionSet ALL_ACTIONS = new AllActionsSet();

//    ActionSet NO_ACTIONS = new NoActionSet();

//    ActionSet ANY_ACTIONS = new AnyActionSet();

    static ActionSet parse(String name) {
        return parse(new HashSet(Arrays.asList(name.split(","))));
    }

    static ActionSet parse(Set<String> names) {
        return names.stream().filter(n -> isAllActions(n)).findFirst().map(x -> ALL_ACTIONS).orElseGet(() -> new ActionSetCollection(names));
    }

    static boolean isAllActions(String action) {
        return "*".equals(action);
    }
}


