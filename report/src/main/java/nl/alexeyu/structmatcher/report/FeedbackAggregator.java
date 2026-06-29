package nl.alexeyu.structmatcher.report;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Accumulates the results of many comparisons into a {@link FeedbackSummary}. Feed it the
 * {@link FeedbackNode} returned by each comparison via {@link #add} (or {@link #addAll}); call
 * {@link #summary()} for a snapshot at any point. For a one-shot summary of a collection use the
 * static {@link #summarize}.
 *
 * <p>
 * Each comparison contributes at most one tally per {@link FeedbackPaths#toFieldPath field}, even if
 * that field broke at several collection indices, so the resulting rates are per-comparison. Not
 * thread-safe; aggregate from a single thread (or one aggregator per thread, then sum offline).
 */
public final class FeedbackAggregator {

    private int total;

    private int matched;

    private final Map<String, Integer> failuresByField = new HashMap<>();

    /** Adds one comparison result to the running totals. Returns {@code this} for chaining. */
    public FeedbackAggregator add(FeedbackNode feedback) {
        total++;
        if (feedback.isEmpty()) {
            matched++;
            return this;
        }
        var brokenFields = FeedbackPaths.brokenPaths(feedback).stream()
                .map(FeedbackPaths::toFieldPath)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        brokenFields.forEach(field -> failuresByField.merge(field, 1, Integer::sum));
        return this;
    }

    /** Adds every comparison result in the collection. Returns {@code this} for chaining. */
    public FeedbackAggregator addAll(Collection<? extends FeedbackNode> feedbacks) {
        feedbacks.forEach(this::add);
        return this;
    }

    /** A snapshot summary of everything added so far. */
    public FeedbackSummary summary() {
        return new FeedbackSummary(total, matched, failuresByField);
    }

    /** Summarizes a collection of comparison results in one call. */
    public static FeedbackSummary summarize(Collection<? extends FeedbackNode> feedbacks) {
        return new FeedbackAggregator().addAll(feedbacks).summary();
    }

}
