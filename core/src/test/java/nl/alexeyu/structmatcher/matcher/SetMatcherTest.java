package nl.alexeyu.structmatcher.matcher;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import nl.alexeyu.structmatcher.feedback.Feedback;

public class SetMatcherTest {

    private final SetMatcher<Object> matcher = Matchers.setsEqual();

    @Test
    public void emptySetsMatch() {
        assertTrue(matcher.match("empty", set(), set()).isEmpty());
    }

    @Test
    public void setsWithEqualElementsMatch() {
        assertTrue(matcher.match("set", set(1, 2, 3), set(1, 2, 3)).isEmpty());
    }

    @Test
    public void orderDoesNotMatter() {
        assertTrue(matcher.match("set", set(1, 2, 3), set(3, 2, 1)).isEmpty());
    }

    @Test
    public void elementMissingFromTheActualSetIsReported() {
        var feedback = matcher.match("set", set(1, 2), set(1));
        assertFalse(feedback.isEmpty());
        assertEquals(Feedback.composite("set", asList(Feedback.gotNull("set[2]", 2))), feedback);
    }

    @Test
    public void elementExtraInTheActualSetIsReported() {
        var feedback = matcher.match("set", set(1), set(1, 2));
        assertFalse(feedback.isEmpty());
        assertEquals(Feedback.composite("set", asList(Feedback.gotNonNull("set[2]", 2))), feedback);
    }

    @Test
    public void differingSetsReportBothMissingAndExtraElements() {
        var feedback = matcher.match("set", set(1, 2), set(1, 3));
        assertEquals(
                Feedback.composite("set",
                        asList(Feedback.gotNull("set[2]", 2), Feedback.gotNonNull("set[3]", 3))),
                feedback);
    }

    /**
     * A set iterates in an order of its own, and a composite node compares its children position
     * by position. Fill the same elements in reverse and the feedback has to come out equal, or
     * two equivalent comparisons disagree and their archives stop diffing.
     */
    @Test
    public void feedbackDoesNotDependOnTheIterationOrderOfTheSets() {
        var feedback = matcher.match("set", set(1, 2, 3), set(3, 4));
        var reversed = matcher.match("set", set(3, 2, 1), set(4, 3));
        assertEquals(feedback, reversed);
    }

    @Test
    public void elementsAreReportedInNameOrder() {
        var feedback = matcher.match("set", set(2, 1), set(2, 3));
        assertEquals(Feedback.composite("set",
                asList(Feedback.gotNull("set[1]", 1), Feedback.gotNonNull("set[3]", 3))), feedback);
    }

    @Test
    public void elementsAreComparedByValueSoRecordElementsMatch() {
        var expected = set(new RecordSubstructure(true), new RecordSubstructure(false));
        var actual = set(new RecordSubstructure(false), new RecordSubstructure(true));
        assertTrue(matcher.match("set", expected, actual).isEmpty());
    }

    @Test
    public void setPropertyIsMatchedEndToEndThroughObjectMatcher() {
        var expected = new SetHolder(Set.of("a", "b"));
        var actual = new SetHolder(Set.of("a", "c"));
        var feedback = ObjectMatcher.forClass(SetHolder.class).match(expected, actual);
        var expectedFeedback = Feedback.composite(SetHolder.class.getName(),
                asList(Feedback.composite("Tags", asList(Feedback.gotNull("Tags[b]", "b"),
                        Feedback.gotNonNull("Tags[c]", "c")))));
        assertEquals(expectedFeedback, feedback);
    }

    private static Set<Object> set(Object... elements) {
        return new LinkedHashSet<>(asList(elements));
    }
}
