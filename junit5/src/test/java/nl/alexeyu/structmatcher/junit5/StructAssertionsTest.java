package nl.alexeyu.structmatcher.junit5;

import static java.util.Arrays.asList;
import static nl.alexeyu.structmatcher.junit5.StructAssertions.assertMatches;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opentest4j.AssertionFailedError;

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
        assertTrue(message, message.contains("3 field(s) did not match"));
        assertTrue(message, message.contains("[Color] expected: <black> but was: <white>"));
        assertTrue(message, message.contains("[Tags[0]] expected: <a> but was: <b>"));
        assertTrue(message, message.contains("[Sub.Flag]"));
    }

    @Test
    public void aConfiguredSpecCanLoosenTheComparison() {
        var actual = new SampleStructure("white", asList("a"), new SampleSub(true));
        ObjectMatcher<SampleStructure> spec = ObjectMatcher.forClass(SampleStructure.class)
                .with(Matchers.anyValue(), "Color");
        assertMatches(expected, actual, spec); // Color diverges but is ignored; rest matches.
    }

    @Test
    public void twoNullsAreEquivalent() {
        assertMatches((SampleStructure) null, (SampleStructure) null);
    }

}
