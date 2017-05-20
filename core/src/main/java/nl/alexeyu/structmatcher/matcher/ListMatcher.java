package nl.alexeyu.structmatcher.matcher;

import java.util.List;

import nl.alexeyu.structmatcher.feedback.CompositeFeedbackNode;
import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

final class ListMatcher implements Matcher<List<?>> {
    
    @Override
    public FeedbackNode match(String property, List<?> expectedList, List<?> actualList) {
        if (expectedList.size() != actualList.size()) {
            return Feedback.differentCollectionSizes(property, expectedList.size(), actualList.size());
        }
        CompositeFeedbackNode feedback = Feedback.composite(property);
        for (int i = 0; i < actualList.size(); i++) {
            Object actualElement = actualList.get(i);
            Object expectedElement = expectedList.get(i);
            String elementProperty = property + "[" + i + "]";
            Matcher<Object> elementMatcher = Matchers.getNullAwareMatcher(actualElement);
            FeedbackNode childFeedback = elementMatcher.match(elementProperty, expectedElement, actualElement);
            if (!childFeedback.isEmpty()) {
                feedback.add(childFeedback);
            }
        }
        return feedback;        
    }

}
