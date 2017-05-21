package nl.alexeyu.structmatcher.feedback;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;

/**
 * Feedback node for a complex data structure (a list or a POJO). Contains other
 * feedback nodes which correspond to properties of an object or members of a
 * list. Empty iff all its children are empty.
 */
public final class CompositeFeedbackNode implements FeedbackNode {

    private final String property;

    private final Collection<FeedbackNode> children;

    CompositeFeedbackNode(String property, Collection<FeedbackNode> children) {
        this.property = property;
        this.children = new LinkedHashSet<>(children);
    }

    @Override
    public String getProperty() {
        return property;
    }

    @Override
    public boolean isEmpty() {
        return children.stream().allMatch(FeedbackNode::isEmpty);
    }

    public Collection<FeedbackNode> getChildren() {
        return Collections.unmodifiableCollection(children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(property, children);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof CompositeFeedbackNode) {
            CompositeFeedbackNode other = (CompositeFeedbackNode) obj;
            return Objects.equals(this.property, other.property) && 
                    Objects.equals(this.children, other.children);
        }
        return false;
    }

    @Override
    public String toString() {
        return property + ": " + children;
    }

}
