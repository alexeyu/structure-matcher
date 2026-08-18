package nl.alexeyu.structmatcher.matcher;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Factory of integer-specific matchers. Each one is strict, in the sense
 * {@link MustConformMatcher} defines: an actual value that fails the test produces non-empty
 * feedback, while a base value that fails it throws <code>BrokenSpecificationException</code>. A
 * value counts as an integer when it is a whole {@link Number} of any type, or a string holding
 * one. The bounds below are ints, while each matcher compares the value at its own magnitude, so a
 * <code>long</code> outside int range still answers as the number it is.
 */
public final class IntegerMatchers {

    private IntegerMatchers() {
    }

    /**
     * Accepts any integer.
     */
    public static Matcher<Object> any() {
        return conforming(n -> true, "An integer");
    }

    /**
     * Accepts an integer of 0 or above.
     */
    public static Matcher<Object> nonNegative() {
        return greaterThan(-1);
    }

    /**
     * Accepts an integer above 0.
     */
    public static Matcher<Object> positive() {
        return greaterThan(0);
    }

    /**
     * Accepts an integer greater than <code>value</code>.
     */
    public static Matcher<Object> greaterThan(int value) {
        var bound = BigInteger.valueOf(value);
        return conforming(n -> n.compareTo(bound) > 0, "An integer greater than " + value);
    }

    /**
     * Accepts an integer below 0.
     */
    public static Matcher<Object> negative() {
        return lessThan(0);
    }

    /**
     * Accepts an integer less than <code>value</code>.
     */
    public static Matcher<Object> lessThan(int value) {
        var bound = BigInteger.valueOf(value);
        return conforming(n -> n.compareTo(bound) < 0, "An integer less than " + value);
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
        var min = BigInteger.valueOf(minExclusive);
        var max = BigInteger.valueOf(maxExclusive);
        return conforming(n -> n.compareTo(min) > 0 && n.compareTo(max) < 0,
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
        var accepted = possibleValuesList.stream().map(BigInteger::valueOf)
                .collect(Collectors.toSet());
        return conforming(accepted::contains,
                String.format("One of the following values: %s", possibleValuesList));
    }

    private static Matcher<Object> conforming(Predicate<BigInteger> condition, String spec) {
        return new MustConformMatcher<>(v -> toInteger(v).filter(condition).isPresent(), spec);
    }

    /**
     * Reads a value as a whole number, keeping a fractional part and a magnitude past int range
     * intact for the comparison to judge. The matcher asks a {@link Number} for its own value and
     * puts anything else through its string form, which is how a numeric string gets in.
     */
    private static Optional<BigInteger> toInteger(Object value) {
        if (value instanceof BigInteger integer) {
            return Optional.of(integer);
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer
                || value instanceof Long) {
            return Optional.of(BigInteger.valueOf(((Number) value).longValue()));
        }
        if (value instanceof BigDecimal decimal) {
            return wholeNumber(decimal);
        }
        return parse(String.valueOf(value));
    }

    private static Optional<BigInteger> parse(String text) {
        try {
            return wholeNumber(new BigDecimal(text));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private static Optional<BigInteger> wholeNumber(BigDecimal decimal) {
        try {
            return Optional.of(decimal.toBigIntegerExact());
        } catch (ArithmeticException ex) {
            return Optional.empty();
        }
    }

}
