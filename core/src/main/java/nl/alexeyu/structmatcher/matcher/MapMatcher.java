package nl.alexeyu.structmatcher.matcher;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Matches two maps entry by entry: they match when they hold the same keys and matching values for
 * each. A key only the base map holds counts as a missing entry, a key only the target holds counts
 * as an extra one, and each lands in the feedback under <code>property[key]</code>, next to the
 * value mismatches. Values go through the usual logic, simple ones by equality and complex ones
 * structurally. Neither map may be <code>null</code>, though their values may.
 * <p>
 * The matcher lists the entries in {@link FeedbackOrder#CANONICAL} order, not the order the map
 * iterates in, so two equal maps give equal feedback.
 */
public final class MapMatcher<K, V> implements Matcher<Map<K, V>> {

    @Override
    public FeedbackNode match(String property, Map<K, V> expected, Map<K, V> actual) {
        var feedbackSubnodes = new ArrayList<FeedbackNode>();
        for (var entry : expected.entrySet()) {
            matchExpectedEntry(property, entry.getKey(), entry.getValue(), actual)
                    .ifPresent(feedbackSubnodes::add);
        }
        for (var entry : actual.entrySet()) {
            if (!expected.containsKey(entry.getKey())) {
                feedbackSubnodes.add(Feedback.gotNonNull(entryProperty(property, entry.getKey()),
                        entry.getValue()));
            }
        }
        feedbackSubnodes.sort(FeedbackOrder.CANONICAL);
        return Feedback.composite(property, feedbackSubnodes);
    }

    /** Feedback when the target lacks the key or holds another value, else an empty optional. */
    private Optional<FeedbackNode> matchExpectedEntry(String property, K key, V expectedValue,
            Map<K, V> actual) {
        var entryProperty = entryProperty(property, key);
        if (!actual.containsKey(key)) {
            return Optional.of(Feedback.gotNull(entryProperty, expectedValue));
        }
        var actualValue = actual.get(key);
        var feedback = Matchers.getNullAwareMatcher(actualValue).match(entryProperty, expectedValue,
                actualValue);
        return feedback.isEmpty() ? Optional.empty() : Optional.of(feedback);
    }

    private String entryProperty(String property, K key) {
        return String.format("%s[%s]", property, key);
    }

}
