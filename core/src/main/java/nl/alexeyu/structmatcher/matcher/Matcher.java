package nl.alexeyu.structmatcher.matcher;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Matches two values of a property and returns feedback about them. If the
 * feedback is empty, the properties are considered matching.
 */
public interface Matcher {

    FeedbackNode match(String property, Object expected, Object actual);

}
