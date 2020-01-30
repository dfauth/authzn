package com.github.dfauth.authzn.config;

import com.github.dfauth.authzn.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiConsumer;

import static com.github.dfauth.authzn.PrincipalType.ROLE;
import static com.github.dfauth.authzn.PrincipalType.USER;

public class PolicyConfig {

    private static final Logger logger = LoggerFactory.getLogger(PolicyConfig.class);
    private final AuthorizationPolicy policy;

    private BiConsumer<List<Directive>, ? super String> accumulator(Config config) {
        return (BiConsumer<List<Directive>, String>) (directives, s) -> {
            Directive.Builder builder = Directive.builder();
            Config c = config.getConfig(String.format("directive.%s",s));
            Set<Principal> principals = optionalStringList(c, "users").stream().map(u -> USER.of(u)).collect(() -> new HashSet<Principal>(), (acc, u) -> {
                acc.add(u);
            }, (acc, u) -> {
            });

            principals.addAll(optionalStringList(c, "roles").stream().map(r -> ROLE.of(r)).collect(() -> new HashSet<Principal>(), (acc, r) -> {
                acc.add(r);
            }, (acc, u) -> {}));

            builder.withPrincipals(principals);

            String resource = c.getString("resource");
            List<String> actions = c.getStringList("actions");
            builder.withPermission(new DirectivePermission(resource, actions));
            optionalString(config, "decision").ifPresent(v -> {
                builder.withAuthorizationDecision(AuthorizationDecisionEnum.valueOf(v.toUpperCase()));
            });
            directives.add(builder.build());
        };
    }


public PolicyConfig(String path, Config config) {
        policy = loadPolicy(config.getConfig(String.format("%s.policy", path)));
    }

    protected AuthorizationPolicy loadPolicy(Config config) {
        List<String> directives = config.getStringList("directives");
        return new AuthorizationPolicyImpl(directives.stream().collect(() -> new ArrayList<Directive>(), accumulator(config), (d1, d2) -> {}));
    }

    private static Optional<String> optionalString(Config config, String key) {
        try {
            return Optional.of(config.getString(key));
        } catch (ConfigException.Missing e){
            return Optional.empty();
        }
    }

    private static List<String> optionalStringList(Config config, String key) {
        try {
            return config.getStringList(key);
        } catch (ConfigException.Missing e){
            return Collections.emptyList();
        }
    }

    public AuthorizationPolicy getPolicy() {
        return policy;
    }
}
