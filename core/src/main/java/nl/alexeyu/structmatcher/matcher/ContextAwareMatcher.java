package nl.alexeyu.structmatcher.matcher;

import java.util.Optional;
import java.util.function.Supplier;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.property.Property;

/**
 * A special matcher which gets a necessary matcher from the context and applies it.
 * The context is defined by the property it pushes to the stack. After pushing the
 * property, there are 3 possibilities:
 * <ol><li>An indirect matcher is defined for that property.</>
 * <ol><li>A custom matcher is defined for that property.</>
 * <ol><li>No matcher is defined for that property.</>
 * </ol>
 * The first case means, although the property gets verified, its actual and expected
 * values get taken from the highest level data structures, so context matcher passes
 * both base and target object to that matcher. In the second or third case, this matcher
 * derives the values of the property for the base and target objects and passes them to
 * an underlying matcher, along with the property name.
 */
final class ContextAwareMatcher<T> {

    private final Supplier<Matcher<Object>> defaultMatcherSupplier;

    private final MatchingStack<T> matchingStack;

    private final Property property;

    ContextAwareMatcher(Property property, MatchingStack<T> matchingStack,
                        Supplier<Matcher<Object>> defaultMatcherSupplier) {
        this.matchingStack = matchingStack;
        this.defaultMatcherSupplier = defaultMatcherSupplier;
        this.property = property;
    }

    FeedbackNode match(Object expected, Object actual) {
        try {
            Matcher<Object> matcher = matchingStack.push(property.getName(), defaultMatcherSupplier);
            if (matcher instanceof IndirectMatcher) {
                IndirectMatcher indirectMatcher = (IndirectMatcher) matcher;
                return indirectMatcher.match(indirectMatcher.getDescription(),
                        matchingStack.getBaseStructure(),
                        matchingStack.getActualStructure());
            }
            return matcher.match(property.getName(), property.getValue(expected), property.getValue(actual));
        } finally {
            matchingStack.pop();
        }
    }

}
