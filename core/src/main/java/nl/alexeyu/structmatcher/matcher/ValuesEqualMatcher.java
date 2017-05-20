package nl.alexeyu.structmatcher.matcher;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

final class ValuesEqualMatcher<V> implements Matcher<V> {
    
    @Override
    public FeedbackNode match(String property, V expected, V actual) {
        return expected.equals(actual)
                ? Feedback.empty(property)
                : Feedback.nonEqual(property, expected, actual);
    }
    
}
