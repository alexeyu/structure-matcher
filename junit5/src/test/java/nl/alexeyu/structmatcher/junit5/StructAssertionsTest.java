package nl.alexeyu.structmatcher.junit5;

import static java.util.Arrays.asList;
import static nl.alexeyu.structmatcher.junit5.StructAssertions.assertMatches;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import nl.alexeyu.structmatcher.matcher.Maskers;
import nl.alexeyu.structmatcher.matcher.Matchers;
import nl.alexeyu.structmatcher.matcher.ObjectMatcher;

/**
 * Pins the JUnit assertion helpers: a matching pair passes silently, a mismatching pair throws
 * {@link AssertionFailedError} carrying both objects as expected/actual and a per-field diff, a
 * configured spec loosens the comparison, and two nulls are trivially equivalent.
 */
public class StructAssertionsTest {

    private final SampleStructure expected =
            new SampleStructure("black", asList("a"), new SampleSub(true));

    @Test
    public void passesWhenTheStructuresMatch() {
        var actual = new SampleStructure("black", asList("a"), new SampleSub(true));
        assertMatches(expected, actual);
    }

    @Test
    public void throwsWithExpectedActualAndPerFieldDiff() {
        var actual = new SampleStructure("white", asList("b"), new SampleSub(false));
        var error = assertThrows(AssertionFailedError.class, () -> assertMatches(expected, actual));

        // opentest4j carries the two objects so the IDE can show a comparison view.
        // getValue() only retains Serializable values; getEphemeralValue() holds the live object.
        assertEquals(expected, error.getExpected().getEphemeralValue());
        assertEquals(actual, error.getActual().getEphemeralValue());

        var message = error.getMessage();
        assertTrue(message.contains("3 field(s) did not match"), message);
        assertTrue(message.contains("[Color] expected: <black> but was: <white>"), message);
        assertTrue(message.contains("[Tags[0]] expected: <a> but was: <b>"), message);
        assertTrue(message.contains("[Sub.Flag]"), message);
    }

    @Test
    public void aConfiguredSpecCanLoosenTheComparison() {
        var actual = new SampleStructure("white", asList("a"), new SampleSub(true));
        ObjectMatcher<SampleStructure> spec = ObjectMatcher.forClass(SampleStructure.class)
                .with(Matchers.anyValue(), "Color");
        assertMatches(expected, actual, spec); // Color diverges but is ignored; rest matches.
    }

    /**
     * A masking spec has to reach the error itself: the message renders the leaves, and the two
     * objects the IDE comparison view holds would show the field in full.
     */
    @Test
    public void aMaskingSpecWithholdsTheValuesAndTheObjects() {
        var actual = new SampleStructure("white", asList("a"), new SampleSub(true));
        var spec = ObjectMatcher.forClass(SampleStructure.class)
                .with(Matchers.valuesEqual().masking(Maskers.redacted()), "Color");
        var error = assertThrows(AssertionFailedError.class,
                () -> assertMatches(expected, actual, spec));

        var message = error.getMessage();
        assertTrue(message.contains("[Color] expected: <***> but was: <***>"), message);
        assertFalse(message.contains("black"), message);
        assertNull(error.getExpected());
        assertNull(error.getActual());
    }

    @Test
    public void twoNullsAreEquivalent() {
        assertMatches((SampleStructure) null, (SampleStructure) null);
    }

}
