package nl.alexeyu.structmatcher.feedback;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;

public final class CompositeFeedbackNode implements FeedbackNode {

    private final Collection<FeedbackNode> children = new LinkedHashSet<>();
    
    private final String name;
    
    CompositeFeedbackNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public CompositeFeedbackNode add(FeedbackNode node) {
        children.add(node);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return children.stream().allMatch(child -> child.isEmpty());
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
        return "Feedback " + children.toString();
    }
    

}
