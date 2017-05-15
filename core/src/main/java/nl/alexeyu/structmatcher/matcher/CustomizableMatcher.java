package nl.alexeyu.structmatcher.matcher;

import nl.alexeyu.structmatcher.Context;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

class CustomizableMatcher implements Matcher {
    
    private final Matcher defaultMatcher;
    
    private final Context context;
    
    public CustomizableMatcher(Context context, Matcher defaultMatcher) {
        this.context = context;
        this.defaultMatcher = defaultMatcher;
    }

    @Override
    public FeedbackNode match(String property, Object expected, Object actual) {
        return context
                .getCustomMatcher()
                .orElse(defaultMatcher)
                .match(property, expected, actual);
    }

}
