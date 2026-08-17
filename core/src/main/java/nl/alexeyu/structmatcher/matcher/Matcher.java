package nl.alexeyu.structmatcher.matcher;

import java.util.function.UnaryOperator;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Tests a property value against an expectation and returns the feedback. Empty feedback means the
 * value met the expectation; non-empty feedback carries what a reader needs to see the difference:
 * the property name, the expected value or condition, and the actual value.
 * <p>
 * Matchers compose in a fluent, left-to-right style, the way {@link java.util.function.Predicate}
 * and {@link java.util.Comparator} do:
 *
 * <pre>
 * Matchers.&lt;String&gt;nonNull()
 *         .and(StringMatchers.nonEmpty())
 *         .and(Matchers.&lt;String&gt;valuesEqual().normalizingBase(n -&gt; n.charAt(0) + "."));
 * </pre>
 */
@FunctionalInterface
public interface Matcher<V> {

    FeedbackNode match(String property, V expected, V actual);

    /**
     * Runs this matcher first and, only if it is satisfied, the {@code other} one. The first
     * non-empty feedback wins, so the combined matcher is satisfied only when both are.
     */
    default Matcher<V> and(Matcher<V> other) {
        return (property, expected, actual) -> {
            var feedback = match(property, expected, actual);
            return feedback.isEmpty() ? other.match(property, expected, actual) : feedback;
        };
    }

    /** Applies {@code normalizer} to the actual value before matching it against the base one. */
    default Matcher<V> normalizing(UnaryOperator<V> normalizer) {
        return (property, expected, actual) -> match(property, expected, normalizer.apply(actual));
    }

    /** Applies {@code normalizer} to the base value before matching the actual one against it. */
    default Matcher<V> normalizingBase(UnaryOperator<V> normalizer) {
        return (property, expected, actual) -> match(property, normalizer.apply(expected), actual);
    }

    /** Applies {@code normalizer} to both values before matching them. */
    default Matcher<V> normalizingBoth(UnaryOperator<V> normalizer) {
        return (property, expected, actual) -> match(property, normalizer.apply(expected),
                normalizer.apply(actual));
    }

}
