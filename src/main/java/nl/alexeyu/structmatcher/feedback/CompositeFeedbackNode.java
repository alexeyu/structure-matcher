package nl.alexeyu.structmatcher.feedback;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;

public final class CompositeFeedbackNode implements FeedbackNode {
    
    private final Collection<FeedbackNode> children = new LinkedHashSet<>();
    
    CompositeFeedbackNode() {
    }

    public CompositeFeedbackNode add(FeedbackNode node) {
        children.add(node);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return children.stream().allMatch(child -> child.isEmpty());
    }

    @Override
    public int hashCode() {
        return Objects.hash(children);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        CompositeFeedbackNode other = (CompositeFeedbackNode) obj;
        return Objects.equals(this.children, other.children);
    }

    @Override
    public String toString() {
        return "Feedback " + children.toString();
    }
    

}
