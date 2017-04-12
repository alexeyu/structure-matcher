package nl.alexeyu.structmatcher.matcher;

import java.util.Optional;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;

public interface PartialMatcher {

    Optional<FeedbackNode> maybeMatch(String property, Object expected, Object actual);
    
}
