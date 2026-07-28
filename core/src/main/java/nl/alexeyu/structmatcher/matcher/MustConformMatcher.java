package nl.alexeyu.structmatcher.matcher;

import java.util.function.Predicate;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Applies one condition to the base and the target value. A target value that fails it yields
 * non-empty feedback. A base value that fails it throws instead: the reference side breaking the
 * rule means the spec is wrong or the reference structure has a bug, and neither is a data
 * mismatch worth reporting.
 */
final class MustConformMatcher<V> implements Matcher<V> {

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
