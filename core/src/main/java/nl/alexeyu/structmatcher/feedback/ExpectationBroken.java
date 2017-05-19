package nl.alexeyu.structmatcher.feedback;

import java.util.Objects;

/**
 * Holds information about an expectation for a value of a property which was
 * not fulfilled.
 */
public final class ExpectationBroken implements FeedbackNode {

    private final String property;

    private final Object expectation;

    private final Object value;

    public ExpectationBroken(String property, Object expectation, Object value) {
        this.property = property;
        this.expectation = expectation;
        this.value = value;
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
    public int hashCode() {
        return Objects.hash(property, expectation, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ExpectationBroken) {
            ExpectationBroken other = (ExpectationBroken) obj;
            return Objects.equals(this.property, other.property) && Objects.equals(this.expectation, other.expectation)
                    && Objects.equals(this.value, other.value);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s: %s !~ %s", property, value, expectation);
    }

}
