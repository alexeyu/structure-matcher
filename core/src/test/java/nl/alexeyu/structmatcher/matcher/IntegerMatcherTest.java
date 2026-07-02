package nl.alexeyu.structmatcher.matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nl.alexeyu.structmatcher.feedback.ExpectationBroken;

public class IntegerMatcherTest {

    private Matcher<Object> matcher = IntegerMatchers.any();

    @Test
    public void throwsExceptionIfExpectedIsNotAnInt() {
        assertThrows(BrokenSpecificationException.class,
                () -> matcher.match("test", "whatever", "whatever"));
    }

    @Test
    public void emptyFeedbackIfActualIsAnInteger() {
        assertTrue(matcher.match("test", -1, 0).isEmpty());
    }

    @Test
    public void wrongTypeFeedbackIfActualIsNotAnInteger() {
        var feedback = matcher.match("test", -1, 1.2f);
        assertEquals(new ExpectationBroken("test", "An integer", 1.2f), feedback);
    }

}
