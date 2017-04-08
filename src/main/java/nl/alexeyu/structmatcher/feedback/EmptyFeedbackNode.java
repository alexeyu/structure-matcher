package nl.alexeyu.structmatcher.feedback;

import java.util.Objects;

final class EmptyFeedbackNode implements FeedbackNode {
    
    private final String property;
    
    EmptyFeedbackNode(String description) {
        this.property = description;
    }

    @Override
    public boolean isEmpty() {
        return true;
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
        if (obj instanceof EmptyFeedbackNode) {
            EmptyFeedbackNode other = (EmptyFeedbackNode) obj;
            return Objects.equals(this.property, other.property);
        }
        return false;
    }

}
