package nl.alexeyu.structmatcher.matcher;

import java.util.ArrayList;
import java.util.Set;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Matches two sets by membership: they match when they hold the same elements, in any order. An
 * element only the base set holds counts as missing, one only the target holds counts as extra, and
 * each lands in the feedback under <code>property[element]</code>.
 * <p>
 * Elements meet through their own <code>equals</code>/<code>hashCode</code>, which is how a
 * {@link Set} decides membership to begin with, never field by field the way a structure's or a
 * map's do. That makes value types (records, strings, enums) the natural set elements. The set
 * itself may not be <code>null</code>.
 * <p>
 * The matcher lists the elements in {@link FeedbackOrder#CANONICAL} order, not the order the set
 * iterates in, so two equal sets give equal feedback.
 */
public final class SetMatcher<V> implements Matcher<Set<V>> {

    @Override
    public FeedbackNode match(String property, Set<V> expected, Set<V> actual) {
        var feedbackSubnodes = new ArrayList<FeedbackNode>();
        for (var element : expected) {
            if (!actual.contains(element)) {
                feedbackSubnodes.add(Feedback.gotNull(elementProperty(property, element), element));
            }
        }
        for (var element : actual) {
            if (!expected.contains(element)) {
                feedbackSubnodes
                        .add(Feedback.gotNonNull(elementProperty(property, element), element));
            }
        }
        feedbackSubnodes.sort(FeedbackOrder.CANONICAL);
        return Feedback.composite(property, feedbackSubnodes);
    }

    private String elementProperty(String property, V element) {
        return String.format("%s[%s]", property, element);
    }

}
