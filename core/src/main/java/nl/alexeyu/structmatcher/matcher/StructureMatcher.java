package nl.alexeyu.structmatcher.matcher;

import nl.alexeyu.structmatcher.Property;
import nl.alexeyu.structmatcher.feedback.CompositeFeedbackNode;
import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

final class StructureMatcher<V> implements Matcher<V> {
    
    @Override
    public FeedbackNode match(String property, V expected, V actual) {
        CompositeFeedbackNode feedback = Feedback.composite(property);
        Property.forClass(expected.getClass())
                .map(p -> matchProperty(p, expected, actual))
                .filter(f -> !f.isEmpty())
                .forEach(feedback::add);
        return feedback;
    }

    @SuppressWarnings("unchecked")
    private FeedbackNode matchProperty(Property property, Object expected, Object actual) {
        return Matchers.contextAware(
                 Matchers.forProperty(property))
                    .match(property.getName(), 
                           property.getValue(expected),
                           property.getValue(actual));
    }
    
}
