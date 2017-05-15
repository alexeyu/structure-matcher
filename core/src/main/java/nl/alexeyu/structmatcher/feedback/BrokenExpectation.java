package nl.alexeyu.structmatcher.feedback;

import java.util.Objects;

public final class BrokenExpectation implements FeedbackNode {

    private final String property;

    private final Object expectation;

    private final Object actual;

    public BrokenExpectation(String property, Object expectation, Object actual) {
        this.property = property;
        this.expectation = expectation;
        this.actual = actual;
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
        return Objects.hash(property, expectation, actual);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BrokenExpectation) {
            BrokenExpectation other = (BrokenExpectation) obj;
            return Objects.equals(this.property, other.property)
                    && Objects.equals(this.expectation, other.expectation)
                    && Objects.equals(this.actual, other.actual);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s: %s !~ %s", property, actual, expectation);
    }

}
