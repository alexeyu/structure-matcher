package nl.alexeyu.structmatcher.matcher;

import java.util.Arrays;
import java.util.List;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

public class AndMatcher implements Matcher {
    
    private final List<Matcher> matchers;

    public AndMatcher(Matcher... matchers) {
        this.matchers = Arrays.asList(matchers);
    }

    @Override
    public FeedbackNode match(String property, Object expected, Object actual) {
        return matchers.stream()
            .map(m -> m.match(property, expected, actual))
            .filter(f -> !f.isEmpty())
            .findFirst()
            .orElse(Feedback.empty(property));
    }

}
