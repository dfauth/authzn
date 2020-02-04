package com.github.dfauth.authzn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.*;
import java.util.stream.Collectors;

import static com.github.dfauth.authzn.Assertions.assertOptional;
import static com.github.dfauth.authzn.PrincipalType.ROLE;
import static org.testng.Assert.*;
import static org.testng.AssertJUnit.assertFalse;

public class ResourceNodeTest {

    private static final Logger logger = LoggerFactory.getLogger(ResourceNodeTest.class);

    @Test
    public void testBasic() {
        ResourceNode<Integer> ROOT = new RootResourceNode();
        ROOT.add(
                asResourceInt("/a", 1),
                asResourceInt("/b", 2)
        );

        {
            final String path = "/a";
            Optional<ResourceNode<Integer>> r = ROOT.find(path);
            assertTrue(r.isPresent());
        }
        {
            final String path = "/b";
            Optional<ResourceNode<Integer>> r = ROOT.find(path);
            assertTrue(r.isPresent());
        }
        {
            final String path = "/b/1";
            ROOT.add((asResourceInt(path, 0)));
            Optional<ResourceNode<Integer>> r = ROOT.find("/b");
            assertOptional(r).withPredicate(e -> e.find("1").isPresent()).doAssert();
            assertTrue(r.isPresent());
            Collection<Integer> c = ROOT.findAllInPath(path);
            assertFalse(c.isEmpty());
            assertEquals(c, Arrays.asList(new Integer[]{0, 2}));
        }
    }

    //@Test
    public void testIt() {
        ResourceNode<Directive> ROOT = new RootResourceNode();
        ROOT.add(
                asResource("/"),
                asResource("/a"),
                asResource("/a/ab"),
                asResource("/a/ab/abc"),
                asResource("/a/ab/abc/resource0"),
                asResource("/a/ab/abc/resource1"),
                asResource("/a/ab/abc/resource2"),
                asResource("/a/ab/abc/resource3"),
                asResource("/a/ab/abd/resource4"),
                asResource("/a/ab/abd/resource5"),
                asResource("/a/ac/abc/resource6"),
                asResource("/a/b/abe/resource7")
        );

        {
            final String path = "/a/ab/abc/resource0";
            Optional<ResourceNode<Directive>> r = ROOT.find(path);
            assertTrue(r.isPresent());
//        r.map(e -> assertEquals(e.resource().iterator().next().getResourcePath().toString(), path)).orElse(fail("Oops"));
            assertEquals(r.get().resource().iterator().next().getResourcePath().toString(), path);
            Optional<ResourceNode<Directive>> h = ROOT.findNearest(path);
            assertNotNull(h);
//        assertEquals(h.getPath(), path);
        }
        {
            final String path = "/a/b/abc/resourceZ";
            Optional<ResourceNode<Directive>> r = ROOT.find(path);
            assertFalse(r.isPresent());
            Optional<ResourceNode<Directive>> h = ROOT.findNearest(path);
            assertTrue(h.isPresent());
//        assertEquals(h.resource().get().getPath(), path);
        }

        {
            final String path = "/a/ac/abc/resource6";
            Iterable<Directive> iterable = ROOT.findAllInPath(path);
            assertNotNull(iterable);
            Iterator<Directive> it = iterable.iterator();
            assertTrue(it.hasNext());
            Directive next = it.next();
            assertNotNull(next);
            assertEquals(next.getResourcePath().toString(), path);
            assertTrue(it.hasNext());
            next = it.next();
            assertNotNull(next);
            assertEquals(next.getResourcePath().toString(), "/a");
            assertFalse(it.hasNext());
        }

        Collection<Directive> iterable;
        Iterator<Directive> it;
        {
            final String path = "/a/ab/abc/resource0";
            iterable = ROOT.findAllInPath(path);
            assertNotNull(iterable);
            it = iterable.iterator();
            assertTrue(it.hasNext());
            Directive next = it.next();
            assertNotNull(next);
            assertEquals(next.getResourcePath().toString(), path);

            assertTrue(it.hasNext());
            next = it.next();
            assertNotNull(next);
            assertEquals(next.getResourcePath().toString(), "/a/ab/abc");
            assertTrue(it.hasNext());

            assertTrue(it.hasNext());
            next = it.next();
            assertNotNull(next);
            assertEquals(next.getResourcePath().toString(), "/a/ab");
            assertTrue(it.hasNext());

            assertTrue(it.hasNext());
            next = it.next();
            assertNotNull(next);
            assertEquals(next.getResourcePath().toString(), "/a");
            assertFalse(it.hasNext());
        }

        {
            final String path = "/c/ab/abc/resource0";
            iterable = ROOT.findAllInPath(path);
            assertNotNull(iterable);
            it = iterable.iterator();
            assertFalse(it.hasNext());
        }
    }

    private Resource<Integer> asResourceInt(String resource, int i) {
        return new Resource(new ResourcePath(resource), i);
    }

    private DirectiveResource asResource(String resource) {
        Directive directive = new Directive(ROLE.of("user"), new ResourcePath(resource));
        return new DirectiveResource(directive);
    }
}
