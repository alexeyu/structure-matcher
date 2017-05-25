package nl.alexeyu.structmatcher.feedback;

final class UseDefaultFeedbackNode implements FeedbackNode {

    @Override
    public String getProperty() {
        throw new IllegalStateException("Should not be called.");
    }

    @Override
    public boolean isEmpty() {
        throw new IllegalStateException("Should not be called.");
    }

}
