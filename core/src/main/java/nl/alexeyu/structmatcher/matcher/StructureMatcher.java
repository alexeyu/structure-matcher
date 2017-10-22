package nl.alexeyu.structmatcher.matcher;

import java.util.Collection;
import java.util.stream.Collectors;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.property.Property;

/**
 * Matches two data structures. Returns a feedback tree regarding all the
 * sub-properties of the structures. If the tree is empty, the structures are
 * considered matching.
 */
final class StructureMatcher<V> implements Matcher<V> {
    
    @Override
    public FeedbackNode match(String name, V expected, V actual) {
        Collection<FeedbackNode> feedbackSubnodes = Property.forClass(expected.getClass())
                .map(p -> matchProperty(p, expected, actual))
                .filter(f -> !f.isEmpty())
                .collect(Collectors.toList());
        return Feedback.composite(name, feedbackSubnodes);
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
