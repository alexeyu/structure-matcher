package nl.alexeyu.structmatcher.matcher;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Factory of integer-specific matchers. 
 */
public class IntegerMatchers {
    
    private static final ToInteger TO_INT = new ToInteger(); 
    
    private IntegerMatchers() {
    }

    /**
     * Returns a strict matcher which considers an actual value matching if it
     * is any integer. Please note that the matcher will throw
     * <code>BrokenSpecificationException</code> if a base value is not an
     * integer.
     * 
     * @return a matcher with the behavior specified above.
     * @see {@link MustConformMatcher}
     */
    public static Matcher<Object> any() {
        return new MustConformMatcher<>(v -> TO_INT.apply(v).isPresent(), "An integer");
    }

    /**
     * Returns a strict matcher which considers an actual value matching if it
     * is any non-negative integer. Please note that the matcher will throw
     * <code>BrokenSpecificationException</code> if a base value is not a
     * positive integer.
     * 
     * @return a matcher with the behavior specified above.
     * @see {@link MustConformMatcher}
     */
    public static Matcher<Object> nonNegative() {
        return greaterThan(-1);
    }

    /**
     * Returns a strict matcher which considers an actual value matching if it
     * is any positive integer. Please note that the matcher will throw
     * <code>BrokenSpecificationException</code> if a base value is not a
     * positive integer.
     * 
     * @return a matcher with the behavior specified above.
     * @see {@link MustConformMatcher}
     */
    public static Matcher<Object> positive() {
        return greaterThan(0);
    }

    /**
     * Returns a strict matcher which considers an actual value matching if it
     * is greater than a parameter value. Please note that the matcher will
     * throw <code>BrokenSpecificationException</code> if a base value is not a
     * positive integer.
     * 
     * @return a matcher with the behavior specified above.
     * @see {@link MustConformMatcher}
     */
    public static Matcher<Object> greaterThan(int value) {
        return new MustConformMatcher<>(
                v -> TO_INT.apply(v).orElse(Integer.MIN_VALUE) > value,
                "An integer greater than " + value);
    }

    /**
     * Returns a strict matcher which considers an actual value matching if it
     * is any negative integer. Please note that the matcher will throw
     * <code>BrokenSpecificationException</code> if a base value is not a
     * positive integer.
     * 
     * @return a matcher with the behavior specified above.
     * @see {@link MustConformMatcher}
     */
    public static Matcher<Object> negative() {
        return lessThan(0);
    }

    /**
     * Returns a strict matcher which considers an actual value matching if it
     * is less than a parameter value. Please note that the matcher will
     * throw <code>BrokenSpecificationException</code> if a base value is not a
     * positive integer.
     * 
     * @return a matcher with the behavior specified above.
     * @see {@link MustConformMatcher}
     */
    public static Matcher<Object> lessThan(int value) {
        return new MustConformMatcher<>(
                v -> TO_INT.apply(v).orElse(Integer.MAX_VALUE) < value,
                "An integer less than " + value);
    }

    /**
     * Returns a strict matcher which considers an actual value matching if it
     * belongs to a specified range. Please note that the matcher will throw
     * <code>BrokenSpecificationException</code> if a base value is not within a
     * given range.
     * 
     * @param minExclusive
     *            a minimum of the range: a value being verified should be
     *            bigger than this parameter.
     * @param maxExclusive
     *            a maximum of the range: a value being verified should be
     *            bigger than this parameter.
     * @return a matcher with the behavior specified above.
     */
    public static Matcher<Object> inRange(int minExclusive, int maxExclusive) {
        return new MustConformMatcher<>(
                v -> TO_INT.andThen(new Within(minExclusive, maxExclusive)).apply(v),
                String.format("Bigger than %s but smaller than %s", minExclusive, maxExclusive));
    }

    /**
     * Returns a strict matcher which considers an actual value matching if it
     * belongs to a set of specified numbers. Please note that the matcher will
     * throw <code>BrokenSpecificationException</code> if it does not belong to
     * the argument list.
     * 
     * @param possibleValues
     *            a list of values a value being verified should belong to.
     * @return a matcher with the behavior specified above.
     */
    public static Matcher<Object> oneOf(Integer... possibleValues) {
        List<Integer> possibleValuesList = Arrays.asList(possibleValues);
        return new MustConformMatcher<>(
                v -> TO_INT.andThen(new OneOf(possibleValuesList)).apply(v),
                String.format("One of the following values: %s", possibleValuesList));
    }

    private static class Within implements Function<Optional<Integer>, Boolean> {
        
        private final int minExclusive;

        private final int maxExclusive;

        public Within(int minExclusive, int maxExclusive) {
            this.minExclusive = minExclusive;
            this.maxExclusive = maxExclusive;
        }

        @Override
        public Boolean apply(Optional<Integer> t) {
            return t.isPresent() && t.get() > minExclusive && t.get() < maxExclusive;
        }
        
    }

    private static class OneOf implements Function<Optional<Integer>, Boolean> {
        
        private final Set<Integer> possibleValues;

        public OneOf(Collection<Integer> possibleValues) {
            this.possibleValues = new HashSet<>(possibleValues);
        }

        @Override
        public Boolean apply(Optional<Integer> t) {
            return t.isPresent() && possibleValues.contains(t.get());
        }
        
    }
    
    private static class ToInteger implements Function<Object, Optional<Integer>> {

        @Override
        public Optional<Integer> apply(Object t) {
            try {
                return Optional.of(
                        Integer.valueOf(
                                String.valueOf(t)));
            } catch (NumberFormatException ex) {
                return Optional.empty();
            }
        }
        
    }

    
}
