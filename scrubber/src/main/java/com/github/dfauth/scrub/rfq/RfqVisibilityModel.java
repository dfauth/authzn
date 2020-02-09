package com.github.dfauth.scrub.rfq;

import com.github.dfauth.scrub.Company;
import com.github.dfauth.scrub.UserModel;
import com.github.dfauth.scrub.VisibilityModel;
import com.github.dfauth.scrub.node.Node;
import com.github.dfauth.scrub.node.NodeImpl;
import com.github.dfauth.scrub.node.ScrubbedNode;

import java.util.Optional;
import java.util.stream.Stream;

public abstract class RfqVisibilityModel<T> implements VisibilityModel<UserModel, T> {

    protected final ScrubbedNode<UserModel, Company> originator;
    protected final ScrubbedNode<UserModel, Company> broker;
    protected final ScrubbedNode<UserModel, Company> tradingCompany;

    public RfqVisibilityModel(Company originatorId, Company brokerId, Company tradingCompanyId) {
        NodeImpl.Builder<UserModel, Company> leaf = NodeImpl.leafBuilder(tradingCompanyId);
        NodeImpl.Builder<UserModel, Company> intermediate = NodeImpl.intermediateBuilder(brokerId, leaf);
        this.originator = new NodeImpl<>(Optional.empty(), originatorId, Optional.of(intermediate));
        this.broker = intermediate.node();
        this.tradingCompany = leaf.node();
    }

    public Node<UserModel, Company> getOriginator() {
        return this.originator;
    }

    public Node<UserModel, Company> getBroker() {
        return this.broker;
    }

    public Node<UserModel, Company> getTradingCompany() {
        return this.tradingCompany;
    }

    @Override
    public boolean isVisibleTo(UserModel u) {
        return nodes().reduce(false,
                (acc, n) -> acc || n.isVisibleTo(u),
                (b1, b2) -> b1 || b2
        );
    }

    private Stream<ScrubbedNode<UserModel, Company>> nodes() {
        return Stream.of(new ScrubbedNode[]{this.originator, this.broker, this.tradingCompany});
    }

    protected <T2> T2 _render(T2 t, UserModel u) {
        return isVisibleTo(u) ? t : null;
    }

    protected String extract(ScrubbedNode<UserModel, Company> node, UserModel u) {
        return node.getValueFor(u).map(c -> c.companyId()).orElseGet(() -> null);
    }
}
