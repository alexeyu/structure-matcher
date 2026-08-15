package nl.alexeyu.structmatcher.matcher;

import java.util.Comparator;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Sorts the feedback about a map or a set, which iterate in an order of their own.
 * <p>
 * A composite node compares its children position by position, so the same entries filled in
 * another sequence give unequal feedback, and their archives stop diffing. Nodes sort by name, and
 * then by their own rendering, since two keys or elements can print alike.
 */
final class FeedbackOrder {

    private static final Comparator<FeedbackNode> BY_NAME = Comparator
            .comparing(FeedbackNode::getProperty);

    static final Comparator<FeedbackNode> CANONICAL = BY_NAME
            .thenComparing(FeedbackNode::toString);

    private FeedbackOrder() {
    }

}
