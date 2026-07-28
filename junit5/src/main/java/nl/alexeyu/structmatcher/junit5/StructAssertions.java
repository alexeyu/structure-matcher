package nl.alexeyu.structmatcher.junit5;

import static java.util.stream.Collectors.joining;

import org.opentest4j.AssertionFailedError;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.matcher.ObjectMatcher;
import nl.alexeyu.structmatcher.report.BrokenLeaf;
import nl.alexeyu.structmatcher.report.FeedbackQuery;

/**
 * Plain assertion helpers for using structure-matcher inside a JUnit test, without pulling in
 * AssertJ. Statically import {@link #assertMatches} and assert that two objects are
 * <em>equivalent enough</em> under per-field rules:
 *
 * <pre>
 * import static nl.alexeyu.structmatcher.junit5.StructAssertions.assertMatches;
 *
 * assertMatches(expectedResponse, actualResponse);
 *
 * // ...or with a configured spec (tolerant / cross-field rules):
 * var spec = ObjectMatcher.forClass(Response.class)
 *         .with(StringMatchers.url(), "*", "Url");
 * assertMatches(expectedResponse, actualResponse, spec);
 * </pre>
 *
 * On a mismatch the helpers throw {@link AssertionFailedError}, carrying the two objects as
 * expected and actual, so a JUnit 5 IDE offers its comparison view, plus a message listing every
 * broken field with its expected and actual value. {@code AssertionFailedError} comes from
 * opentest4j, JUnit 5's assertion foundation, and stays engine-agnostic, so these helpers work
 * under JUnit 5 or 4.
 */
public final class StructAssertions {

    private StructAssertions() {
    }

    /**
     * Asserts that {@code actual} matches {@code expected} under the default rules: per-field
     * equality, recursing into nested structures and collections. Pass a configured, tolerant spec
     * to {@link #assertMatches(Object, Object, ObjectMatcher)} instead when you need one.
     *
     * @throws AssertionFailedError
     *             if the two diverge on any field.
     */
    public static <T> void assertMatches(T expected, T actual) {
        var base = expected != null ? expected : actual;
        if (base == null) {
            return; // Both null: nothing to compare, so they are equivalent.
        }
        @SuppressWarnings("unchecked")
        var type = (Class<T>) base.getClass();
        assertMatches(expected, actual, ObjectMatcher.forClass(type));
    }

    /**
     * Asserts that {@code actual} matches {@code expected} under the given {@code spec}, an
     * {@link ObjectMatcher} you have configured with tolerant or cross-field rules. Its registered
     * matchers, wildcards and typed paths all apply.
     *
     * @throws AssertionFailedError
     *             if the two diverge on any field the spec does not tolerate.
     */
    public static <T> void assertMatches(T expected, T actual, ObjectMatcher<T> spec) {
        var feedback = spec.match(expected, actual);
        if (!feedback.isEmpty()) {
            throw new AssertionFailedError(describe(feedback), expected, actual);
        }
    }

    private static String describe(FeedbackNode feedback) {
        var leaves = FeedbackQuery.brokenLeaves(feedback);
        var body = leaves.stream().map(StructAssertions::describeLeaf).collect(joining());
        return String.format("%d field(s) did not match:%s", leaves.size(), body);
    }

    private static String describeLeaf(BrokenLeaf leaf) {
        return String.format("%n  [%s] expected: <%s> but was: <%s>", leaf.path(),
                leaf.expectation(), leaf.value());
    }

}
