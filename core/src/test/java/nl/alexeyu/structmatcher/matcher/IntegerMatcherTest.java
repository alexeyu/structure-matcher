package nl.alexeyu.structmatcher.matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicLong;

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

    @Test
    public void acceptsWholeNumbersOfAnyNumericType() {
        assertTrue(matcher.match("test", 42.0d, 42.0f).isEmpty());
        assertTrue(matcher.match("test", new BigDecimal("42.00"), new AtomicLong(7)).isEmpty());
        assertTrue(matcher.match("test", (short) 3, BigInteger.TEN).isEmpty());
    }

    @Test
    public void acceptsALongBeyondIntRange() {
        assertTrue(matcher.match("test", 3_000_000_000L, 5_000_000_000L).isEmpty());
    }

    @Test
    public void rejectsANumberWithAFractionalPart() {
        assertEquals(new ExpectationBroken("test", "An integer", 0.5d),
                matcher.match("test", 1, 0.5d));
        assertEquals(new ExpectationBroken("test", "An integer", new BigDecimal("0.50")),
                matcher.match("test", 1, new BigDecimal("0.50")));
    }

    @Test
    public void rejectsANumberThatIsNotFinite() {
        assertEquals(new ExpectationBroken("test", "An integer", Double.NaN),
                matcher.match("test", 1, Double.NaN));
        assertEquals(new ExpectationBroken("test", "An integer", Double.POSITIVE_INFINITY),
                matcher.match("test", 1, Double.POSITIVE_INFINITY));
    }

    @Test
    public void parsesAStringHoldingANumber() {
        assertTrue(matcher.match("test", "42", "42.0").isEmpty());
        assertEquals(new ExpectationBroken("test", "An integer", "42.5"),
                matcher.match("test", "42", "42.5"));
    }

    @Test
    public void reportsANullActualValue() {
        assertEquals(new ExpectationBroken("test", "An integer", null),
                matcher.match("test", 1, null));
    }

    @Test
    public void comparesALongBeyondIntRangeByValue() {
        // (int) 4_294_967_338L is 42, so truncating would place this inside the range
        assertEquals(new ExpectationBroken("test", "Bigger than 0 but smaller than 100",
                4_294_967_338L), IntegerMatchers.inRange(0, 100).match("test", 42, 4_294_967_338L));
        assertEquals(new ExpectationBroken("test", "An integer less than 0", 3_000_000_000L),
                IntegerMatchers.lessThan(0).match("test", -1, 3_000_000_000L));
    }

    @Test
    public void comparesWholeNumbersOfAnyTypeAgainstBounds() {
        assertTrue(IntegerMatchers.inRange(0, 100).match("test", 42, 42.0d).isEmpty());
        assertTrue(IntegerMatchers.positive().match("test", 1, new BigDecimal("7.0")).isEmpty());
        assertTrue(IntegerMatchers.oneOf(8080, 9090).match("test", 8080, 9090L).isEmpty());
    }

    @Test
    public void doesNotBlameTheSpecificationForAWholeNumberBaseValue() {
        assertTrue(IntegerMatchers.inRange(0, 100).match("test", 42.0d, 43).isEmpty());
    }

}
