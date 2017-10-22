package nl.alexeyu.structmatcher.property;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PropertyPathPatternTest {
    
    @Test
    public void isEmptyByDefault() {
        assertTrue(new PropertyPathPattern().isEmpty());
    }

    @Test(expected = IllegalStateException.class )
    public void cannotGetHeadOfEmptyPath() {
        new PropertyPathPattern().head();
    }

    @Test(expected = IllegalStateException.class )
    public void cannotGetTailOfEmptyPath() {
        new PropertyPathPattern().tail();
    }

    @Test
    public void fedFromList() {
        PropertyPathPattern path = new PropertyPathPattern("a");
        assertFalse(path.isEmpty());
        assertEquals("a", path.head());
        assertTrue(path.tail().isEmpty());
    }

    @Test
    public void headAlwaysPointsToFirstElement() {
        PropertyPathPattern path = new PropertyPathPattern("a", "b");
        assertEquals("a", path.head());
    }

    @Test
    public void tailAlwaysReturnsAllElementsButFirst() {
        PropertyPathPattern path = new PropertyPathPattern("a", "b", "c");
        assertEquals("b", path.tail().head());
        assertEquals("c", path.tail().tail().head());
    }

    @Test
    public void equalsHashCodeContractMetForEqualObjects() {
        PropertyPathPattern path1 = new PropertyPathPattern("a", "b");
        PropertyPathPattern path2 = new PropertyPathPattern("a", "b");
        assertEquals(path1, path2);
        assertTrue(path1.hashCode() == path2.hashCode());
    }

    @Test
    public void equalsHashCodeContractMetForNonEqualObjects() {
        PropertyPathPattern path1 = new PropertyPathPattern("a", "b");
        PropertyPathPattern path2 = new PropertyPathPattern("a", "b", "c");
        assertNotEquals(path1, path2);
    }

    @Test
    public void positivePatterns() {
        assertTrue(new PropertyPathPattern().isPositive());
        assertTrue(new PropertyPathPattern("*").isPositive());
        assertTrue(new PropertyPathPattern("*", "*").isPositive());
    }

    @Test
    public void nonPositivePatterns() {
        assertFalse(new PropertyPathPattern("*", "a").isPositive());
        assertFalse(new PropertyPathPattern("a", "*").isPositive());
    }

    @Test
    public void startsWithWildcard() {
        assertFalse(new PropertyPathPattern().startsWithWildcard());
        assertTrue(new PropertyPathPattern("*", "a").startsWithWildcard());
        assertFalse(new PropertyPathPattern("a", "*").startsWithWildcard());
    }

    @Test
    public void headsMatch() {
        assertTrue(new PropertyPathPattern("a", "x").headsMatch(new PropertyPath(asList("a", "b"))));
        assertFalse(new PropertyPathPattern("*").headsMatch(new PropertyPath(asList("a"))));
        assertFalse(new PropertyPathPattern("a", "x").headsMatch(new PropertyPath(asList("b", "z"))));
    }

}
