package nl.alexeyu.structmatcher.matcher;

import java.util.List;

import nl.alexeyu.structmatcher.feedback.CompositeFeedbackNode;
import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

public class ListMatcher implements Matcher {
    
    private final PartialMatcher nullAwareMatcher = new NullAwareMatcher();

    @Override
    public FeedbackNode match(String property, Object expected, Object actual) {
        List<?> expectedList = (List<?>) expected;
        List<?> actualList = (List<?>) actual;
        if (expectedList.size() != actualList.size()) {
            return Feedback.differentCollectionSizes(property, expectedList.size(), actualList.size());
        }
        CompositeFeedbackNode feedback = Feedback.composite();
        for (int i = 0; i < actualList.size(); i++) {
            Object actualElement = actualList.get(i);
            Object expectedElement = expectedList.get(i);
            String elementProperty = property + "[" + i + "]";
            FeedbackNode childFeedback = nullAwareMatcher
                    .maybeMatch(property, expectedElement, actualElement)
                    .orElseGet(() -> Matchers.getMatcher(actualElement.getClass())
                            .match(elementProperty, expectedElement, actualElement));
            if (!childFeedback.isEmpty()) {
                feedback.add(childFeedback);
            }
        }
        return feedback;        
    }

}