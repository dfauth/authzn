package com.github.dfauth.scrub.rfq;

import com.github.dfauth.authzn.Company;
import com.github.dfauth.scrub.VisibilityModel;
import com.github.dfauth.scrub.node.Node;
import com.github.dfauth.scrub.node.NodeImpl;
import com.github.dfauth.scrub.node.ScrubbedNode;

import java.util.Optional;
import java.util.stream.Stream;

public abstract class RfqVisibilityModel<T> implements VisibilityModel<Company, T> {

    protected final ScrubbedNode<Company> originator;
    protected final ScrubbedNode<Company> broker;
    protected final ScrubbedNode<Company> tradingCompany;

    public RfqVisibilityModel(Company originatorId, Company brokerId, Company tradingCompanyId) {
        NodeImpl.Builder<Company> leaf = NodeImpl.leafBuilder(tradingCompanyId);
        NodeImpl.Builder<Company> intermediate = NodeImpl.intermediateBuilder(brokerId, leaf);
        this.originator = new NodeImpl<>(Optional.empty(), originatorId, Optional.of(intermediate));
        this.broker = intermediate.node();
        this.tradingCompany = leaf.node();
    }

    public Node<Company> getOriginator() {
        return this.originator;
    }

    public Node<Company> getBroker() {
        return this.broker;
    }

    public Node<Company> getTradingCompany() {
        return this.tradingCompany;
    }

    @Override
    public boolean isVisibleTo(Company c) {
        return nodes().reduce(false,
                (acc, n) -> acc || n.isVisibleTo(c),
                (b1, b2) -> b1 || b2
        );
    }

    private Stream<ScrubbedNode<Company>> nodes() {
        return Stream.of(new ScrubbedNode[]{this.originator, this.broker, this.tradingCompany});
    }

    protected <T2> T2 _render(T2 t, Company c) {
        return isVisibleTo(c) ? t : null;
    }

    protected String extract(ScrubbedNode<Company> node, Company company) {
        return node.getValueFor(company).map(c -> c.companyId()).orElseGet(() -> null);
    }
}
