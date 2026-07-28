package nl.alexeyu.structmatcher.matcher;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Matches two values that are equal. It calls <code>equals</code> on the expected value, so wrap it
 * in a {@link NullAwareMatcher} to handle the null cases first.
 */
final class ValuesEqualMatcher<V> implements Matcher<V> {

    @Override
    public FeedbackNode match(String property, V expected, V actual) {
        return expected.equals(actual)
                ? Feedback.empty(property)
                : Feedback.nonEqual(property, expected, actual);
    }

}
