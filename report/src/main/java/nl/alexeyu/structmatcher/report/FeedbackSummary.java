package nl.alexeyu.structmatcher.report;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * An aggregate view over a corpus of comparisons: how many were run, how many matched, and — per
 * field — how often that field broke. A "field" is a {@link FeedbackPaths#toFieldPath normalized}
 * path, so mismatches that differ only by collection index, map key or set element are grouped
 * together. Each field is counted at most once per comparison, so {@link #failureRate} reads as
 * "the fraction of comparisons in which this field broke".
 *
 * <p>
 * Produced by {@link FeedbackAggregator}; {@link #failuresByField()} (and everything derived from
 * it) is ordered by descending failure count, then by path, so the most problematic fields come
 * first.
 */
public final class FeedbackSummary {

    private final int total;

    private final int matched;

    private final Map<String, Integer> failuresByField;

    FeedbackSummary(int total, int matched, Map<String, Integer> failuresByField) {
        this.total = total;
        this.matched = matched;
        this.failuresByField = sortByCountDescending(failuresByField);
    }

    /** The number of comparisons aggregated. */
    public int total() {
        return total;
    }

    /** How many of the comparisons matched fully (empty feedback). */
    public int matched() {
        return matched;
    }

    /** How many of the comparisons had at least one broken expectation. */
    public int mismatched() {
        return total - matched;
    }

    /** The fraction of comparisons that did not fully match, in {@code [0, 1]}. */
    public double mismatchRate() {
        return rate(mismatched());
    }

    /**
     * Per-field failure counts (how many comparisons broke at each field), ordered most-failing
     * first. Unmodifiable.
     */
    public Map<String, Integer> failuresByField() {
        return failuresByField;
    }

    /** How many comparisons broke at the given normalized field path (0 if never). */
    public int failureCount(String field) {
        return failuresByField.getOrDefault(field, 0);
    }

    /** The fraction of comparisons that broke at the given field, in {@code [0, 1]}. */
    public double failureRate(String field) {
        return rate(failureCount(field));
    }

    /** Per-field failure rates, ordered most-failing first. Unmodifiable. */
    public Map<String, Double> failureRatesByField() {
        var rates = new LinkedHashMap<String, Double>();
        failuresByField.forEach((field, count) -> rates.put(field, rate(count)));
        return Collections.unmodifiableMap(rates);
    }

    /** The {@code limit} most frequently failing fields, most-failing first. */
    public List<String> topMismatchingFields(int limit) {
        return failuresByField.keySet().stream().limit(limit).collect(Collectors.toList());
    }

    private double rate(int count) {
        return total == 0 ? 0.0 : (double) count / total;
    }

    private static Map<String, Integer> sortByCountDescending(Map<String, Integer> counts) {
        var sorted = counts.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry<String, Integer>::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a,
                        LinkedHashMap::new));
        return Collections.unmodifiableMap(sorted);
    }

    @Override
    public String toString() {
        var report = new StringBuilder(String.format(
                "%d comparisons: %d matched, %d mismatched (%.1f%%)", total, matched, mismatched(),
                mismatchRate() * 100));
        failuresByField.forEach((field, count) -> report.append(String.format("%n  %s: %d (%.1f%%)",
                field, count, rate(count) * 100)));
        return report.toString();
    }

}
