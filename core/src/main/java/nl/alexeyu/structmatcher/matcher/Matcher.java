package nl.alexeyu.structmatcher.matcher;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Tests a value of a property against an expectation and returns feedback about
 * it. If the feedback is empty, the expectation is considered fulfilled.
 * <p/>
 * If the Feedback is not empty, the values are considered non matching. The
 * result should contain the necessary information for such a case (a name of
 * the property, an expected value or condition and the actual value).
 */
@FunctionalInterface
public interface Matcher<V> {

    FeedbackNode match(String property, V expected, V actual);

}
