package nl.alexeyu.structmatcher.matcher;

import java.util.ArrayList;
import java.util.List;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

public final class ListMatcher<V> implements Matcher<List<V>> {

    @Override
    public FeedbackNode match(String property, List<V> expectedList, List<V> actualList) {
        if (expectedList.size() != actualList.size()) {
            return Feedback.differentCollectionSizes(property, expectedList.size(),
                    actualList.size());
        }
        var feedbackSubnodes = new ArrayList<FeedbackNode>();
        for (int i = 0; i < actualList.size(); i++) {
            var actualElement = actualList.get(i);
            var expectedElement = expectedList.get(i);
            var elementProperty = String.format("%s[%s]", property, i);
            var elementMatcher = Matchers.getNullAwareMatcher(actualElement);
            var elementFeedback = elementMatcher.match(elementProperty, expectedElement,
                    actualElement);
            if (!elementFeedback.isEmpty()) {
                feedbackSubnodes.add(elementFeedback);
            }
        }
        return Feedback.composite(property, feedbackSubnodes);
    }

}
