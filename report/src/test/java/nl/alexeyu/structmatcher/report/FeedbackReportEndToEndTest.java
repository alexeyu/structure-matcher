package nl.alexeyu.structmatcher.report;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.HashSet;

import org.junit.Test;

import nl.alexeyu.structmatcher.matcher.ObjectMatcher;

/**
 * Exercises the report package against feedback produced by a real {@link ObjectMatcher} run, to
 * pin that the canonical paths line up with the live tree's property naming (simple field, list
 * element, nested structure field). Uses a small local model ({@link SampleStructure}) since core's
 * own test fixtures are not visible across the module boundary.
 */
public class FeedbackReportEndToEndTest {

    private final SampleStructure expected = new SampleStructure("black", asList("a"),
            new SampleSub(true));

    private final SampleStructure actual = new SampleStructure("white", asList("b"),
            new SampleSub(false));

    @Test
    public void canonicalPathsMatchALiveComparisonTree() {
        var feedback = ObjectMatcher.forClass(SampleStructure.class).match(expected, actual);
        assertEquals(new HashSet<>(asList("Color", "Tags[0]", "Sub.Flag")),
                new HashSet<>(FeedbackPaths.brokenPaths(feedback)));
    }

    @Test
    public void summaryAggregatesLiveComparisons() {
        var allDiffer = ObjectMatcher.forClass(SampleStructure.class).match(expected, actual);
        var onlyColorDiffers = ObjectMatcher.forClass(SampleStructure.class)
                .match(expected, new SampleStructure("white", asList("a"), new SampleSub(true)));
        var identical = ObjectMatcher.forClass(SampleStructure.class).match(expected, expected);

        var summary = FeedbackAggregator.summarize(asList(allDiffer, onlyColorDiffers, identical));
        assertEquals(3, summary.total());
        assertEquals(1, summary.matched());
        assertEquals(2, summary.failureCount("Color"));
        assertEquals(1, summary.failureCount("Tags[]"));
        assertEquals(1, summary.failureCount("Sub.Flag"));
        assertEquals("Color", summary.topMismatchingFields(1).get(0));
    }

}
