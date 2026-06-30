package nl.alexeyu.structmatcher.report;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

public class FeedbackAggregatorTest {

    private static FeedbackNode matched() {
        return Feedback.composite("com.X", singletonList(Feedback.empty("Color")));
    }

    private static FeedbackNode broken(String... properties) {
        var children = new java.util.ArrayList<FeedbackNode>();
        for (var property : properties) {
            children.add(Feedback.nonEqual(property, "expected", "actual"));
        }
        return Feedback.composite("com.X", children);
    }

    @Test
    public void countsMatchedAndMismatchedComparisons() {
        var summary = FeedbackAggregator
                .summarize(asList(matched(), broken("Color"), broken("Color", "Bool")));
        assertEquals(3, summary.total());
        assertEquals(1, summary.matched());
        assertEquals(2, summary.mismatched());
        assertEquals(2.0 / 3.0, summary.mismatchRate(), 1e-9);
    }

    @Test
    public void tracksPerFieldFailureCountsAndRates() {
        var summary = FeedbackAggregator.summarize(
                asList(matched(), broken("Color"), broken("Color", "Bool"), broken("Bool")));
        assertEquals(4, summary.total());
        assertEquals(2, summary.failureCount("Color"));
        assertEquals(2, summary.failureCount("Bool"));
        assertEquals(0, summary.failureCount("Missing"));
        assertEquals(0.5, summary.failureRate("Color"), 1e-9);
        assertEquals(0.0, summary.failureRate("Missing"), 1e-9);
    }

    @Test
    public void ordersFieldsByDescendingFailureCountThenByName() {
        var summary = FeedbackAggregator
                .summarize(asList(broken("Bool"), broken("Color"), broken("Color")));
        // Color failed twice, Bool once.
        assertEquals(asList("Color", "Bool"), summary.topMismatchingFields(10));
    }

    @Test
    public void countsAFieldOncePerComparisonEvenWithManyBrokenIndices() {
        var strings = Feedback.composite("Strings",
                asList(Feedback.nonEqual("Strings[0]", "a", "b"),
                        Feedback.nonEqual("Strings[1]", "c", "d")));
        var feedback = Feedback.composite("com.X", singletonList(strings));

        var summary = FeedbackAggregator.summarize(singletonList(feedback));
        assertEquals(1, summary.total());
        // Both broken indices collapse to one field, counted once for this single comparison.
        assertEquals(1, summary.failureCount("Strings[]"));
        assertEquals(1, summary.failuresByField().size());
    }

    @Test
    public void incrementalAddMatchesOneShotSummarize() {
        var aggregator = new FeedbackAggregator().add(matched()).add(broken("Color"));
        var summary = aggregator.summary();
        assertEquals(2, summary.total());
        assertEquals(1, summary.matched());
        assertEquals(1, summary.failureCount("Color"));
    }

    @Test
    public void aggregatesFromBrokenPathsLikeFromLiveTrees() {
        // The reload path: same batch described by stored canonical paths instead of FeedbackNodes.
        var summary = new FeedbackAggregator()
                .addBrokenPaths(emptyList())
                .addBrokenPaths(singletonList("Color"))
                .addBrokenPaths(asList("Color", "Sub.Bool"))
                .summary();
        assertEquals(3, summary.total());
        assertEquals(1, summary.matched());
        assertEquals(2, summary.failureCount("Color"));
        assertEquals(1, summary.failureCount("Sub.Bool"));
    }

    @Test
    public void brokenPathsCollapseCollectionIndicesToOneFieldPerComparison() {
        var summary = new FeedbackAggregator()
                .addBrokenPaths(asList("Strings[0]", "Strings[1]"))
                .summary();
        assertEquals(1, summary.total());
        assertEquals(1, summary.failureCount("Strings[]"));
        assertEquals(1, summary.failuresByField().size());
    }

    @Test
    public void emptyCollectionYieldsZeroesAndNoDivisionByZero() {
        var summary = FeedbackAggregator.summarize(emptyList());
        assertEquals(0, summary.total());
        assertEquals(0.0, summary.mismatchRate(), 1e-9);
        assertTrue(summary.failuresByField().isEmpty());
        assertTrue(summary.topMismatchingFields(5).isEmpty());
    }

}
