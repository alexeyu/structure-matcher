package nl.alexeyu.structmatcher.matcher;

import java.util.Optional;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Matches two {@link Optional} values by treating an empty optional as if it were
 * <code>null</code>: it unwraps both sides to their contained value (or
 * <code>null</code> when empty) and matches those the way any other value is
 * matched — simple values by equality, complex values structurally. Two empty
 * optionals match; a present one and an empty one do not. A non-optional value is
 * passed through unchanged, so the matcher is harmless if applied to one.
 */
public final class OptionalMatcher implements Matcher<Object> {

    @Override
    public FeedbackNode match(String property, Object expected, Object actual) {
        Object expectedValue = unwrap(expected);
        Object actualValue = unwrap(actual);
        return Matchers.getNullAwareMatcher(actualValue).match(property, expectedValue, actualValue);
    }

    private Object unwrap(Object value) {
        return value instanceof Optional<?> optional ? optional.orElse(null) : value;
    }

}
