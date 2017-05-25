package nl.alexeyu.structmatcher.feedback;

/**
 * Feedback about a value of a property. The value gets verified with certain
 * expectation about it (e.g. it should be equal to some base value). A node is
 * empty if this expectation was fulfilled and is non-empty otherwise.
 */
public interface FeedbackNode {

    String getProperty();

    boolean isEmpty();

}
