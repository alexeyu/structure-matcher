package nl.alexeyu.structmatcher.matcher;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Tests a value of a property against an expectation and returns feedback about
 * it. If the feedback is empty, the expectation is considered fulfilled.
 */
public interface Matcher {

    FeedbackNode match(String property, Object expectation, Object value);

}
