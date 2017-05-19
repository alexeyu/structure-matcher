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
    public FeedbackNode match(String property, Object expectation, Object value) {
        return matchers.stream()
            .map(m -> m.match(property, expectation, value))
            .filter(f -> !f.isEmpty())
            .findFirst()
            .orElse(Feedback.empty(property));
    }

}
