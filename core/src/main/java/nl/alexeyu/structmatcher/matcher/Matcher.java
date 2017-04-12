package nl.alexeyu.structmatcher.matcher;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;

public interface Matcher {
    
    FeedbackNode match(String property, Object expected, Object actual);

}
