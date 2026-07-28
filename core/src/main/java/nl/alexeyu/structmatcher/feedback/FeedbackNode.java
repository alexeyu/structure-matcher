package nl.alexeyu.structmatcher.feedback;

/**
 * Feedback about one property value, held against an expectation such as "equal to the base value".
 * The node is empty when the value met that expectation and non-empty when it broke it.
 */
public sealed interface FeedbackNode
        permits CompositeFeedbackNode, ExpectationBroken, ExpectationMet {

    String getProperty();

    boolean isEmpty();

}
