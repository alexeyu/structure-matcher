package nl.alexeyu.structmatcher.matcher;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import nl.alexeyu.structmatcher.feedback.Feedback;

public class ArrayMatcherTest {

    private final ArrayMatcher matcher = Matchers.arraysEqual();

    @Test
    public void emptyArraysMatch() {
        assertTrue(matcher.match("empty", new String[] {}, new String[] {}).isEmpty());
    }

    @Test
    public void equalObjectArraysMatch() {
        assertTrue(matcher.match("arr", new String[] { "a", "b" }, new String[] { "a", "b" }).isEmpty());
    }

    @Test
    public void differentObjectArraysReportTheDifferingElement() {
        var feedback = matcher.match("arr", new String[] { "a", "b" }, new String[] { "a", "c" });
        assertEquals(Feedback.composite("arr", asList(Feedback.nonEqual("arr[1]", "b", "c"))), feedback);
    }

    @Test
    public void arraysOfDifferentLengthDoNotMatch() {
        var feedback = matcher.match("arr", new String[] { "a" }, new String[] { "a", "b" });
        assertEquals(Feedback.differentCollectionSizes("arr", 1, 2), feedback);
    }

    @Test
    public void primitiveIntArraysMatch() {
        assertTrue(matcher.match("arr", new int[] { 1, 2, 3 }, new int[] { 1, 2, 3 }).isEmpty());
    }

    @Test
    public void differentPrimitiveIntArraysReportTheDifferingElement() {
        var feedback = matcher.match("arr", new int[] { 1, 2, 3 }, new int[] { 1, 2, 4 });
        assertEquals(Feedback.composite("arr", asList(Feedback.nonEqual("arr[2]", 3, 4))), feedback);
    }

    @Test
    public void arraysOfComplexElementsAreMatchedStructurally() {
        Substructure[] expected = { new Substructure(true) };
        Substructure[] actual = { new Substructure(false) };
        var feedback = matcher.match("arr", expected, actual);
        var expectedFeedback = Feedback.composite("arr",
                asList(Feedback.composite("arr[0]", asList(Feedback.nonEqual("Bool", true, false)))));
        assertEquals(expectedFeedback, feedback);
    }

    @Test
    public void arrayPropertyIsMatchedEndToEndThroughObjectMatcher() {
        var expected = new ArrayHolder(new String[] { "a", "b" });
        var actual = new ArrayHolder(new String[] { "a", "c" });
        var feedback = ObjectMatcher.forClass(ArrayHolder.class).match(expected, actual);
        var expectedFeedback = Feedback.composite(ArrayHolder.class.getName(), asList(
                Feedback.composite("Tags", asList(
                        Feedback.nonEqual("Tags[1]", "b", "c")))));
        assertEquals(expectedFeedback, feedback);
    }
}
