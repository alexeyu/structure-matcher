package nl.alexeyu.structmatcher.matcher;

import java.util.Optional;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

public class SimplePropertyMatcher implements Matcher {
    
    private final PartialMatcher nullAwareMatcher = new NullAwareMatcher();
    
    @Override
    public FeedbackNode match(String property, Object source, Object target) {
        Optional<FeedbackNode> feedback = nullAwareMatcher.maybeMatch(property, source, target);
        return feedback.orElseGet(() ->
            source.equals(target)
                ? Feedback.empty(property)
                : Feedback.nonEqual(property, source, target)); 
    }

}
