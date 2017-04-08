package nl.alexeyu.structmatcher.matcher;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.alexeyu.structmatcher.feedback.Feedback;

public class SimplePropertyMatcherTest {

    private SimplePropertyMatcher simpleMatcher = new SimplePropertyMatcher();

    @Test
    public void treatsNullsAsEqual() {
        assertEquals(Feedback.empty("test"), simpleMatcher.match("test", null, null));
    }

    @Test
    public void specifiesThatExpectedNullButGotNonNull() {
        assertEquals(Feedback.gotNonNull("test", ""), simpleMatcher.match("test", null, ""));
    }

    @Test
    public void specifiesExpectedNonNullButGotNull() {
        assertEquals(Feedback.gotNull("test", ""), simpleMatcher.match("test", "", null));
    }

    @Test
    public void emptyFeedbackWhenPropertiesAreEqual() {
        assertEquals(Feedback.empty("test"), simpleMatcher.match("test", 100500, 100500));
    }

    @Test
    public void nonEqualPropertyFeedbackWhenPropertiesAreNotEqual() {
        assertEquals(Feedback.nonEqual("test", "x", "y"), simpleMatcher.match("test", "x", "y"));
    }

}
