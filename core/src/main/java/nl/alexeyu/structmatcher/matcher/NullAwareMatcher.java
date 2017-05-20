package nl.alexeyu.structmatcher.matcher;

import java.util.Optional;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

public final class NullAwareMatcher<V> implements Matcher<V> {
    
    private final Matcher<V> nextMatcher;
    
    public NullAwareMatcher(Matcher<V> nextMatcher) {
        this.nextMatcher = nextMatcher;
    }

    @Override
    public FeedbackNode match(String property, V expected, V actual) {
        return maybeMatch(property, expected, actual)
                .orElseGet(() -> nextMatcher.match(property, expected, actual));
    }

    private Optional<FeedbackNode> maybeMatch(String property, Object expected, Object actual) {
        if (expected == null && actual == null) {
            return Optional.of(Feedback.empty(property));
        }
        if (expected == null) {
            return Optional.of(Feedback.gotNonNull(property, actual));
        }
        if (actual == null) {
            return Optional.of(Feedback.gotNull(property, expected));
        }
        return Optional.empty();
    }

}
