package nl.alexeyu.structmatcher.matcher;

import java.util.Optional;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

public final class NullAwareMatcher implements PartialMatcher {

    @Override
    public Optional<FeedbackNode> maybeMatch(String property, Object expected, Object actual) {
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
