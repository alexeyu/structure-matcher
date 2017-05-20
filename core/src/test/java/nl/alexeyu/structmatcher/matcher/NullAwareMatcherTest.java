package nl.alexeyu.structmatcher.matcher;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

public class NullAwareMatcherTest {
    
    private Matcher<Object> failingMatcher = (p, e, a) -> { throw new IllegalStateException("Should not be called"); };
    
    @Test
    public void treatsNullsAsEqualObjects() {
        Matcher<Object> m = new NullAwareMatcher<>(failingMatcher);
        assertEquals(Feedback.empty("test"), m.match("test", null, null));
    }

    @Test
    public void passesControlToDelegateMatcherIfBothValuesAreNotNull() {
        FakeMatcher delegateMatcher = new FakeMatcher();
        Matcher<Object> m = new NullAwareMatcher<>(delegateMatcher);
        m.match("test", "something", "something else");
        assertEquals("test", delegateMatcher.property);
        assertEquals("something", delegateMatcher.expected);
        assertEquals("something else", delegateMatcher.actual);
    }

    @Test
    public void specifiesThatExpectedNullButGotNonNull() {
        Matcher<Object> m = new NullAwareMatcher<>(failingMatcher);
        assertEquals(Feedback.gotNonNull("test", ""), m.match("test", null, ""));
    }

    @Test
    public void specifiesExpectedNonNullButGotNull() {
        Matcher<Object> m = new NullAwareMatcher<>(failingMatcher);
        assertEquals(Feedback.gotNull("test", "something"),
                m.match("test", "something", null));
    }

    private static class FakeMatcher implements Matcher<Object> {
        
        String property;
        Object expected, actual;
        
        @Override
        public FeedbackNode match(String property, Object expected, Object actual) {
            this.property = property;
            this.expected = expected;
            this.actual = actual;
            return Feedback.empty(property);
        }
        
    }
}
