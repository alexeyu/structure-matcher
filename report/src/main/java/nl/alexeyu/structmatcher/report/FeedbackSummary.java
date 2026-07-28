package nl.alexeyu.structmatcher.report;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * An aggregate view over a batch of comparisons: how many ran, how many matched, and how often
 * each field broke. A "field" is a {@link FeedbackPaths#toFieldPath normalized} path, which groups
 * mismatches that differ only by collection index, map key or set element. Each field counts at
 * most once per comparison, so {@link #failureRate} reads as "the fraction of comparisons in which
 * this field broke".
 *
 * <p>
 * {@link FeedbackAggregator} builds it. {@link #failuresByField()}, and everything derived from it,
 * runs by descending failure count and then by path, putting the worst field first.
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

    /** How many comparisons matched fully, i.e. produced empty feedback. */
    public int matched() {
        return matched;
    }

    /** How many comparisons broke at least one expectation. */
    public int mismatched() {
        return total - matched;
    }

    /** The fraction of comparisons that broke somewhere, in {@code [0, 1]}. */
    public double mismatchRate() {
        return rate(mismatched());
    }

    /**
     * How many comparisons broke at each field, worst field first. Unmodifiable.
     */
    public Map<String, Integer> failuresByField() {
        return failuresByField;
    }

    /** How many comparisons broke at the given normalized field path, 0 if none did. */
    public int failureCount(String field) {
        return failuresByField.getOrDefault(field, 0);
    }

    /** The fraction of comparisons that broke at the given field, in {@code [0, 1]}. */
    public double failureRate(String field) {
        return rate(failureCount(field));
    }

    /** Per-field failure rates, worst field first. Unmodifiable. */
    public Map<String, Double> failureRatesByField() {
        var rates = new LinkedHashMap<String, Double>();
        failuresByField.forEach((field, count) -> rates.put(field, rate(count)));
        return Collections.unmodifiableMap(rates);
    }

    /** The {@code limit} fields that break most often, worst first. */
    public List<String> topMismatchingFields(int limit) {
        return failuresByField.keySet().stream().limit(limit).toList();
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
