package com.github.dfauth.authzn.avro;

import com.github.dfauth.authzn.*;
import com.github.dfauth.avro.authzn.Decision;

import java.util.function.Function;
import java.util.stream.Collectors;

public class Transformations {

    public static Function<String, Principal> toPrincipal = p -> {
        String[] arr = p.split(":");
        return PrincipalType.valueOf(arr[0]).of(arr[1], arr[2]);
    };

    public static Function<Directive, com.github.dfauth.avro.authzn.Directive> toAvro = d -> {
        com.github.dfauth.avro.authzn.Directive.Builder builder = com.github.dfauth.avro.authzn.Directive.newBuilder();
        builder.setPrincipals(d.getPrincipals().stream().map(p -> p.getName()).collect(Collectors.toList()))
        .setResource(d.getResourcePath().toString())
        .setActions(d.getActionSet().toString())
        .setDecision(d.getDecision().isAllowed() ? Decision.ALLOW : Decision.DENY);
        return builder.build();
    };

    public static Function<com.github.dfauth.avro.authzn.Directive, Directive> fromAvro = avro -> {
        Directive directive = new Directive(avro.getPrincipals().stream().map(toPrincipal).collect(Collectors.toSet()),
                new ResourcePath(avro.getResource()),
                ActionSet.parse(avro.getActions()),
                avro.getDecision() == Decision.ALLOW ? AuthorizationDecisionEnum.ALLOW : AuthorizationDecisionEnum.DENY
                );
        return directive;
    };

}
