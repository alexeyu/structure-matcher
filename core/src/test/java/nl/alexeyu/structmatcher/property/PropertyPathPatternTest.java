package nl.alexeyu.structmatcher.property;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

public class PropertyPathPatternTest {

    @Test
    public void isEmptyByDefault() {
        assertTrue(new PropertyPathPattern().isEmpty());
    }

    @Test
    public void cannotGetHeadOfEmptyPath() {
        assertThrows(IllegalStateException.class, () -> new PropertyPathPattern().head());
    }

    @Test
    public void cannotGetTailOfEmptyPath() {
        assertThrows(IllegalStateException.class, () -> new PropertyPathPattern().tail());
    }

    @Test
    public void fedFromList() {
        var path = new PropertyPathPattern("a");
        assertFalse(path.isEmpty());
        assertEquals("a", path.head());
        assertTrue(path.tail().isEmpty());
    }

    @Test
    public void headAlwaysPointsToFirstElement() {
        var path = new PropertyPathPattern("a", "b");
        assertEquals("a", path.head());
    }

    @Test
    public void tailAlwaysReturnsAllElementsButFirst() {
        var path = new PropertyPathPattern("a", "b", "c");
        assertEquals("b", path.tail().head());
        assertEquals("c", path.tail().tail().head());
    }

    @Test
    public void equalsHashCodeContractMetForEqualObjects() {
        var path1 = new PropertyPathPattern("a", "b");
        var path2 = new PropertyPathPattern("a", "b");
        assertEquals(path1, path2);
        assertTrue(path1.hashCode() == path2.hashCode());
    }

    @Test
    public void equalsHashCodeContractMetForNonEqualObjects() {
        var path1 = new PropertyPathPattern("a", "b");
        var path2 = new PropertyPathPattern("a", "b", "c");
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
        assertTrue(
                new PropertyPathPattern("a", "x").headsMatch(new PropertyPath(asList("a", "b"))));
        assertFalse(new PropertyPathPattern("*").headsMatch(new PropertyPath(asList("a"))));
        assertFalse(
                new PropertyPathPattern("a", "x").headsMatch(new PropertyPath(asList("b", "z"))));
    }

    @Test
    public void mostSpecificFirstRanksNamesAboveWildcards() {
        var patterns = new ArrayList<>(asList(new PropertyPathPattern("*", "Url"),
                new PropertyPathPattern("Root", "*", "Url"),
                new PropertyPathPattern("Root", "A", "*"),
                new PropertyPathPattern("Root", "A", "Url")));
        patterns.sort(PropertyPathPattern.MOST_SPECIFIC_FIRST);
        assertEquals(asList("[Root, A, Url]", "[Root, A, *]", "[Root, *, Url]", "[*, Url]"),
                patterns.stream().map(Object::toString).toList());
    }

    /** Patterns of identical shape are rare. The comparator still ranks them, either way round. */
    @Test
    public void patternsOfTheSameShapeStillRankApart() {
        var bThenC = new PropertyPathPattern("A", "*", "B", "*", "C");
        var cThenB = new PropertyPathPattern("A", "*", "C", "*", "B");
        assertTrue(PropertyPathPattern.MOST_SPECIFIC_FIRST.compare(bThenC, cThenB) < 0);
        assertTrue(PropertyPathPattern.MOST_SPECIFIC_FIRST.compare(cThenB, bThenC) > 0);
    }

}
