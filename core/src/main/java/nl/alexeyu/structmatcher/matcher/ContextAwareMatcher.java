package nl.alexeyu.structmatcher.matcher;

import java.util.Optional;

import nl.alexeyu.structmatcher.MatchingStack;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

class ContextAwareMatcher<V> implements Matcher<V> {
    
    private final Matcher<V> defaultMatcher;
    
    private final MatchingStack matchingStack;
    
    public ContextAwareMatcher(MatchingStack matchingStack, Matcher<V> defaultMatcher) {
        this.matchingStack = matchingStack;
        this.defaultMatcher = defaultMatcher;
    }

    @Override
    public FeedbackNode match(String property, V expected, V actual) {
        try {
            Optional<Matcher<V>> customMatcher = matchingStack.push(property);
            Matcher<V> matcher = customMatcher.orElse(defaultMatcher);
            return matcher.match(property, expected, actual);
        } finally {
            matchingStack.pop();
        }
    }

}
