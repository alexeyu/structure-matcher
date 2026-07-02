package nl.alexeyu.structmatcher.assertj;

import static java.util.Arrays.asList;
import static nl.alexeyu.structmatcher.assertj.StructMatcherAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.Test;

import nl.alexeyu.structmatcher.matcher.Matchers;
import nl.alexeyu.structmatcher.matcher.ObjectMatcher;

/**
 * Pins the AssertJ bridge: a matching pair passes (chainable), a mismatching pair fails with the
 * structured per-field diff (canonical paths + expected/actual values), a configured spec loosens
 * the comparison, and a null actual is rejected like any AssertJ assertion.
 */
public class StructureAssertTest {

    private final SampleStructure expected =
            new SampleStructure("black", asList("a"), new SampleSub(true));

    @Test
    public void passesWhenTheStructuresMatch() {
        var actual = new SampleStructure("black", asList("a"), new SampleSub(true));
        assertThatNoException().isThrownBy(() -> assertThat(actual).matchesStructure(expected));
    }

    @Test
    public void failsWithTheStructuredPerFieldDiff() {
        var actual = new SampleStructure("white", asList("b"), new SampleSub(false));
        assertThatExceptionOfType(AssertionError.class)
                .isThrownBy(() -> assertThat(actual).matchesStructure(expected))
                .withMessageContaining("3 field(s) diverged")
                .withMessageContaining("[Color] expected: <black> but was: <white>")
                .withMessageContaining("[Tags[0]] expected: <a> but was: <b>")
                .withMessageContaining("[Sub.Flag]");
    }

    @Test
    public void aConfiguredSpecCanLoosenTheComparison() {
        var actual = new SampleStructure("white", asList("a"), new SampleSub(true));
        ObjectMatcher<SampleStructure> spec = ObjectMatcher.forClass(SampleStructure.class)
                .with(Matchers.anyValue(), "Color");
        // Color diverges but is ignored by the spec; the rest matches, so no failure.
        assertThatNoException()
                .isThrownBy(() -> assertThat(actual).matchesStructure(expected, spec));
    }

    @Test
    public void rejectsANullActual() {
        assertThatExceptionOfType(AssertionError.class)
                .isThrownBy(() -> assertThat((SampleStructure) null).matchesStructure(expected));
    }

}
