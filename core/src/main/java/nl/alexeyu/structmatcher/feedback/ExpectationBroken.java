package nl.alexeyu.structmatcher.feedback;

/**
 * Records a property value that broke its expectation.
 *
 * @param property
 *            the property name.
 * @param expectation
 *            what the matcher expected, in its own words: 'Non-null', 'A positive integer', '42'.
 * @param value
 *            the value that broke it.
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
