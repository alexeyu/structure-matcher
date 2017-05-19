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

    private final Collection<FeedbackNode> children = new LinkedHashSet<>();

    private final String property;

    CompositeFeedbackNode(String property) {
        this.property = property;
    }

    @Override
    public String getProperty() {
        return property;
    }

    public CompositeFeedbackNode add(FeedbackNode node) {
        children.add(node);
        return this;
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
        return Objects.hash(children);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof CompositeFeedbackNode) {
            CompositeFeedbackNode other = (CompositeFeedbackNode) obj;
            return Objects.equals(this.children, other.children);
        }
        return false;
    }

    @Override
    public String toString() {
        return children.toString();
    }

}
