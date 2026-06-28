package nl.alexeyu.structmatcher.matcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Matches two maps entry by entry. Two maps match when they have the same keys
 * and, for every key, matching values. A key present in only one of the maps is
 * reported as a missing entry (present in the base, absent in the target) or an
 * extra entry (the other way around); every value mismatch is reported too. Each
 * entry is reported under the property name <code>property[key]</code>. Values are
 * matched the same way as anywhere else: simple values by equality, complex values
 * structurally. The maps themselves must not be <code>null</code> (their values
 * may be).
 */
public final class MapMatcher<K, V> implements Matcher<Map<K, V>> {

    @Override
    public FeedbackNode match(String property, Map<K, V> expected, Map<K, V> actual) {
        Collection<FeedbackNode> feedbackSubnodes = new ArrayList<>();
        for (Map.Entry<K, V> entry : expected.entrySet()) {
            matchExpectedEntry(property, entry.getKey(), entry.getValue(), actual).ifPresent(feedbackSubnodes::add);
        }
        for (Map.Entry<K, V> entry : actual.entrySet()) {
            if (!expected.containsKey(entry.getKey())) {
                feedbackSubnodes.add(Feedback.gotNonNull(entryProperty(property, entry.getKey()), entry.getValue()));
            }
        }
        return Feedback.composite(property, feedbackSubnodes);
    }

    /**
     * Matches a single entry of the base map against the target map, returning its
     * feedback if the entry is missing from the target or its value does not match,
     * or an empty optional if the values match.
     */
    private Optional<FeedbackNode> matchExpectedEntry(String property, K key, V expectedValue, Map<K, V> actual) {
        String entryProperty = entryProperty(property, key);
        if (!actual.containsKey(key)) {
            return Optional.of(Feedback.gotNull(entryProperty, expectedValue));
        }
        V actualValue = actual.get(key);
        FeedbackNode feedback = Matchers.getNullAwareMatcher(actualValue).match(entryProperty, expectedValue, actualValue);
        return feedback.isEmpty() ? Optional.empty() : Optional.of(feedback);
    }

    private String entryProperty(String property, K key) {
        return String.format("%s[%s]", property, key);
    }

}
