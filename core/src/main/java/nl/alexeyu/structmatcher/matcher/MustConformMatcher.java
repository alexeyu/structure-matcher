package nl.alexeyu.structmatcher.matcher;

import java.util.function.Predicate;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

public final class MustConformMatcher<V> implements Matcher<V> {
    
    private final Predicate<V> condition;
    
    private final String specification;
    
    public MustConformMatcher(Predicate<V> predicate, String specification) {
        this.condition = predicate;
        this.specification = specification;
    }

    @Override
    public FeedbackNode match(String property, V baseValue, V testedValue) {
        if (!condition.test(baseValue)) {
            throw new BrokenSpecificationException(property, baseValue, specification);
        }
        if (condition.test(testedValue)) {
            return Feedback.empty(property);
        }
        return Feedback.doesNotConform(property, testedValue, specification);
    }

}
