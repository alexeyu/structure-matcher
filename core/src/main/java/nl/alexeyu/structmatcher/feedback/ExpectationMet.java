package nl.alexeyu.structmatcher.feedback;

/**
 * Positive feedback: the property value met its expectation.
 */
record ExpectationMet(String property) implements FeedbackNode {

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public String getProperty() {
        return property;
    }

    @Override
    public String toString() {
        return property;
    }

}
