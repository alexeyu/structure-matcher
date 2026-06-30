package nl.alexeyu.structmatcher.json;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Test;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.matcher.ObjectMatcher;
import nl.alexeyu.structmatcher.report.FeedbackAggregator;

/**
 * Closes the persist-reload-aggregate loop across the module boundary: run comparisons, persist each as archive JSON,
 * reload, and aggregate the reloaded archives into the same summary the live trees would have
 * produced — without ever rebuilding a {@link FeedbackNode}. The bridge is
 * {@code FeedbackArchive.brokenPaths()} feeding {@link FeedbackAggregator#addBrokenPaths}.
 */
public class ArchiveReloadAggregateTest {

    private final SampleStructure expected = new SampleStructure("black", asList("a"),
            new SampleSub(true));

    private FeedbackNode compare(SampleStructure actual) {
        return ObjectMatcher.forClass(SampleStructure.class).match(expected, actual);
    }

    @Test
    public void persistedComparisonsReloadIntoTheSameSummary() {
        var allDiffer = compare(new SampleStructure("white", asList("b"), new SampleSub(false)));
        var onlyColorDiffers = compare(new SampleStructure("white", asList("a"), new SampleSub(true)));
        var identical = compare(expected);

        // Persist to JSON (what would be written to disk/DB), then read back.
        var persisted = new ArrayList<String>();
        for (var feedback : asList(allDiffer, onlyColorDiffers, identical)) {
            persisted.add(FeedbackArchives.toJson(feedback));
        }

        var aggregator = new FeedbackAggregator();
        for (var json : persisted) {
            aggregator.addBrokenPaths(FeedbackArchives.fromJson(json).brokenPaths());
        }
        var summary = aggregator.summary();

        assertEquals(3, summary.total());
        assertEquals(1, summary.matched());
        assertEquals(2, summary.failureCount("Color"));
        assertEquals(1, summary.failureCount("Tags[]"));
        assertEquals(1, summary.failureCount("Sub.Flag"));
        assertEquals("Color", summary.topMismatchingFields(1).get(0));
    }

}
