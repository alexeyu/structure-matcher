package nl.alexeyu.structmatcher.matcher;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

final class ValuesEqualMatcher implements Matcher {
    
    @Override
    public FeedbackNode match(String property, Object expected, Object actual) {
        return expected.equals(actual)
                ? Feedback.empty(property)
                : Feedback.nonEqual(property, expected, actual);
    }
    
}
