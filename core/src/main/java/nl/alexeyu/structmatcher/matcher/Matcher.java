package nl.alexeyu.structmatcher.matcher;

import java.util.function.UnaryOperator;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Tests a value of a property against an expectation and returns feedback about it. If the feedback
 * is empty, the expectation is considered fulfilled.
 * <p/>
 * If the Feedback is not empty, the values are considered non matching. The result should contain
 * the necessary information for such a case (a name of the property, an expected value or condition
 * and the actual value).
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
     *
     * @param other
     *            the matcher to apply after this one.
     * @return a matcher that requires both.
     */
    default Matcher<V> and(Matcher<V> other) {
        return (property, expected, actual) -> {
            var feedback = match(property, expected, actual);
            return feedback.isEmpty() ? other.match(property, expected, actual) : feedback;
        };
    }

    /**
     * Applies {@code normalizer} to the actual value before matching it against the base one.
     *
     * @param normalizer
     *            the function applied to the actual value.
     * @return a matcher that normalizes the actual value first.
     */
    default Matcher<V> normalizing(UnaryOperator<V> normalizer) {
        return (property, expected, actual) -> match(property, expected, normalizer.apply(actual));
    }

    /**
     * Applies {@code normalizer} to the base value before matching the actual one against it.
     *
     * @param normalizer
     *            the function applied to the base value.
     * @return a matcher that normalizes the base value first.
     */
    default Matcher<V> normalizingBase(UnaryOperator<V> normalizer) {
        return (property, expected, actual) -> match(property, normalizer.apply(expected), actual);
    }

    /**
     * Applies {@code normalizer} to both values before matching them.
     *
     * @param normalizer
     *            the function applied to both values.
     * @return a matcher that normalizes both values first.
     */
    default Matcher<V> normalizingBoth(UnaryOperator<V> normalizer) {
        return (property, expected, actual) -> match(property, normalizer.apply(expected),
                normalizer.apply(actual));
    }

}
