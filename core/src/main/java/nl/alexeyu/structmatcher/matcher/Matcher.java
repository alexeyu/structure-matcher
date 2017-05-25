package nl.alexeyu.structmatcher.matcher;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Tests a value of a property against an expectation and returns feedback about
 * it. If the feedback is empty, the expectation is considered fulfilled. A
 * custom matcher may return a special value <code>Feedback.useDefault()</code>.
 * In this case the mechanism will use the default matcher for given property to
 * derive the feedback.
 * <p/>
 * If the Feedback is not empty and not equal to
 * <code>Feedback.useDefault()</code>, the values are considered non matching.
 * The result should contain the necessary information for such a case (a name
 * of the property, an expected value or condition and the actual value).
 */
public interface Matcher<V> {

    FeedbackNode match(String property, V expected, V value);

}
