package com.github.dfauth.authzn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class ResourceNode<V> {

    private static final Logger logger = LoggerFactory.getLogger(ResourceNode.class);

    protected Set<Resource<V>> resource = new HashSet<>();
    protected Map<String, ResourceNode<V>> children = new HashMap<>();
    protected BiConsumer<Iterator<String>, ResourceNode<V>> addingConsumer(Resource resource) {
        return  (i,n) -> {
            BiConsumer<Iterator<String>, ResourceNode<V>> c = addingConsumer(resource);
            if(i.hasNext()) {
                String k = i.next();
                ResourceNode node = new ResourceNodeImpl(k);
                n.children.put(k, node);
                node.findNearest(i, c);
            } else {
                this.resource.add(resource);
            }
        };
    }

    public ResourceNode<V> add(Resource<V> resource) {
        findNearest(resource.getResourcePath().getPath().iterator(), addingConsumer(resource));
        return this;
    }

    public ResourceNode<V> add(Resource<V>... resources) {
        Arrays.stream(resources).forEach(r -> add(r));
        return this;
    }

    public Optional<ResourceNode<V>> find(String path) {
        return find(new ResourcePath(path));
    }

    public Optional<ResourceNode<V>> find(ResourcePath path) {
        return find(path, (i,n) -> {});
    }

    public Optional<ResourceNode<V>> find(ResourcePath path, BiConsumer<Iterator<String>, ResourceNode<V>> consumer) {
        return find(path.getPath().iterator(), consumer);
    }

    public Optional<ResourceNode<V>> find(Iterator<String> it) {
        return find(it, (i,n) -> {});
    }

    public Optional<ResourceNode<V>> find(Iterator<String> it, BiConsumer<Iterator<String>, ResourceNode<V>> consumer) {
        return find(it, consumer, n -> Optional.empty());
    }

    public Optional<ResourceNode<V>> findNearest(String path) {
        return find(new ResourcePath(path));
    }

    public Optional<ResourceNode<V>> findNearest(Iterator<String> it) {
        return findNearest(it, (i,n) -> {});
    }

    public Optional<ResourceNode<V>> findNearest(Iterator<String> it, BiConsumer<Iterator<String>, ResourceNode<V>> consumer) {
        return find(it, consumer, n -> Optional.of(n));
    }

    private Optional<ResourceNode<V>> find(Iterator<String> it, BiConsumer<Iterator<String>, ResourceNode<V>> consumer, Function<ResourceNode<V>, Optional<ResourceNode<V>>> f) {
        if(!it.hasNext()) {
            consumer.accept(it, this);
            return Optional.of(this);
        } else {
            String key = it.next();
            return Optional.ofNullable(children.get(key)).map(e -> {
                consumer.accept(it,this);
                return e.find(it);
            }).orElseGet(() -> {
                    Optional<ResourceNode<V>> result = f.apply(this);
                    result.ifPresent(e -> consumer.accept(it, e));
                    return result;
            });
        }
    }

    public Collection<Resource<V>> resource() {
        return resource;
    }

    public Collection<V> findAllInPath(String path) {
        return findAllInPath(new ResourcePath(path).getPath());
    }

    public Collection<V> findAllInPath(Iterable<String> path) {
        Deque<V> stack = new ArrayDeque();
        findNearest(path.iterator(), (i, n) -> n.resource.forEach(p -> stack.push(p.payload)));
        return stack;
    }

    public void walk(Consumer<Resource<V>> consumer) {
        children.values().forEach(c -> c.walk(consumer));
        resource.forEach(consumer);
    }
}
