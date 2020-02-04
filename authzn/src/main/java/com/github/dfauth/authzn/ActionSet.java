package com.github.dfauth.authzn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    ActionSet ALL_ACTIONS = new AllActionsSet();

    static ActionSet parse(String name) {
        return parse(new HashSet(Arrays.asList(name.split(","))));
    }

    static ActionSet parse(Set<String> names) {
        return names.stream().filter(n -> isAllActions(n)).findFirst().map(x -> ALL_ACTIONS).orElseGet(() -> new ActionSetCollection(names));
    }

    static boolean isAllActions(String action) {
        return "*".equals(action);
    }

    static ActionSet from(Action... actions) {
        return new ActionSetCollection(Stream.of(actions).map(a -> a.name()).collect(Collectors.toSet()));
    }

}


