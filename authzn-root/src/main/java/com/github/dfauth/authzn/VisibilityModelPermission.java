package com.github.dfauth.authzn;

import com.github.dfauth.scrub.VisibilityModel;

public class VisibilityModelPermission<T> extends Permission {

    private final VisibilityModel<Company, T> model;
    private final Company company;

    public VisibilityModelPermission(VisibilityModel<Company, T> model, Company company) {
        super(new ResourcePath("/rfq"));
        this.model = model;
        this.company = company;
    }

    @Override
    public boolean allows(AuthorizationDecision decision) {
        return model.isVisibleTo(company);
    }
}
