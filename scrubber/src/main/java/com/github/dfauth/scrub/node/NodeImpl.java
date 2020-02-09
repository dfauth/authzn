package com.github.dfauth.scrub.node;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class NodeImpl<U extends Supplier<T>,T> implements ScrubbedNode<U,T> {

    private final T t;
    private final Optional<Node<U,T>> parent;
    private final Optional<Node<U,T>> child;

    public NodeImpl(Optional<Node<U,T>> parent, T t, Optional<Builder<U,T>> builder) {
        this.parent = parent;
        this.t = t;
        this.child = builder.map(b -> b.build(Optional.of(this)));
    }

    @Override
    public T payload() {
        return t;
    }

    @Override
    public Optional<Node<U,T>> parent() {
        return parent;
    }

    @Override
    public Optional<Node<U,T>> child() {
        return child;
    }

    @Override
    public boolean isVisibleTo(U u) {
        return this.nodes().reduce(this.payload().equals(u.get()),
                (acc, n) -> acc || n.payload().equals(u.get()),
                (b1, b2) -> b1 || b2
                );
    }

    private Stream<Node<U,T>> nodes() {
        return Stream.of(new Optional[]{this.parent, this.child}).filter(o -> o.isPresent()).map(o -> (Node<U,T>)o.get());
    }

    public interface Builder<U extends Supplier<T>,T> {
        ScrubbedNode<U,T> build(Optional<Node<U,T>> parent);

        ScrubbedNode<U,T> node();
    }

    public static <U extends Supplier<T>,T> Builder<U,T> leafBuilder(T payload) {
        return new LeafBuilder(payload);
    }

    public static <U extends Supplier<T>,T> Builder<U,T> intermediateBuilder(T payload, Builder<U,T> child) {
        return new IntermediateBuilder(payload, child);
    }

    static class LeafBuilder<U extends Supplier<T>,T> implements Builder<U,T> {

        private T payload;
        private ScrubbedNode<U,T> node;

        LeafBuilder(T payload) {
            this.payload = payload;
        }

        @Override
        public ScrubbedNode<U,T> build(Optional<Node<U,T>> parent) {
            if(node == null) {
                node = new NodeImpl(parent, payload, Optional.empty());
            }
            return node;
        }

        @Override
        public ScrubbedNode<U,T> node() {
            return node;
        }
    }

    static class IntermediateBuilder<U extends Supplier<T>,T> implements Builder<U,T> {

        private T payload;
        private final Builder<U,T> child;
        private ScrubbedNode<U,T> node;

        public IntermediateBuilder(T payload, Builder<U,T> child) {
            this.payload = payload;
            this.child = child;
        }

        @Override
        public ScrubbedNode<U,T> build(Optional<Node<U,T>> parent) {
            if(node == null) {
                node = new NodeImpl<U,T>(parent, payload, Optional.of(this.child));
            }
            return node;
        }

        @Override
        public ScrubbedNode<U,T> node() {
            return node;
        }
    }
}
