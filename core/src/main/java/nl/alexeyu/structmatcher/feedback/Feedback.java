package nl.alexeyu.structmatcher.feedback;

public final class Feedback {

    public static FeedbackNode empty(String property) {
        return new EmptyFeedbackNode(property);
    }

    public static FeedbackNode nonEqual(String property, Object expected, Object actual) {
        return new NonEqualPropertyFeedbackNode(property, expected, actual);
    }

    public static FeedbackNode gotNull(String property, Object expected) {
        return new NonEqualPropertyFeedbackNode(property, expected, null);
    }

    public static FeedbackNode gotNonNull(String property, Object actual) {
        return new NonEqualPropertyFeedbackNode(property, null, actual);
    }

    public static CompositeFeedbackNode composite() {
        return new CompositeFeedbackNode();
    }

    public static FeedbackNode differentCollectionSizes(String property, int size, int size2) {
        return new NonEqualPropertyFeedbackNode(property, size, size2);
    }

    public static FeedbackNode wrongType(String property, Class<?> expected, Class<?> actual) {
        return new NonEqualPropertyFeedbackNode(property, expected, actual);
    }

}
