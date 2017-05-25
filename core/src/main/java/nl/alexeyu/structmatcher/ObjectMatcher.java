package nl.alexeyu.structmatcher;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.matcher.Matcher;
import nl.alexeyu.structmatcher.matcher.Matchers;

public class ObjectMatcher {
    
    private final String objectName;
    
    private ObjectMatcher(String objectName) {
        this.objectName = objectName;
    }

    public static ObjectMatcher forObject(String name) {
        return new ObjectMatcher(name);
    }
    
    public ObjectMatcher withMatcher(Matcher<?> matcher, String... propertyPath) {
        String[] fullPath = new String[propertyPath.length + 1];
        System.arraycopy(propertyPath, 0, fullPath, 1, propertyPath.length);
        fullPath[0] = objectName;
        Matchers.registerCustomMatcher(matcher, fullPath);
        return this;
    }

    public ObjectMatcher with(Matcher<?> matcher, String propertyPath) {
        return withMatcher(matcher, propertyPath.split("\\."));
    }

    public FeedbackNode match(Object expected, Object actual) {
        return Matchers.contextAware(Matchers.structuresEqual()).match(objectName, expected, actual);
    }

}
