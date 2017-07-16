package nl.alexeyu.structmatcher.matcher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * List matcher which ignores order of values. It is still not a set matcher
 * because it allows duplicate elements. It simply sorts given lists before
 * matching, so needs to be initialized with a comparator.
 */
final class IgnoreOrderListMatcher<V> implements Matcher<List<V>> {
    
    private final Comparator<V> comparator;
    
    private final Matcher<List<V>> listMatcher = new ListMatcher<>();
    
    public IgnoreOrderListMatcher(Comparator<V> comparator) {
        this.comparator = comparator;
    }

    @Override
    public FeedbackNode match(String property, List<V> expectedList, List<V> actualList) {
        List<V> expectedSorted = new ArrayList<>(expectedList);
        expectedSorted.sort(comparator);
        List<V> actualSorted = new ArrayList<>(actualList);
        actualSorted.sort(comparator);
        return listMatcher.match(property, expectedSorted, actualSorted);
    }

}
