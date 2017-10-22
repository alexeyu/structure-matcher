package nl.alexeyu.structmatcher.property;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PropertyPathTest {
    
    @Test
    public void isEmptyByDefault() {
        assertTrue(new PropertyPath().isEmpty());
    }

    @Test(expected = IllegalStateException.class )
    public void cannotGetHeadOfEmptyPath() {
        new PropertyPath().head();
    }

    @Test(expected = IllegalStateException.class )
    public void cannotGetTailOfEmptyPath() {
        new PropertyPath().tail();
    }

    @Test
    public void fedFromList() {
        PropertyPath path = new PropertyPath(asList("a"));
        assertFalse(path.isEmpty());
        assertEquals("a", path.head());
        assertTrue(path.tail().isEmpty());
    }

    @Test
    public void pushAddsElementToTail() {
        PropertyPath path = new PropertyPath();
        path.push("a");
        assertFalse(path.isEmpty());
        assertEquals("a", path.head());
        assertTrue(path.tail().isEmpty());
    }

    @Test
    public void popSafelyRemovesElementFromTail() {
        PropertyPath path = new PropertyPath();
        path.push("a");
        assertFalse(path.isEmpty());
        path.pop();
        assertTrue(path.isEmpty());
        path.pop();
        assertTrue(path.isEmpty());
    }

    @Test
    public void headAlwaysPointsToFirstElement() {
        PropertyPath path = new PropertyPath();
        path.push("a");
        assertEquals("a", path.head());
        path.push("b");
        assertEquals("a", path.head());
    }

    @Test
    public void tailAlwaysReturnsAllElementsButFirst() {
        PropertyPath path = new PropertyPath(asList("a", "b", "c"));
        assertEquals("b", path.tail().head());
        assertEquals("c", path.tail().tail().head());
    }

    @Test
    public void equalsHashCodeContractMetForEqualObjects() {
        PropertyPath path1 = new PropertyPath(asList("a", "b"));
        PropertyPath path2 = new PropertyPath(asList("a"));
        path2.push("b");
        assertEquals(path1, path2);
        assertTrue(path1.hashCode() == path2.hashCode());
    }

    @Test
    public void equalsHashCodeContractMetForNonEqualObjects() {
        PropertyPath path1 = new PropertyPath(asList("a", "b"));
        PropertyPath path2 = new PropertyPath(asList("a", "b", "c"));
        assertNotEquals(path1, path2);
    }

}
