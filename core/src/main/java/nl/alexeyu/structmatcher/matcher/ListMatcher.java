package nl.alexeyu.structmatcher.matcher;

import java.util.List;

import nl.alexeyu.structmatcher.feedback.CompositeFeedbackNode;
import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

final class ListMatcher implements Matcher {
    
    @Override
    public FeedbackNode match(String property, Object expected, Object actual) {
        List<?> expectedList = List.class.cast(expected);
        List<?> actualList = List.class.cast(actual);
        if (expectedList.size() != actualList.size()) {
            return Feedback.differentCollectionSizes(property, expectedList.size(), actualList.size());
        }
        CompositeFeedbackNode feedback = Feedback.composite(property);
        for (int i = 0; i < actualList.size(); i++) {
            Object actualElement = actualList.get(i);
            Object expectedElement = expectedList.get(i);
            String elementProperty = property + "[" + i + "]";
            FeedbackNode childFeedback = Matchers.getNullAwareMatcher(actualElement)
                    .match(elementProperty, expectedElement, actualElement);
            if (!childFeedback.isEmpty()) {
                feedback.add(childFeedback);
            }
        }
        return feedback;        
    }

}
