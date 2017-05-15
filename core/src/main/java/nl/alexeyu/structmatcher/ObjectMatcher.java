package nl.alexeyu.structmatcher;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
    
    public ObjectMatcher withMatcher(Matcher matcher, String... propertyPath) {
        List<String> path = new LinkedList<>(Arrays.asList(propertyPath));
        path.add(0, objectName);
        Matchers.registerCustomMatcher(path, matcher);
        return this;
    }

    public ObjectMatcher with(Matcher matcher, String propertyPath) {
        return withMatcher(matcher, propertyPath.split("\\."));
    }

    public FeedbackNode match(Object expected, Object actual) {
        return Matchers.structuresEqual().match(objectName, expected, actual);
    }

}
