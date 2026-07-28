package nl.alexeyu.structmatcher.report;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Accumulates the results of many comparisons into a {@link FeedbackSummary}. Feed it the
 * {@link FeedbackNode} each comparison returned through {@link #add} or {@link #addAll}, and call
 * {@link #summary()} for a snapshot at any point. The static {@link #summarize} does a collection
 * in one call. Once a batch has been persisted and reloaded and the live tree is gone, feed each
 * comparison's stored broken paths to {@link #addBrokenPaths} instead.
 *
 * <p>
 * A comparison contributes at most one tally per {@link FeedbackPaths#toFieldPath field}, however
 * many collection indices that field broke at, which keeps the rates per-comparison. The class
 * keeps mutable state, so aggregate from one thread, or give each thread its own aggregator and
 * sum the results offline.
 */
public final class FeedbackAggregator {

    private int total;

    private int matched;

    private final Map<String, Integer> failuresByField = new HashMap<>();

    /** Adds one comparison result to the running totals. Returns {@code this} for chaining. */
    public FeedbackAggregator add(FeedbackNode feedback) {
        return addBrokenPaths(FeedbackPaths.brokenPaths(feedback));
    }

    /**
     * Adds one comparison described by the canonical paths at which it broke, rather than by a
     * live {@link FeedbackNode}. This is the reload path: hand it the paths a report archive holds
     * ({@code FeedbackArchive.brokenPaths()} in the {@code json} module) and a batch stored to disk
     * rolls up without rebuilding a feedback tree. An empty collection counts as a fully matched
     * comparison. Otherwise the paths normalize to {@link FeedbackPaths#toFieldPath fields}, each
     * tallied once. Returns {@code this} for chaining.
     */
    public FeedbackAggregator addBrokenPaths(Collection<String> brokenPaths) {
        total++;
        if (brokenPaths.isEmpty()) {
            matched++;
            return this;
        }
        var brokenFields = brokenPaths.stream()
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
