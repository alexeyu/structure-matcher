package nl.alexeyu.structmatcher.matcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import nl.alexeyu.structmatcher.feedback.Feedback;

public class NullAwareMatcherTest {
    
    private PartialMatcher matcher = new NullAwareMatcher();
    
    @Test
    public void treatsNullsAsEqualObjects() {
        assertEquals(Feedback.empty("test"), matcher.maybeMatch("test", null, null).orElse(null));
    }

    @Test
    public void nullFeedbackOnNonNullObjects() {
        assertFalse(matcher.maybeMatch("test", "something", "something else").isPresent());
    }

    @Test
    public void specifiesThatExpectedNullButGotNonNull() {
        assertEquals(Feedback.gotNonNull("test", ""), matcher.maybeMatch("test", null, "").orElse(null));
    }

    @Test
    public void specifiesExpectedNonNullButGotNull() {
        assertEquals(Feedback.gotNull("test", "something"),
                matcher.maybeMatch("test", "something", null).orElse(null));
    }

}
