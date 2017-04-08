package nl.alexeyu.structmatcher.matcher;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

public final class IgnoreMatcher implements Matcher {

    @Override
    public FeedbackNode match(String property, Object expected, Object actual) {
        return Feedback.empty(property);
    }

}
