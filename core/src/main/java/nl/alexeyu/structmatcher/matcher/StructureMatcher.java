package nl.alexeyu.structmatcher.matcher;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import nl.alexeyu.structmatcher.Property;
import nl.alexeyu.structmatcher.feedback.CompositeFeedbackNode;
import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

public final class StructureMatcher implements Matcher {
    
    public FeedbackNode match(String property, Object expected, Object actual) {
        return Matchers
                .nullAware()
                .maybeMatch(property, expected, actual)
                .orElseGet(() -> matchObjects(property, expected, actual));
    }

    private FeedbackNode matchObjects(String property, Object expected, Object actual) {
        CompositeFeedbackNode feedback = Feedback.composite();
        getProperties(expected.getClass())
                .map(p -> matchProperty(p, expected, actual))
                .filter(f -> !f.isEmpty())
                .forEach(feedback::add);
        return feedback;
    }
    
    private FeedbackNode matchProperty(Property property, Object expected, Object actual) {
        return Matchers.getMatcher(property)
                    .match(property.getName(), 
                           property.getValue(expected),
                           property.getValue(actual));
    }
    
    private Stream<Property> getProperties(Class<?> cl) {
        return Arrays.stream(cl.getMethods())
            .map(Property::of)
            .filter(Optional::isPresent)
            .map(Optional::get);
    }

}
