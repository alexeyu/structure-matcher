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
 * static {@link #summarize}. To aggregate a batch that was persisted and reloaded (where the live
 * tree is no longer in hand), feed each comparison's stored broken paths to {@link #addBrokenPaths}.
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
        return addBrokenPaths(FeedbackPaths.brokenPaths(feedback));
    }

    /**
     * Adds one comparison described directly by the canonical paths at which it broke, rather than a
     * live {@link FeedbackNode}. This is the reload path: feed it the paths persisted in a report
     * archive (e.g. {@code FeedbackArchive.brokenPaths()} from the {@code json} module) so a batch
     * stored to disk can be aggregated without rebuilding the feedback tree. An empty collection
     * counts as a fully matched comparison; otherwise the paths are normalized to
     * {@link FeedbackPaths#toFieldPath fields} and each field is tallied at most once. Returns
     * {@code this} for chaining.
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
