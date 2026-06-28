package nl.alexeyu.structmatcher.feedback;

/**
 * Positive feedback, which implies an expectation about a property value was met.
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
