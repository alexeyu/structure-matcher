package nl.alexeyu.structmatcher.feedback;

/**
 * A factory of feedback nodes used by matchers.
 */
public final class Feedback {

    /**
     * Produces an empty feedback about comparison two values of a property.
     * 
     * @param property
     *            property name.
     * @return an empty feedback which means that values are considered
     *         matching.
     */
    public static FeedbackNode empty(String property) {
        return new ExpecteationMet(property);
    }

    public static FeedbackNode nonEqual(String property, Object expected, Object actual) {
        return new ExpectationBroken(property, expected, actual);
    }

    /**
     * Produces a non-empty feedback node which means that an expectation
     * regarding certain property value is not fulfilled.
     * 
     * @param property
     *            property name.
     * @param value
     *            a value which tested.
     * @param specification
     *            an expectation regarding a value being tested (e.g. 'a
     *            positive number')
     * @return a feedback node which captures information about the broken
     *         expectation.
     */
    public static FeedbackNode doesNotConform(String property, Object value, String specification) {
        return new ExpectationBroken(property, specification, value);
    }

    /**
     * Produces a non-empty feedback node which means that a null value was
     * tested while a non-null value was expected.
     * 
     * @param property
     *            property name.
     * @param expected
     *            a value which was expected.
     * @return a feedback node which captures information about the broken
     *         expectation.
     */
    public static FeedbackNode gotNull(String property, Object expected) {
        return new ExpectationBroken(property, expected, null);
    }

    /**
     * Produces a non-empty feedback node which means that a non-null value was
     * tested while a null was expected.
     * 
     * @param property
     *            property name.
     * @param actual
     *            a value which was tested.
     * @return a feedback node which captures information about the broken
     *         expectation.
     */
    public static FeedbackNode gotNonNull(String property, Object actual) {
        return new ExpectationBroken(property, "null", actual);
    }

    /**
     * Produces an empty composite feedback node for a property. It is expected
     * that the other feedback nodes will be added to it.
     * 
     * @param property
     *            a property name.
     * @return an empty composite feedback node.
     */
    public static CompositeFeedbackNode composite(String property) {
        return new CompositeFeedbackNode(property);
    }

    /**
     * Produces a non-empty feedback node which means that the size of
     * collection is not as expected. Useful when comparing list properties.
     * 
     * @param expectedSize
     *            an expected size of a collection.
     * @param actual
     *            an actual collection size.
     * @return a feedback node which captures information about the broken
     *         expectation.
     */
    public static FeedbackNode differentCollectionSizes(String property, int expectedSize, int actualSize) {
        return new ExpectationBroken(property, "Size " + expectedSize, actualSize);
    }

}
