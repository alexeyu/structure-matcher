package nl.alexeyu.structmatcher.matcher;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.alexeyu.structmatcher.feedback.Feedback;

public class ExpectAnyValueMatcherTest {

    private ExpectAnyValueMatcher matcher = new ExpectAnyValueMatcher();

    @Test
    public void treatsNullsAsEqual() {
        assertEquals(Feedback.empty("test"), matcher.match("test", null, null));
    }

    @Test
    public void specifiesThatExpectedNullButGotNonNull() {
        assertEquals(Feedback.gotNonNull("test", ""), matcher.match("test", null, ""));
    }

    @Test
    public void specifiesExpectedNonNullButGotNull() {
        assertEquals(Feedback.gotNull("test", ""), matcher.match("test", "", null));
    }

    @Test
    public void emptyFeedbackWhenPropertiesAreEqual() {
        assertEquals(Feedback.empty("test"), matcher.match("test", 100500, 100500));
    }

    @Test
    public void emptyFeedbackWhenPropertiesAreNotEqual() {
        assertEquals(Feedback.empty("test"), matcher.match("test", "x", "y"));
    }

}
