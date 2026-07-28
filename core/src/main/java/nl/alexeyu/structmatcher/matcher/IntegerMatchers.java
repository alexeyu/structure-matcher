package nl.alexeyu.structmatcher.matcher;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Factory of integer-specific matchers. Each one is strict, in the sense
 * {@link MustConformMatcher} defines: an actual value that fails the test produces non-empty
 * feedback, while a base value that fails it throws <code>BrokenSpecificationException</code>. A
 * value counts as an integer when {@link Integer#valueOf(String)} parses its string form.
 */
public final class IntegerMatchers {

    private static final Function<Object, Optional<Integer>> TO_INT = IntegerMatchers::toInt;

    private IntegerMatchers() {
    }

    /**
     * Accepts any integer.
     *
     * @see MustConformMatcher
     */
    public static Matcher<Object> any() {
        return new MustConformMatcher<>(v -> TO_INT.apply(v).isPresent(), "An integer");
    }

    /**
     * Accepts an integer of 0 or above.
     *
     * @see MustConformMatcher
     */
    public static Matcher<Object> nonNegative() {
        return greaterThan(-1);
    }

    /**
     * Accepts an integer above 0.
     *
     * @see MustConformMatcher
     */
    public static Matcher<Object> positive() {
        return greaterThan(0);
    }

    /**
     * Accepts an integer greater than <code>value</code>.
     *
     * @see MustConformMatcher
     */
    public static Matcher<Object> greaterThan(int value) {
        return new MustConformMatcher<>(v -> TO_INT.apply(v).orElse(Integer.MIN_VALUE) > value,
                "An integer greater than " + value);
    }

    /**
     * Accepts an integer below 0.
     *
     * @see MustConformMatcher
     */
    public static Matcher<Object> negative() {
        return lessThan(0);
    }

    /**
     * Accepts an integer less than <code>value</code>.
     *
     * @see MustConformMatcher
     */
    public static Matcher<Object> lessThan(int value) {
        return new MustConformMatcher<>(v -> TO_INT.apply(v).orElse(Integer.MAX_VALUE) < value,
                "An integer less than " + value);
    }

    /**
     * Accepts an integer inside the range, both bounds excluded.
     *
     * @param minExclusive
     *            the value must be greater than this.
     * @param maxExclusive
     *            the value must be smaller than this.
     */
    public static Matcher<Object> inRange(int minExclusive, int maxExclusive) {
        return new MustConformMatcher<>(
                v -> TO_INT.andThen(new Within(minExclusive, maxExclusive)).apply(v),
                String.format("Bigger than %s but smaller than %s", minExclusive, maxExclusive));
    }

    /**
     * Accepts an integer drawn from a known set, e.g. the ports a service pool listens on.
     *
     * @param possibleValues
     *            the values the matcher accepts.
     */
    public static Matcher<Object> oneOf(Integer... possibleValues) {
        var possibleValuesList = Arrays.asList(possibleValues);
        return new MustConformMatcher<>(v -> TO_INT.andThen(new OneOf(possibleValuesList)).apply(v),
                String.format("One of the following values: %s", possibleValuesList));
    }

    private record Within(int minExclusive, int maxExclusive)
            implements Function<Optional<Integer>, Boolean> {

        @Override
        public Boolean apply(Optional<Integer> t) {
            return t.isPresent() && t.get() > minExclusive && t.get() < maxExclusive;
        }

    }

    private record OneOf(Set<Integer> possibleValues)
            implements Function<Optional<Integer>, Boolean> {

        OneOf(Collection<Integer> possibleValues) {
            this(new HashSet<>(possibleValues));
        }

        @Override
        public Boolean apply(Optional<Integer> t) {
            return t.isPresent() && possibleValues.contains(t.get());
        }

    }

    private static Optional<Integer> toInt(Object t) {
        try {
            return Optional.of(Integer.valueOf(String.valueOf(t)));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

}
