package nl.alexeyu.structmatcher.matcher;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

public class SimplePropertyMatcherTest {

    @Test
    public void emptyFeedbackWhenPropertiesAreEqual() {
        FeedbackNode feedback = new SimplePropertyMatcher().match("test", 100500, 100500); 
        assertEquals(Feedback.empty("test"), feedback);
    }

    @Test
    public void nonEqualPropertyFeedbackWhenPropertiesAreNotEqual() {
        FeedbackNode feedback = new SimplePropertyMatcher().match("test", "x", "y");
        assertEquals(Feedback.nonEqual("test", "x", "y"), feedback);
    }

}
