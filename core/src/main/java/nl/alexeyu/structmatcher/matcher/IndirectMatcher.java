package nl.alexeyu.structmatcher.matcher;

import java.util.function.Function;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * A matcher that allows the maximal level of flexibility: operates with 2 functions
 * that derive an expected and a actual values from the base and target objects respectively.
 * Then it feeds these values to an underlying matcher.
 */
public final class IndirectMatcher<T, V> implements Matcher<T> {

    private final Matcher<V> valueMatcher;

    private final Function<T, V> expectedValueFetcher;

    private final Function<T, V> actualValueFetcher;

    private final String description;

    public IndirectMatcher(String description,
                           Matcher<V> valueMatcher,
                           Function<T, V> expectedValueFetcher,
                           Function<T, V> actualValueFetcher) {
        this.valueMatcher = valueMatcher;
        this.expectedValueFetcher = expectedValueFetcher;
        this.actualValueFetcher = actualValueFetcher;
        this.description = description;
    }

    @Override
    public FeedbackNode match(String property, T expected, T actual) {
        return valueMatcher.match(property,
                expectedValueFetcher.apply(expected),
                actualValueFetcher.apply(actual));
    }

    public String getDescription() {
        return description;
    }
}
