package nl.alexeyu.structmatcher.matcher;

import java.util.Optional;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Runs a matcher unchanged and withholds the values from the feedback it produced: the comparison
 * is the delegate's, so a masked field is matched as strictly as before and only reported
 * differently.
 * <p>
 * {@link Masking} does the withholding over the whole subtree, so masking a structure redacts
 * everything beneath it.
 *
 * @see Matcher#masking(Masker)
 */
final class MaskingMatcher<V> implements Matcher<V> {

    private final Matcher<V> delegate;

    private final Masker masker;

    MaskingMatcher(Matcher<V> delegate, Masker masker) {
        this.delegate = delegate;
        this.masker = masker;
    }

    @Override
    public Optional<Masker> masker() {
        return Optional.of(masker);
    }

    @Override
    public FeedbackNode match(String property, V expected, V actual) {
        return Masking.mask(delegate.match(property, expected, actual), masker);
    }

}
