package nl.alexeyu.structmatcher.matcher;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

public class ExpectAnyValueMatcher implements Matcher {
    
    private final PartialMatcher nullAwareMatcher = new NullAwareMatcher();
    
    @Override
    public FeedbackNode match(String property, Object expected, Object actual) {
        return nullAwareMatcher
                .maybeMatch(property, expected, actual)
                .orElseGet(() -> Feedback.empty(property));
    }

}
