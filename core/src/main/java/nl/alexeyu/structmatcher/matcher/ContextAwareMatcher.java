package nl.alexeyu.structmatcher.matcher;

import nl.alexeyu.structmatcher.Context;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

class ContextAwareMatcher implements Matcher {
    
    private final CustomizableMatcher matcher;
    
    private final Context context;
    
    public ContextAwareMatcher(Context context, Matcher defaultMatcher) {
        this.matcher = new CustomizableMatcher(context, defaultMatcher);
        this.context = context;
    }

    @Override
    public FeedbackNode match(String property, Object expected, Object actual) {
        try {
            context.push(property);
            return matcher.match(property, expected, actual);
        } finally {
            context.pop();
        }
    }

}
