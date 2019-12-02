package nl.alexeyu.structmatcher.matcher;

import java.util.function.Supplier;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.property.Property;

final class ContextAwareMatcher<T> implements Matcher<Object>{
    
    private final Supplier<Matcher<Object>> defaultMatcherSupplier;
    
    private final MatchingStack<T> matchingStack;

    private final Property property;

    public ContextAwareMatcher(Property property, MatchingStack<T> matchingStack,
                               Supplier<Matcher<Object>> defaultMatcherSupplier) {
        this.matchingStack = matchingStack;
        this.defaultMatcherSupplier = defaultMatcherSupplier;
        this.property = property;
    }

    public FeedbackNode match(String description, Object expected, Object actual) {
        try {
            Matcher<Object> customMatcher = matchingStack.push(property.getName(), defaultMatcherSupplier);
            if (isIndirect(customMatcher)) {
                return customMatcher.match(description,
                        matchingStack.getBaseStructure(),
                        matchingStack.getActualStructure());
            }
            return customMatcher.match(property.getName(), property.getValue(expected), property.getValue(actual));
        } finally {
            matchingStack.pop();
        }
    }

    private boolean isIndirect(Matcher matcher) {
        return matcher instanceof IndirectMatcher;
    }

}
