package com.github.dfauth.authzn;

public class DirectiveResource extends SimpleResource<Directive> {

    public DirectiveResource(Directive directive) {
        super(directive.getResourcePath(), directive);
    }
}
