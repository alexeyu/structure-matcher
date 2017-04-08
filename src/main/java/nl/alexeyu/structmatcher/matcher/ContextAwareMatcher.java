package nl.alexeyu.structmatcher.matcher;

import nl.alexeyu.structmatcher.Context;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

class ContextAwareMatcher implements Matcher {
    
    private final Matcher innerMatcher;
    
    private final Context context;
    
    public ContextAwareMatcher(Context context, Matcher innerMatcher) {
        this.innerMatcher = innerMatcher;
        this.context = context;
    }

    @Override
    public FeedbackNode match(String property, Object expected, Object actual) {
        try {
            context.push(property);
            Matcher matcher = context.getCustomMatcher().orElse(innerMatcher);
            return matcher.match(property, expected, actual);
        } finally {
            context.pop();
        }
    }

}
