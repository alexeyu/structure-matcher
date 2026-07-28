package nl.alexeyu.structmatcher.matcher;

import java.util.function.Supplier;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.property.Property;

/**
 * Pushes the current property onto the matching stack, which hands back the matcher for the path
 * that results, then runs it. What it feeds that matcher depends on its kind:
 * <ul>
 * <li>an {@link IndirectMatcher} receives the top-level base and target structures, since it
 * derives the two values to compare itself;
 * <li>any other matcher, custom or default, receives the property name and the two values read off
 * this property.
 * </ul>
 */
final class ContextAwareMatcher {

    private final Supplier<Matcher<Object>> defaultMatcherSupplier;

    private final MatchingStack<Object> matchingStack;

    private final Property property;

    ContextAwareMatcher(Property property, MatchingStack<Object> matchingStack,
            Supplier<Matcher<Object>> defaultMatcherSupplier) {
        this.matchingStack = matchingStack;
        this.defaultMatcherSupplier = defaultMatcherSupplier;
        this.property = property;
    }

    FeedbackNode match(Object expected, Object actual) {
        try {
            var matcher = matchingStack.push(property.getName(), defaultMatcherSupplier);
            if (matcher instanceof IndirectMatcher<?, ?> indirectMatcher) {
                return indirectMatcher.matchStructures(matchingStack.getBaseStructure(),
                        matchingStack.getActualStructure());
            }
            return matcher.match(property.getName(), property.getValue(expected),
                    property.getValue(actual));
        } finally {
            matchingStack.pop();
        }
    }

}
