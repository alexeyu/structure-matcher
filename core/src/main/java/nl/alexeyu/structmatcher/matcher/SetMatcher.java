package nl.alexeyu.structmatcher.matcher;

import java.util.ArrayList;
import java.util.Set;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Matches two sets by membership: they match when they contain the same elements,
 * regardless of order. An element present in the base set but not the target is
 * reported as missing, and an element present only in the target is reported as
 * extra; each is reported under the property name <code>property[element]</code>.
 * <p>
 * Elements are compared using their own <code>equals</code>/<code>hashCode</code>,
 * which is how a {@link Set} determines membership in the first place. Unlike
 * structures and maps, a set's elements are therefore not matched field by field;
 * value types (records, primitives wrappers, strings, enums) are the natural set
 * elements. The set itself must not be <code>null</code>.
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
                feedbackSubnodes.add(Feedback.gotNonNull(elementProperty(property, element), element));
            }
        }
        return Feedback.composite(property, feedbackSubnodes);
    }

    private String elementProperty(String property, V element) {
        return String.format("%s[%s]", property, element);
    }

}
