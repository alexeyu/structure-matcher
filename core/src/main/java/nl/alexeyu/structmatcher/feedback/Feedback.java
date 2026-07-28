package nl.alexeyu.structmatcher.feedback;

import java.util.Collection;

/**
 * Factory of feedback nodes used by matchers.
 */
public final class Feedback {

    private Feedback() {
    }

    /**
     * Reports that the two values of a property match.
     *
     * @param property
     *            the property name.
     */
    public static FeedbackNode empty(String property) {
        return new ExpectationMet(property);
    }

    /**
     * Reports that the two values of a property differ.
     *
     * @param property
     *            the property name.
     * @param expected
     *            the value the base structure holds.
     * @param actual
     *            the value found instead.
     */
    public static FeedbackNode nonEqual(String property, Object expected, Object actual) {
        return new ExpectationBroken(property, expected, actual);
    }

    /**
     * Reports a value that breaks a stated expectation.
     *
     * @param property
     *            the property name.
     * @param value
     *            the value under test.
     * @param specification
     *            what the value should have satisfied, e.g. 'a positive number'.
     */
    public static FeedbackNode doesNotConform(String property, Object value, String specification) {
        return new ExpectationBroken(property, specification, value);
    }

    /**
     * Reports a null value where the base structure holds one.
     *
     * @param property
     *            the property name.
     * @param expected
     *            the value the base structure holds.
     */
    public static FeedbackNode gotNull(String property, Object expected) {
        return new ExpectationBroken(property, expected, null);
    }

    /**
     * Reports a present value where the base structure holds null.
     *
     * @param property
     *            the property name.
     * @param actual
     *            the value found instead.
     */
    public static FeedbackNode gotNonNull(String property, Object actual) {
        return new ExpectationBroken(property, "null", actual);
    }

    /**
     * Groups the feedback about a property's sub-properties. The node counts as empty only when
     * every child is empty; otherwise walk the children to find which sub-properties diverged and
     * why.
     *
     * @param property
     *            the property name.
     * @param children
     *            the feedback from matching its sub-properties.
     */
    public static CompositeFeedbackNode composite(String property,
            Collection<FeedbackNode> children) {
        return new CompositeFeedbackNode(property, children);
    }

    /**
     * Reports two collections of different size, which the list and array matchers treat as a
     * mismatch on the spot.
     *
     * @param property
     *            the property name.
     * @param expectedSize
     *            the size of the base collection.
     * @param actualSize
     *            the size found instead.
     */
    public static FeedbackNode differentCollectionSizes(String property, int expectedSize,
            int actualSize) {
        return new ExpectationBroken(property, "Size " + expectedSize, actualSize);
    }

}
