package nl.alexeyu.structmatcher.matcher;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

public class OptionalMatcherTest {

    private final OptionalMatcher matcher = Matchers.optional();

    @Test
    public void twoEmptyOptionalsMatch() {
        assertTrue(matcher.match("opt", Optional.empty(), Optional.empty()).isEmpty());
    }

    @Test
    public void twoPresentEqualValuesMatch() {
        assertTrue(matcher.match("opt", Optional.of("a"), Optional.of("a")).isEmpty());
    }

    @Test
    public void twoPresentDifferentValuesDoNotMatch() {
        FeedbackNode feedback = matcher.match("opt", Optional.of("a"), Optional.of("b"));
        assertEquals(Feedback.nonEqual("opt", "a", "b"), feedback);
    }

    @Test
    public void presentVersusEmptyIsReportedAsAMissingValue() {
        FeedbackNode feedback = matcher.match("opt", Optional.of("a"), Optional.empty());
        assertEquals(Feedback.gotNull("opt", "a"), feedback);
    }

    @Test
    public void emptyVersusPresentIsReportedAsAnUnexpectedValue() {
        FeedbackNode feedback = matcher.match("opt", Optional.empty(), Optional.of("b"));
        assertEquals(Feedback.gotNonNull("opt", "b"), feedback);
    }

    @Test
    public void presentComplexValuesAreMatchedStructurally() {
        FeedbackNode feedback = matcher.match("opt",
                Optional.of(new Substructure(true)), Optional.of(new Substructure(false)));
        FeedbackNode expectedFeedback = Feedback.composite("opt", asList(Feedback.nonEqual("Bool", true, false)));
        assertEquals(expectedFeedback, feedback);
    }

    @Test
    public void optionalPropertyIsMatchedEndToEndThroughObjectMatcher() {
        OptionalHolder expected = new OptionalHolder(Optional.of("Ada"));
        OptionalHolder actual = new OptionalHolder(Optional.empty());
        FeedbackNode feedback = ObjectMatcher.forClass(OptionalHolder.class).match(expected, actual);
        FeedbackNode expectedFeedback = Feedback.composite(OptionalHolder.class.getName(), asList(
                Feedback.gotNull("Nickname", "Ada")));
        assertEquals(expectedFeedback, feedback);
    }
}
