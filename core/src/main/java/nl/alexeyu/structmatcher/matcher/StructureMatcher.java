package nl.alexeyu.structmatcher.matcher;

import nl.alexeyu.structmatcher.Property;
import nl.alexeyu.structmatcher.feedback.CompositeFeedbackNode;
import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

final class StructureMatcher implements Matcher {
    
    @Override
    public FeedbackNode match(String property, Object expected, Object actual) {
        CompositeFeedbackNode feedback = Feedback.composite(property);
        Property.forClass(expected.getClass())
                .map(p -> matchProperty(p, expected, actual))
                .filter(f -> !f.isEmpty())
                .forEach(feedback::add);
        return feedback;
    }

    private FeedbackNode matchProperty(Property property, Object expected, Object actual) {
        return Matchers.contextAware(
                 Matchers.forProperty(property))
                    .match(property.getName(), 
                           property.getValue(expected),
                           property.getValue(actual));
    }
    
}
