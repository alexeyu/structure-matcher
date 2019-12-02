package nl.alexeyu.structmatcher.matcher;

import java.util.Arrays;
import java.util.List;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

final class AndMatcher<V> implements Matcher<V> {
    
    private final List<Matcher<V>> matchers;

    @SafeVarargs
    public AndMatcher(Matcher<V>... matchers) {
        this.matchers = Arrays.asList(matchers);
    }

    @Override
    public FeedbackNode match(String property, V expected, V value) {
        return matchers.stream()
            .map(m -> m.match(property, expected, value))
            .filter(feedback -> !feedback.isEmpty())
            .findFirst()
            .orElse(Feedback.empty(property));
    }

}
