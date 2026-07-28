package nl.alexeyu.structmatcher.matcher;

import java.util.Optional;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Matches two {@link Optional} values, treating an empty optional as <code>null</code>. It unwraps
 * both sides to the contained value (or <code>null</code> when empty) and runs the usual logic on
 * those, simple values by equality and complex ones structurally. Two empty optionals match; an
 * empty one against a present one does not. A non-optional value passes through untouched, so
 * registering this matcher on the wrong property costs nothing.
 */
public final class OptionalMatcher implements Matcher<Object> {

    @Override
    public FeedbackNode match(String property, Object expected, Object actual) {
        var expectedValue = unwrap(expected);
        var actualValue = unwrap(actual);
        return Matchers.getNullAwareMatcher(actualValue).match(property, expectedValue,
                actualValue);
    }

    private Object unwrap(Object value) {
        return value instanceof Optional<?> optional ? optional.orElse(null) : value;
    }

}
