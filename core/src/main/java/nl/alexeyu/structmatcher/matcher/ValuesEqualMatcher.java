package nl.alexeyu.structmatcher.matcher;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Matcher which considers two values matching if they are equal.
 * Obviously, an expected value cannot be null, so it is recommended
 * to use this matcher with the {@link NullAwareMatcher}.
 */
final class ValuesEqualMatcher<V> implements Matcher<V> {
    
    @Override
    public FeedbackNode match(String property, V expected, V actual) {
        return expected.equals(actual)
                ? Feedback.empty(property)
                : Feedback.nonEqual(property, expected, actual);
    }
    
}
