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
 * @param valueSlots
 *            which of the two slots above carry data from the compared objects; masking redacts
 *            those.
 */
public record ExpectationBroken(String property, Object expectation, Object value,
        ValueSlots valueSlots) implements FeedbackNode {

    /**
     * A leaf whose expectation states a condition rather than a value, so only the value slot
     * holds data. Build the other shapes through {@link Feedback}, which sets the slots to match.
     */
    public ExpectationBroken(String property, Object expectation, Object value) {
        this(property, expectation, value, ValueSlots.VALUE);
    }

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
