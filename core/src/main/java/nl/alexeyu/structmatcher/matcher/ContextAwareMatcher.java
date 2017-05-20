package nl.alexeyu.structmatcher.matcher;

import nl.alexeyu.structmatcher.Context;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

class ContextAwareMatcher<V> implements Matcher<V> {
    
    private final CustomizableMatcher<V> matcher;
    
    private final Context context;
    
    public ContextAwareMatcher(Context context, Matcher<V> defaultMatcher) {
        this.matcher = new CustomizableMatcher<>(context, defaultMatcher);
        this.context = context;
    }

    @Override
    public FeedbackNode match(String property, V expected, V actual) {
        try {
            context.push(property);
            return matcher.match(property, expected, actual);
        } finally {
            context.pop();
        }
    }

}
