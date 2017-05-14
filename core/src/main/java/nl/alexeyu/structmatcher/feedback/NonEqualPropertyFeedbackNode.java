package nl.alexeyu.structmatcher.feedback;

import java.util.Objects;

public final class NonEqualPropertyFeedbackNode implements FeedbackNode {

    private final String property;

    private final Object expected;

    private final Object actual;

    public NonEqualPropertyFeedbackNode(String property, Object expected, Object actual) {
        this.property = property;
        this.expected = expected;
        this.actual = actual;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
    
    public String getProperty() {
        return property;
    }

    @Override
    public int hashCode() {
        return Objects.hash(property, expected, actual);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NonEqualPropertyFeedbackNode) {
            NonEqualPropertyFeedbackNode other = (NonEqualPropertyFeedbackNode) obj;
            return Objects.equals(this.property, other.property)
                    && Objects.equals(this.expected, other.expected)
                    && Objects.equals(this.actual, other.actual);
        }
        return false;
    }

    @Override
    public String toString() {
        return property + ": " + actual + " != " + expected;
    }

}
