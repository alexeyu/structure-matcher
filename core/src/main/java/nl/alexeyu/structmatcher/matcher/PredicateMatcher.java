package nl.alexeyu.structmatcher.matcher;

import java.util.function.Predicate;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

public final class PredicateMatcher implements Matcher {
    
    private final Predicate<Object> predicate;
    
    private final String specification;
    
    public PredicateMatcher(Predicate<Object> predicate, String description) {
        this.predicate = predicate;
        this.specification = description;
    }

    @Override
    public FeedbackNode match(String property, Object expected, Object actual) {
        if (!predicate.test(expected)) {
            throw new BrokenSpecificationException(property, expected, specification);
        }
        if (predicate.test(actual)) {
            return Feedback.empty(property);
        }
        return Feedback.nonEqual(property, specification, actual);
    }

}
