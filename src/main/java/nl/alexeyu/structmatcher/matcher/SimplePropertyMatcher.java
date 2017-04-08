package nl.alexeyu.structmatcher.matcher;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

public class SimplePropertyMatcher implements Matcher {
    
    private final PartialMatcher nullAwareMatcher = new NullAwareMatcher();
    
    @Override
    public FeedbackNode match(String property, Object expected, Object actual) {
        return nullAwareMatcher
                .maybeMatch(property, expected, actual)
                .orElseGet(() -> expected.equals(actual)
                                    ? Feedback.empty(property)
                                    : Feedback.nonEqual(property, expected, actual)); 
    }

}
