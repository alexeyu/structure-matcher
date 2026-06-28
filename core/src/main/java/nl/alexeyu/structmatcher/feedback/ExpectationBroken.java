package nl.alexeyu.structmatcher.feedback;

/**
 * Holds information about an expectation for a value of a property which was not fulfilled.
 *
 * @param property
 *            name of a property which was verified.
 * @param expectation
 *            description of an expected value. Depends on the implementation of a matcher. For
 *            example 'Non-null', 'A positive integer', '42'.
 * @param value
 *            actual value which does not match the provided expectation.
 */
public record ExpectationBroken(String property, Object expectation,
        Object value) implements FeedbackNode {

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public String getProperty() {
        return property;
    }

    @Override
    public String toString() {
        return String.format("%s: %s !~ %s", property, value, expectation);
    }

}
