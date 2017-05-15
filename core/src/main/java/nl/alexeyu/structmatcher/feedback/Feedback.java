package nl.alexeyu.structmatcher.feedback;

public final class Feedback {

    public static FeedbackNode empty(String property) {
        return new EmptyFeedbackNode(property);
    }

    public static FeedbackNode nonEqual(String property, Object expected, Object actual) {
        return new BrokenExpectation(property, expected, actual);
    }

    public static FeedbackNode doesNotConform(String property, Object value, String specification) {
        return new BrokenExpectation(property, specification, value);
    }

    public static FeedbackNode gotNull(String property, Object expected) {
        return new BrokenExpectation(property, expected, null);
    }

    public static FeedbackNode gotNonNull(String property, Object actual) {
        return new BrokenExpectation(property, "null", actual);
    }

    public static CompositeFeedbackNode composite(String property) {
        return new CompositeFeedbackNode(property);
    }

    public static FeedbackNode differentCollectionSizes(String property, int expectedSize, int actualSize) {
        return new BrokenExpectation(property, "Size " + expectedSize, actualSize);
    }

    public static FeedbackNode wrongType(String property, Class<?> expected, Class<?> actual) {
        return new BrokenExpectation(property, expected, actual);
    }

}
