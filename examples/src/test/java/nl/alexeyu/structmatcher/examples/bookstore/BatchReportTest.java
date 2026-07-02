package nl.alexeyu.structmatcher.examples.bookstore;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
 * The batch / "report is the product" scenario: instead of matching one pair, check many responses
 * against a baseline and roll the feedback up to see <em>which fields systematically diverge</em>.
 * Every comparison uses the shared {@link ContextTolerantSpec}, so the execution-context metadata
 * is filtered out and the report is about real book-payload divergence, not environment noise —
 * one v1 desktop baseline checked against a v2 mobile response (books genuinely changed), the v1
 * production XML (only context differs), and itself. Demonstrates the {@code report} module
 * ({@link FeedbackAggregator}, {@link FeedbackQuery}) and the {@code json} persistence format
 * ({@link FeedbackArchives}).
 */
public class BatchReportTest {

    private Path rootPath;

    private BookSearchResult desktopTest, desktopProd, mobileTest;

    @BeforeEach
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

    /** The baseline checked against each actual response, tolerating the context metadata. */
    private List<FeedbackNode> batch() {
        ObjectMatcher<BookSearchResult> matcher = ContextTolerantSpec.matcher();
        return List.of(matcher.match(desktopTest, mobileTest),
                matcher.match(desktopTest, desktopProd), matcher.match(desktopTest, desktopTest));
    }

    @Test
    public void aggregateRevealsSystematicallyDivergingFields() {
        FeedbackSummary summary = FeedbackAggregator.summarize(batch());

        assertEquals(3, summary.total());
        assertEquals(2, summary.matched());
        assertEquals(1, summary.mismatched());
        assertEquals(1.0 / 3.0, summary.mismatchRate(), 1e-9);

        // Only the mobile response diverges, and only in the book payload: abbreviated author
        // first names and the dropped per-book meta. The tolerated metadata never shows up.
        assertEquals(1, summary.failureCount("Books[].Authors[].FirstName"));
        assertEquals(1, summary.failureCount("Books[].Meta"));
        assertEquals(0, summary.failureCount("Metadata.Server.Ip"));

        // Most-failing first, ties broken by path — the real payload regressions bubble to the top.
        assertEquals(List.of("Books[].Authors[].FirstName", "Books[].Meta"),
                summary.topMismatchingFields(2));
    }

    @Test
    public void querySingleComparisonForTheLeavesUnderAPath() {
        var feedback = ContextTolerantSpec.matcher().match(desktopTest, mobileTest);

        // "What broke in the first book?" — the author's first name and the meta both changed.
        List<BrokenLeaf> underFirstBook = FeedbackQuery.mismatchesUnder(feedback, "Books[0]");
        assertEquals(List.of("Books[0].Authors[0].FirstName", "Books[0].Meta"),
                underFirstBook.stream().map(BrokenLeaf::path).collect(toList()));

        var firstName = underFirstBook.get(0);
        assertEquals("Stephen", firstName.expectation()); // baseline (expected)
        assertEquals("S.", firstName.value()); // mobile (actual)
    }

    @Test
    public void persistEachComparisonThenReloadAndAggregate() {
        var comparisons = batch();

        // Store each comparison as archive JSON (what you would write to disk / a DB).
        List<String> persisted =
                comparisons.stream().map(FeedbackArchives::toJson).collect(toList());

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
