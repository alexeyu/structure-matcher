package nl.alexeyu.structmatcher.matcher;

import java.util.Optional;

import nl.alexeyu.structmatcher.Context;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

class CustomizableMatcher<V> implements Matcher<V> {
    
    private final Matcher<V> defaultMatcher;
    
    private final Context context;
    
    public CustomizableMatcher(Context context, Matcher<V> defaultMatcher) {
        this.context = context;
        this.defaultMatcher = defaultMatcher;
    }

    @Override
    public FeedbackNode match(String property, V expected, V actual) {
        Optional<Matcher<V>> customMatcher = context.getCustomMatcher();
        Matcher<V> matcher = customMatcher.orElse(defaultMatcher);
        return matcher.match(property, expected, actual);
    }

}
