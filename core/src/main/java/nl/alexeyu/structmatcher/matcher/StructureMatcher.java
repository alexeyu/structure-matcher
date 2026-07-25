package nl.alexeyu.structmatcher.matcher;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.property.ClassProperty;

/**
 * Matches two data structures. Returns a feedback tree regarding all the sub-properties of the
 * structures. If the tree is empty, the structures are considered matching.
 * <p>
 * A type with no discoverable properties is compared by its own {@code equals}, or rejected with
 * {@link NoComparablePropertiesException} when it has none: empty feedback would mean "these
 * match".
 */
final class StructureMatcher<V> implements Matcher<V> {

    @Override
    public FeedbackNode match(String name, V expected, V actual) {
        var properties = ClassProperty.forClass(expected.getClass()).toList();
        if (properties.isEmpty()) {
            return matchWithoutProperties(name, expected, actual);
        }
        var feedbackSubnodes = properties.stream().map(p -> matchProperty(p, expected, actual))
                .filter(f -> !f.isEmpty()).toList();
        return Feedback.composite(name, feedbackSubnodes);
    }

    private FeedbackNode matchProperty(ClassProperty property, Object expected, Object actual) {
        return Matchers.contextAware(property, () -> Matchers.forProperty(property)).match(expected,
                actual);
    }

    private FeedbackNode matchWithoutProperties(String name, V expected, V actual) {
        if (!hasOwnEquals(expected.getClass())) {
            throw new NoComparablePropertiesException(name, expected.getClass());
        }
        return Matchers.<V>valuesEqual().match(name, expected, actual);
    }

    private static boolean hasOwnEquals(Class<?> cl) {
        try {
            return cl.getMethod("equals", Object.class).getDeclaringClass() != Object.class;
        } catch (NoSuchMethodException e) {
            // Unreachable: every class inherits Object.equals.
            return false;
        }
    }

}
