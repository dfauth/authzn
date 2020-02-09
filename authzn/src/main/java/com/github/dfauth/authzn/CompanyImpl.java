package com.github.dfauth.authzn;

public class CompanyImpl implements Company {

    private final String companyId;

    public CompanyImpl(String companyId) {
        this.companyId = companyId;
    }

    @Override
    public String companyId() {
        return companyId;
    }
}
