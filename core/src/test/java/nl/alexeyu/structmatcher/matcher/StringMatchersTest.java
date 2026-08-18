package nl.alexeyu.structmatcher.matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.PatternSyntaxException;

import org.junit.jupiter.api.Test;

import nl.alexeyu.structmatcher.feedback.ExpectationBroken;

public class StringMatchersTest {

    private final Matcher<String> matcher = StringMatchers.regex("[a-z]+-\\d+");

    @Test
    public void emptyFeedbackIfActualMatchesTheExpression() {
        assertTrue(matcher.match("test", "order-1", "order-22").isEmpty());
    }

    @Test
    public void reportsAnActualValueTheExpressionRejects() {
        var feedback = matcher.match("test", "order-1", "ORDER-1");
        assertEquals(
                new ExpectationBroken("test", "The regular expression: [a-z]+-\\d+", "ORDER-1"),
                feedback);
    }

    @Test
    public void reportsAPartialMatchAsBroken() {
        assertEquals(
                new ExpectationBroken("test", "The regular expression: [a-z]+-\\d+", "order-1x"),
                matcher.match("test", "order-1", "order-1x"));
    }

    @Test
    public void reportsANullActualValueInsteadOfThrowing() {
        assertEquals(new ExpectationBroken("test", "The regular expression: [a-z]+-\\d+", null),
                matcher.match("test", "order-1", null));
    }

    @Test
    public void blamesTheSpecificationForANullBaseValue() {
        assertThrows(BrokenSpecificationException.class, () -> matcher.match("test", null, "x"));
    }

    @Test
    public void rejectsAnInvalidExpressionWhenTheMatcherIsBuilt() {
        assertThrows(PatternSyntaxException.class, () -> StringMatchers.regex("[a-z"));
    }

    @Test
    public void rejectsANullExpressionWhenTheMatcherIsBuilt() {
        assertThrows(NullPointerException.class, () -> StringMatchers.regex(null));
    }

    @Test
    public void emptyFeedbackIfBothValuesAreNonEmpty() {
        assertTrue(StringMatchers.nonEmpty().match("test", "a", "b").isEmpty());
    }

    @Test
    public void reportsAnEmptyActualValue() {
        assertEquals(new ExpectationBroken("test", "A non-empty string", ""),
                StringMatchers.nonEmpty().match("test", "a", ""));
    }

}
