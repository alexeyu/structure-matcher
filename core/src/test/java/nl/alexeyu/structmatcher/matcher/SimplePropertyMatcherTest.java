package nl.alexeyu.structmatcher.matcher;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.alexeyu.structmatcher.feedback.Feedback;

public class SimplePropertyMatcherTest {

    private Matcher simpleMatcher = new SimplePropertyMatcher();

    @Test
    public void emptyFeedbackWhenPropertiesAreEqual() {
        assertEquals(Feedback.empty("test"), simpleMatcher.match("test", 100500, 100500));
    }

    @Test
    public void nonEqualPropertyFeedbackWhenPropertiesAreNotEqual() {
        assertEquals(Feedback.nonEqual("test", "x", "y"), simpleMatcher.match("test", "x", "y"));
    }

}
