package com.github.dfauth.authzn;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourcePath {

    private final Iterable<String> path;

    public static Iterable<String> parse(String path) {
        return Stream.of(path.split("/")).filter(e -> !"".equals(e)).collect(Collectors.toSet());
    }

    public ResourcePath(String path) {
        this(parse(path));
    }

    public ResourcePath(Iterable<String> path) {
        this.path = path;
    }

    public static ResourcePath root() {
        return new ResourcePath(Collections.emptySet());
    }

    public Iterable<String> getPath() {
        return path;
    }

    public boolean isRoot() {
        return !path.iterator().hasNext();
    }

    @Override
    public String toString() {
        StringWriter tmp = new StringWriter();
        path.iterator().forEachRemaining(e -> {
            tmp.append("/"+e);
        });
        return tmp.toString();
    }
}
