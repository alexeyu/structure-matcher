package nl.alexeyu.structmatcher.matcher;

import java.util.Optional;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;

class ContextAwareMatcher<V> implements Matcher<V> {
    
    private final Matcher<V> defaultMatcher;
    
    private final Context context;
    
    public ContextAwareMatcher(Context context, Matcher<V> defaultMatcher) {
        this.context = context;
        this.defaultMatcher = defaultMatcher;
    }

    @Override
    public FeedbackNode match(String property, V expected, V actual) {
        try {
            Optional<Matcher<V>> customMatcher = context.push(property);
            Matcher<V> matcher = customMatcher.orElse(defaultMatcher);
            return matcher.match(property, expected, actual);
        } finally {
            context.pop();
        }
    }

}
