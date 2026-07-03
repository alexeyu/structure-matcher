package nl.alexeyu.structmatcher.matcher;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Covers the fluent composition default methods on {@link Matcher} ({@code and} and the three
 * {@code normalizing*} variants), including that the static {@link Matchers} factories now route
 * through them.
 */
class MatcherCompositionTest {

    private final Matcher<String> equal = Matchers.valuesEqual();

    @Test
    void andIsSatisfiedOnlyWhenBothAre() {
        var both = equal.and(Matchers.constant("a"));
        assertTrue(both.match("p", "a", "a").isEmpty());   // equal, and actual is "a"
    }

    @Test
    void andFailsWhenTheFirstFails() {
        var both = equal.and(Matchers.constant("a"));
        assertFalse(both.match("p", "a", "b").isEmpty());  // not equal, second never reached
    }

    @Test
    void andFailsWhenTheSecondFails() {
        var both = equal.and(Matchers.constant("x"));
        assertFalse(both.match("p", "a", "a").isEmpty());  // equal, but actual is not "x"
    }

    @Test
    void normalizingRewritesTheActualValue() {
        assertTrue(equal.normalizing(String::trim).match("p", "Alex", " Alex ").isEmpty());
    }

    @Test
    void normalizingBaseRewritesTheBaseValue() {
        assertTrue(equal.normalizingBase(name -> name.charAt(0) + ".")
                .match("p", "Alex", "A.").isEmpty());
    }

    @Test
    void normalizingBothRewritesBothValues() {
        assertTrue(equal.normalizingBoth(String::trim).match("p", "  Alex", "Alex  ").isEmpty());
    }

    @Test
    void staticFactoryMatchesTheFluentForm() {
        var fluent = equal.normalizingBoth(String::trim);
        var viaFactory = Matchers.normalizingBoth(String::trim, equal);
        assertTrue(fluent.match("p", " x ", "x").isEmpty());
        assertTrue(viaFactory.match("p", " x ", "x").isEmpty());
    }

}
