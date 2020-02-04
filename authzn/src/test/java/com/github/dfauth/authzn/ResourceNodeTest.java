package com.github.dfauth.authzn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

import static com.github.dfauth.authzn.PrincipalType.ROLE;
import static org.testng.Assert.*;
import static org.testng.AssertJUnit.assertFalse;

public class ResourceNodeTest {

    private static final Logger logger = LoggerFactory.getLogger(ResourceNodeTest.class);

    @Test
    public void testBasic() {
        ResourceNode<Directive> ROOT = new RootResourceNode();
        ROOT.add(
                asResource("/a")
        );

        {
            final String path = "/a";
            Optional<ResourceNode<Directive>> r = ROOT.find(path);
            assertTrue(r.isPresent());
        }
    }

    @Test
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

    private DirectiveResource asResource(String resource) {
        Directive directive = new Directive(ROLE.of("user"), new ResourcePath(resource));
        return new DirectiveResource(directive);
    }

}
