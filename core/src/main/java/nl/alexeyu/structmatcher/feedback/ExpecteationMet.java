package nl.alexeyu.structmatcher.feedback;

import java.util.Objects;

/**
 * Positive feedback, which implies an expectation about a property value was
 * met.
 */
final class ExpecteationMet implements FeedbackNode {

    private final String property;

    ExpecteationMet(String property) {
        this.property = property;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public String getProperty() {
        return property;
    }

    @Override
    public String toString() {
        return property;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(property);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ExpecteationMet) {
            ExpecteationMet other = (ExpecteationMet) obj;
            return Objects.equals(this.property, other.property);
        }
        return false;
    }

}
