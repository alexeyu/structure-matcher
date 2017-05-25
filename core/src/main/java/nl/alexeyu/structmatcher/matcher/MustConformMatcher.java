package nl.alexeyu.structmatcher.matcher;

import java.util.function.Predicate;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Matcher that verifies that certain condition is fulfilled. It applies this
 * condition both to a value of a base and a target structure. It assumes the
 * base value absolutely must conform the condition, so, if it it doesn't, it
 * throws an exception, which effectively breaks the matching. It means either
 * that the contract is defined wrongly, or the base ("ethalon") structure has a
 * bug. If the target structure does not conform the condition, it returns a
 * non-empty feedback node.
 */
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
