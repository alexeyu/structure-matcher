package nl.alexeyu.structmatcher.examples.bookstore;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.json.FeedbackArchives;
import nl.alexeyu.structmatcher.matcher.ObjectMatcher;
import nl.alexeyu.structmatcher.report.BrokenLeaf;
import nl.alexeyu.structmatcher.report.FeedbackAggregator;
import nl.alexeyu.structmatcher.report.FeedbackQuery;
import nl.alexeyu.structmatcher.report.FeedbackSummary;

/**
 * The batch / "report is the product" scenario: instead of matching one pair, run many comparisons
 * against a baseline and roll the feedback up to see <em>which fields systematically diverge</em>.
 * Uses the same bookstore fixtures as {@link ResponseMatchingTest} — one v1 desktop baseline checked
 * against a v2 mobile response, the v1 production XML, and itself — with the default (raw
 * equivalence) rules so real divergence surfaces. Demonstrates the {@code report} module
 * ({@link FeedbackAggregator}, {@link FeedbackQuery}) and the {@code json} persistence format
 * ({@link FeedbackArchives}).
 */
public class BatchReportTest {

    private Path rootPath;

    private BookSearchResult desktopTest, desktopProd, mobileTest;

    @Before
    public void setUp() throws Exception {
        rootPath = Paths.get(BatchReportTest.class.getResource("/").toURI())
                .resolve("../../../resources/test");
        var jsonMapper = new ObjectMapper();
        desktopTest = fromFile(jsonMapper, "response-on-smoke-for-desktop-test.json");
        mobileTest = fromFile(jsonMapper, "response-on-smoke-for-mobile-test.json");
        desktopProd = fromFile(new XmlMapper(), "response-on-smoke-for-desktop-prod.xml");
    }

    private BookSearchResult fromFile(ObjectMapper mapper, String fileName) throws Exception {
        return mapper.readValue(rootPath.resolve(fileName).toFile(), BookSearchResult.class);
    }

    /** The baseline checked against each actual response in the batch (raw equivalence, no rules). */
    private List<FeedbackNode> batch() {
        var matcher = ObjectMatcher.forClass(BookSearchResult.class);
        return List.of(matcher.match(desktopTest, mobileTest),
                matcher.match(desktopTest, desktopProd), matcher.match(desktopTest, desktopTest));
    }

    @Test
    public void aggregateRevealsSystematicallyDivergingFields() {
        FeedbackSummary summary = FeedbackAggregator.summarize(batch());

        assertEquals(3, summary.total());
        assertEquals(1, summary.matched());
        assertEquals(2, summary.mismatched());

        // ProcessingTimeMs and the server IP differ in both non-identical responses; the port only
        // in the production XML; a field is counted at most once per comparison.
        assertEquals(2, summary.failureCount("Metadata.ProcessingTimeMs"));
        assertEquals(2, summary.failureCount("Metadata.Server.Ip"));
        assertEquals(1, summary.failureCount("Metadata.Server.Port"));
        assertEquals(2.0 / 3.0, summary.failureRate("Metadata.Server.Ip"), 1e-9);

        // Most-failing first, ties broken by path — the systematic divergences bubble to the top.
        assertEquals(List.of("Metadata.ProcessingTimeMs", "Metadata.Server.Ip"),
                summary.topMismatchingFields(2));
    }

    @Test
    public void querySingleComparisonForTheLeavesUnderAPath() {
        var feedback = ObjectMatcher.forClass(BookSearchResult.class).match(desktopTest, mobileTest);

        // "What broke under the server?" — the IP diverges, the port matches (both 8080).
        List<BrokenLeaf> underServer = FeedbackQuery.mismatchesUnder(feedback, "Metadata.Server");
        assertEquals(List.of("Metadata.Server.Ip"),
                underServer.stream().map(BrokenLeaf::path).collect(toList()));

        var ip = underServer.get(0);
        assertEquals("192.168.10.10", ip.expectation()); // baseline (expected)
        assertEquals("192.168.10.45", ip.value()); // mobile (actual)
    }

    @Test
    public void persistEachComparisonThenReloadAndAggregate() {
        var comparisons = batch();

        // Store each comparison as archive JSON (what you would write to disk / a DB).
        List<String> persisted = comparisons.stream().map(FeedbackArchives::toJson).collect(toList());

        // Later, in another process: reload and aggregate without the original feedback trees.
        var aggregator = new FeedbackAggregator();
        for (var json : persisted) {
            aggregator.addBrokenPaths(FeedbackArchives.fromJson(json).brokenPaths());
        }
        var reloaded = aggregator.summary();

        var inMemory = FeedbackAggregator.summarize(comparisons);
        assertEquals(inMemory.total(), reloaded.total());
        assertEquals(inMemory.matched(), reloaded.matched());
        assertEquals(inMemory.failuresByField(), reloaded.failuresByField());
        assertTrue(persisted.get(0).contains("\"schemaVersion\""));
    }

}
