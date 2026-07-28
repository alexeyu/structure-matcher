package nl.alexeyu.structmatcher.matcher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Matches two lists whatever order they arrive in, by sorting both with the comparator it is built
 * with and matching the results pairwise. Duplicates survive the sort, which keeps this a list
 * matcher rather than a set one.
 */
public final class IgnoreOrderListMatcher<V> implements Matcher<List<V>> {

    private final Comparator<V> comparator;

    private final Matcher<List<V>> listMatcher = new ListMatcher<>();

    public IgnoreOrderListMatcher(Comparator<V> comparator) {
        this.comparator = comparator;
    }

    @Override
    public FeedbackNode match(String property, List<V> expectedList, List<V> actualList) {
        var expectedSorted = new ArrayList<V>(expectedList);
        expectedSorted.sort(comparator);
        var actualSorted = new ArrayList<V>(actualList);
        actualSorted.sort(comparator);
        return listMatcher.match(property, expectedSorted, actualSorted);
    }

}
