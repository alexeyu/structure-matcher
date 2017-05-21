package nl.alexeyu.structmatcher.matcher;

import java.util.Collection;
import java.util.stream.Collectors;

import nl.alexeyu.structmatcher.Property;
import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

final class StructureMatcher<V> implements Matcher<V> {
    
    @Override
    public FeedbackNode match(String property, V expected, V actual) {
        Collection<FeedbackNode> feedbackSubnodes = Property.forClass(expected.getClass())
                .map(p -> matchProperty(p, expected, actual))
                .filter(f -> !f.isEmpty())
                .collect(Collectors.toList());
        return Feedback.composite(property, feedbackSubnodes);
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
