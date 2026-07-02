package nl.alexeyu.structmatcher.property;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PropertyPathTest {

    @Test
    public void isEmptyByDefault() {
        assertTrue(new PropertyPath().isEmpty());
    }

    @Test
    public void cannotGetHeadOfEmptyPath() {
        assertThrows(IllegalStateException.class, () -> new PropertyPath().head());
    }

    @Test
    public void cannotGetTailOfEmptyPath() {
        assertThrows(IllegalStateException.class, () -> new PropertyPath().tail());
    }

    @Test
    public void fedFromList() {
        var path = new PropertyPath(asList("a"));
        assertFalse(path.isEmpty());
        assertEquals("a", path.head());
        assertTrue(path.tail().isEmpty());
    }

    @Test
    public void pushAddsElementToTail() {
        var path = new PropertyPath();
        path.push("a");
        assertFalse(path.isEmpty());
        assertEquals("a", path.head());
        assertTrue(path.tail().isEmpty());
    }

    @Test
    public void popSafelyRemovesElementFromTail() {
        var path = new PropertyPath();
        path.push("a");
        assertFalse(path.isEmpty());
        path.pop();
        assertTrue(path.isEmpty());
        path.pop();
        assertTrue(path.isEmpty());
    }

    @Test
    public void headAlwaysPointsToFirstElement() {
        var path = new PropertyPath();
        path.push("a");
        assertEquals("a", path.head());
        path.push("b");
        assertEquals("a", path.head());
    }

    @Test
    public void tailAlwaysReturnsAllElementsButFirst() {
        var path = new PropertyPath(asList("a", "b", "c"));
        assertEquals("b", path.tail().head());
        assertEquals("c", path.tail().tail().head());
    }

    @Test
    public void equalsHashCodeContractMetForEqualObjects() {
        var path1 = new PropertyPath(asList("a", "b"));
        var path2 = new PropertyPath(asList("a"));
        path2.push("b");
        assertEquals(path1, path2);
        assertTrue(path1.hashCode() == path2.hashCode());
    }

    @Test
    public void equalsHashCodeContractMetForNonEqualObjects() {
        var path1 = new PropertyPath(asList("a", "b"));
        var path2 = new PropertyPath(asList("a", "b", "c"));
        assertNotEquals(path1, path2);
    }

}
