package nl.alexeyu.structmatcher.matcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.feedback.NonEqualPropertyFeedbackNode;

public class IntegerMatcherTest {
    
    private Matcher matcher = PredicateMatchers.integer();
    
    @Test(expected = IllegalArgumentException.class)
    public void throwsExceptionIfExpectedIsNotAnInt() {
        matcher.match("test", "whatever", "whatever");
    }

    @Test
    public void emptyFeedbackIfActualIsAnInteger() {
        assertTrue(matcher.match("test", -1, 0).isEmpty());
    }

    @Test
    public void wrongTypeFeedbackIfActualIsNotAnInteger() {
        FeedbackNode feedback = matcher.match("test", -1, 1.2f);
        assertEquals(new NonEqualPropertyFeedbackNode("test", "Must be an integer", 1.2f), feedback);
    }

}
