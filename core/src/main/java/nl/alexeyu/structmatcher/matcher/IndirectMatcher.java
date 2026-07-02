package nl.alexeyu.structmatcher.matcher;

import java.util.function.Function;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * A matcher that allows the maximal level of flexibility: operates with 2 functions that derive an
 * expected and an actual value from the base and target objects respectively. Then it feeds these
 * values to an underlying matcher.
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
     * from the matching stack. The cast is safe by construction: an indirect matcher is registered
     * for the structure type {@code T} it was built against, which is exactly the type the matching
     * stack carries — so isolating the one unavoidable unchecked cast here keeps the caller (which
     * only holds an {@code IndirectMatcher<?, ?>}) free of raw types and unchecked operations.
     */
    @SuppressWarnings("unchecked")
    FeedbackNode matchStructures(Object baseStructure, Object actualStructure) {
        return match(description, (T) baseStructure, (T) actualStructure);
    }

    public String getDescription() {
        return description;
    }
}
