package nl.alexeyu.structmatcher.matcher;

import java.util.Optional;
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

    /**
     * Runs this matcher unchanged and withholds the values from the feedback it produces, so a
     * sensitive field is compared as strictly as before but never printed:
     *
     * <pre>
     * spec.with(Matchers.valuesEqual().masking(Maskers.hash()), "*.Email");
     * </pre>
     *
     * The masker replaces values, never the conditions a matcher states in words ('Non-null',
     * 'Size 3'), and reaches the whole subtree, so masking a structure redacts every field under
     * it. It reaches <em>upwards</em> too: where a comparison stops above a masked path, because
     * one side is null or the two clash, the whole object lands in one leaf and this matcher never
     * runs, so an ancestor reporting a value whole withholds it as well.
     * <p>
     * Registration is per path, so this redacts what you point it at and nothing else.
     */
    default Matcher<V> masking(Masker masker) {
        return new MaskingMatcher<>(this, masker);
    }

    /**
     * The masker this matcher withholds its values with, empty when it withholds nothing. A
     * consumer that would otherwise print the values - an assertion message, an archive - asks
     * this rather than testing a type, since masking sits inside an {@link IndirectMatcher} rather
     * than around it.
     * <p>
     * Composition drops it: {@code masking(m).and(other)} answers empty, since {@code other}'s
     * feedback is unmasked. Mask last.
     */
    default Optional<Masker> masker() {
        return Optional.empty();
    }

    /** Applies {@code normalizer} to both values before matching them. */
    default Matcher<V> normalizingBoth(UnaryOperator<V> normalizer) {
        return (property, expected, actual) -> match(property, normalizer.apply(expected),
                normalizer.apply(actual));
    }

}
