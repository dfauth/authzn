package com.github.dfauth.scrub.node;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class NodeImpl<T> implements ScrubbedNode<T> {

    private final T t;
    private final Optional<Node<T>> parent;
    private final Optional<Node<T>> child;

    public NodeImpl(Optional<Node<T>> parent, T t, Optional<Builder<T>> builder) {
        this.parent = parent;
        this.t = t;
        this.child = builder.map(b -> b.build(Optional.of(this)));
    }

    @Override
    public T payload() {
        return t;
    }

    @Override
    public Optional<Node<T>> parent() {
        return parent;
    }

    @Override
    public Optional<Node<T>> child() {
        return child;
    }

    @Override
    public boolean isVisibleTo(T t) {
        return this.nodes().reduce(this.payload().equals(t),
                (acc, n) -> acc || n.payload().equals(t),
                (b1, b2) -> b1 || b2
                );
    }

    private Stream<Node<T>> nodes() {
        return Stream.of(new Optional[]{this.parent, this.child}).filter(o -> o.isPresent()).map(o -> (Node<T>)o.get());
    }

    public interface Builder<T> {
        ScrubbedNode<T> build(Optional<Node<T>> parent);

        ScrubbedNode<T> node();
    }

    public static <U extends Supplier<T>,T> Builder<T> leafBuilder(T payload) {
        return new LeafBuilder(payload);
    }

    public static <U extends Supplier<T>,T> Builder<T> intermediateBuilder(T payload, Builder<T> child) {
        return new IntermediateBuilder(payload, child);
    }

    static class LeafBuilder<U extends Supplier<T>,T> implements Builder<T> {

        private T payload;
        private ScrubbedNode<T> node;

        LeafBuilder(T payload) {
            this.payload = payload;
        }

        @Override
        public ScrubbedNode<T> build(Optional<Node<T>> parent) {
            if(node == null) {
                node = new NodeImpl(parent, payload, Optional.empty());
            }
            return node;
        }

        @Override
        public ScrubbedNode<T> node() {
            return node;
        }
    }

    static class IntermediateBuilder<U extends Supplier<T>,T> implements Builder<T> {

        private T payload;
        private final Builder<T> child;
        private ScrubbedNode<T> node;

        public IntermediateBuilder(T payload, Builder<T> child) {
            this.payload = payload;
            this.child = child;
        }

        @Override
        public ScrubbedNode<T> build(Optional<Node<T>> parent) {
            if(node == null) {
                node = new NodeImpl<T>(parent, payload, Optional.of(this.child));
            }
            return node;
        }

        @Override
        public ScrubbedNode<T> node() {
            return node;
        }
    }
}
