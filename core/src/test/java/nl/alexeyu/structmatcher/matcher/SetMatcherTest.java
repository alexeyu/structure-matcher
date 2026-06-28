package nl.alexeyu.structmatcher.matcher;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

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
        FeedbackNode feedback = matcher.match("set", set(1, 2), set(1));
        assertFalse(feedback.isEmpty());
        assertEquals(Feedback.composite("set", asList(Feedback.gotNull("set[2]", 2))), feedback);
    }

    @Test
    public void elementExtraInTheActualSetIsReported() {
        FeedbackNode feedback = matcher.match("set", set(1), set(1, 2));
        assertFalse(feedback.isEmpty());
        assertEquals(Feedback.composite("set", asList(Feedback.gotNonNull("set[2]", 2))), feedback);
    }

    @Test
    public void differingSetsReportBothMissingAndExtraElements() {
        FeedbackNode feedback = matcher.match("set", set(1, 2), set(1, 3));
        assertEquals(Feedback.composite("set",
                asList(Feedback.gotNull("set[2]", 2), Feedback.gotNonNull("set[3]", 3))), feedback);
    }

    @Test
    public void elementsAreComparedByValueSoRecordElementsMatch() {
        Set<Object> expected = set(new RecordSubstructure(true), new RecordSubstructure(false));
        Set<Object> actual = set(new RecordSubstructure(false), new RecordSubstructure(true));
        assertTrue(matcher.match("set", expected, actual).isEmpty());
    }

    @Test
    public void setPropertyIsMatchedEndToEndThroughObjectMatcher() {
        SetHolder expected = new SetHolder(Set.of("a", "b"));
        SetHolder actual = new SetHolder(Set.of("a", "c"));
        FeedbackNode feedback = ObjectMatcher.forClass(SetHolder.class).match(expected, actual);
        FeedbackNode expectedFeedback = Feedback.composite(SetHolder.class.getName(), asList(
                Feedback.composite("Tags", asList(
                        Feedback.gotNull("Tags[b]", "b"),
                        Feedback.gotNonNull("Tags[c]", "c")))));
        assertEquals(expectedFeedback, feedback);
    }

    private static Set<Object> set(Object... elements) {
        return new LinkedHashSet<>(asList(elements));
    }
}
