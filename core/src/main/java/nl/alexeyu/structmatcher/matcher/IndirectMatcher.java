package nl.alexeyu.structmatcher.matcher;

import java.util.function.Function;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Derives the two values to compare from the whole base and target objects, through a fetcher
 * function each, and hands them to an underlying matcher. Use it when the expected value for one
 * field depends on a different part of the object.
 */
public final class IndirectMatcher<T, V> implements Matcher<T> {

    private final Matcher<V> valueMatcher;

    private final Function<T, V> expectedValueFetcher;

    private final Function<T, V> actualValueFetcher;

    private final String description;

    public IndirectMatcher(String description, Matcher<V> valueMatcher,
            Function<T, V> expectedValueFetcher, Function<T, V> actualValueFetcher) {
        this.valueMatcher = valueMatcher;
        this.expectedValueFetcher = expectedValueFetcher;
        this.actualValueFetcher = actualValueFetcher;
        this.description = description;
    }

    @Override
    public FeedbackNode match(String property, T expected, T actual) {
        return valueMatcher.match(property, expectedValueFetcher.apply(expected),
                actualValueFetcher.apply(actual));
    }

    /**
     * Applies this matcher to the top-level base/actual structures, deriving the values to compare
     * via the two fetchers. Called by {@link ContextAwareMatcher}, which sources the structures
     * from the matching stack. The cast holds by construction: you register an indirect matcher for
     * the structure type {@code T} it was built against, which is the type the matching stack
     * carries. Isolating the cast here keeps the caller, which holds only an
     * {@code IndirectMatcher<?, ?>}, free of raw types and unchecked operations.
     */
    @SuppressWarnings("unchecked")
    FeedbackNode matchStructures(Object baseStructure, Object actualStructure) {
        return match(description, (T) baseStructure, (T) actualStructure);
    }

    public String getDescription() {
        return description;
    }
}
