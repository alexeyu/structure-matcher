package nl.alexeyu.structmatcher.matcher;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.alexeyu.structmatcher.feedback.Feedback;

public class ValuesEqualMatcherTest {

    @Test
    public void emptyFeedbackWhenPropertiesAreEqual() {
        var feedback = new ValuesEqualMatcher<>().match("test", 100500, 100500);
        assertEquals(Feedback.empty("test"), feedback);
    }

    @Test
    public void nonEqualPropertyFeedbackWhenPropertiesAreNotEqual() {
        var feedback = new ValuesEqualMatcher<>().match("test", "x", "y");
        assertEquals(Feedback.nonEqual("test", "x", "y"), feedback);
    }

}
