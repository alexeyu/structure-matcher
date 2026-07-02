package nl.alexeyu.structmatcher.assertj;

import static java.util.stream.Collectors.joining;

import org.assertj.core.api.AbstractAssert;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.matcher.ObjectMatcher;
import nl.alexeyu.structmatcher.report.BrokenLeaf;
import nl.alexeyu.structmatcher.report.FeedbackQuery;

/**
 * AssertJ assertion that an object is <em>equivalent enough</em> to an expected one under
 * structure-matcher's per-field rules. Obtain one via
 * {@link StructMatcherAssertions#assertThat(Object)}.
 *
 * <p>
 * The assertion runs a full {@link ObjectMatcher} comparison and, on any mismatch, fails with the
 * structured per-field diff — each broken {@link FeedbackQuery canonical path} with its expected
 * and actual value — so the failure says exactly which fields diverged, not merely that the two
 * objects differ.
 *
 * @param <T>
 *            the type under assertion.
 */
public class StructureAssert<T> extends AbstractAssert<StructureAssert<T>, T> {

    StructureAssert(T actual) {
        super(actual, StructureAssert.class);
    }

    /**
     * Asserts that {@code actual} matches {@code expected} using the default rules (plain per-field
     * equality, recursing into nested structures and collections). Use
     * {@link #matchesStructure(Object, ObjectMatcher)} to apply a configured, tolerant spec.
     *
     * @return {@code this} for chaining.
     */
    public StructureAssert<T> matchesStructure(T expected) {
        isNotNull();
        @SuppressWarnings("unchecked")
        var type = (Class<T>) actual.getClass();
        return matchesStructure(expected, ObjectMatcher.forClass(type));
    }

    /**
     * Asserts that {@code actual} matches {@code expected} using the given {@code spec} — an
     * {@link ObjectMatcher} you have configured with tolerant / cross-field rules. The spec's
     * registered matchers, wildcards and typed paths all apply.
     *
     * @return {@code this} for chaining.
     */
    public StructureAssert<T> matchesStructure(T expected, ObjectMatcher<T> spec) {
        isNotNull();
        var feedback = spec.match(expected, actual);
        if (!feedback.isEmpty()) {
            // Pass the built message as an argument, not as the format string: field values may
            // themselves contain '%'.
            failWithMessage("%s", describe(expected, feedback));
        }
        return this;
    }

    private String describe(T expected, FeedbackNode feedback) {
        var leaves = FeedbackQuery.brokenLeaves(feedback);
        var body = leaves.stream().map(StructureAssert::describeLeaf).collect(joining());
        return String.format(
                "%nExpecting:%n  <%s>%nto match the structure of:%n  <%s>%n"
                        + "but %d field(s) diverged:%s",
                actual, expected, leaves.size(), body);
    }

    private static String describeLeaf(BrokenLeaf leaf) {
        return String.format("%n  [%s] expected: <%s> but was: <%s>", leaf.path(),
                leaf.expectation(), leaf.value());
    }

}
