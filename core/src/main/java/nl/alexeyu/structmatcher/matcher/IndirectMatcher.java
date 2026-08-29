package nl.alexeyu.structmatcher.matcher;

import java.util.Optional;
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
     * from the matching stack. The cast holds by construction: you register an indirect matcher
     * for the structure type {@code T} it was built against, which is the type the stack carries.
     * Keeping the cast here spares the caller, which holds an {@code IndirectMatcher<?, ?>}, the
     * raw types.
     */
    @SuppressWarnings("unchecked")
    FeedbackNode matchStructures(Object baseStructure, Object actualStructure) {
        return match(description, (T) baseStructure, (T) actualStructure);
    }

    /**
     * Masks the values this matcher derives, from the inside. {@link ContextAwareMatcher} picks an
     * indirect matcher out by its type and feeds it the whole structures, so a masking wrapper
     * <em>around</em> one would be handed a single property value and the fetchers would meet the
     * wrong type. The masker goes to the underlying value matcher, which builds the feedback.
     */
    @Override
    public IndirectMatcher<T, V> masking(Masker masker) {
        return new IndirectMatcher<>(description, valueMatcher.masking(masker),
                expectedValueFetcher, actualValueFetcher);
    }

    @Override
    public Optional<Masker> masker() {
        return valueMatcher.masker();
    }

    public String getDescription() {
        return description;
    }
}
