package nl.alexeyu.structmatcher.matcher;

import java.util.function.Function;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;

public final class IndirectMatcher<T, V> implements Matcher<T> {

    private final Matcher<V> valueMatcher;

    private final Function<T, V> expectedValueFetcher;

    private final Function<T, V> actualValueFetcher;

    public IndirectMatcher(Matcher<V> valueMatcher, Function<T, V> expectedValueFetcher,
                           Function<T, V> actualValueFetcher) {
        this.valueMatcher = valueMatcher;
        this.expectedValueFetcher = expectedValueFetcher;
        this.actualValueFetcher = actualValueFetcher;
    }

    @Override
    public FeedbackNode match(String property, T expected, T actual) {
        return valueMatcher.match(property,
                expectedValueFetcher.apply(expected),
                actualValueFetcher.apply(actual));
    }

}
