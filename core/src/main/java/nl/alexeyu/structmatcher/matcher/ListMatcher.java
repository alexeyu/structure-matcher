package nl.alexeyu.structmatcher.matcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

public final class ListMatcher<V> implements Matcher<List<V>> {
    
    @Override
    public FeedbackNode match(String property, List<V> expectedList, List<V> actualList) {
        if (expectedList.size() != actualList.size()) {
            return Feedback.differentCollectionSizes(property, expectedList.size(), actualList.size());
        }
        Collection<FeedbackNode> feedbackSubnodes = new ArrayList<>();
        for (int i = 0; i < actualList.size(); i++) {
            Object actualElement = actualList.get(i);
            Object expectedElement = expectedList.get(i);
            String elementProperty = String.format("%s[%s]", property, i);
            Matcher<Object> elementMatcher = Matchers.getNullAwareMatcher(actualElement);
            FeedbackNode elementFeedback = elementMatcher.match(elementProperty, expectedElement, actualElement);
            if (!elementFeedback.isEmpty()) {
                feedbackSubnodes.add(elementFeedback);
            }
        }
        return Feedback.composite(property, feedbackSubnodes);
    }

}
