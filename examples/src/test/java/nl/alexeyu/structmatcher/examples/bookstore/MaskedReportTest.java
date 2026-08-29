package nl.alexeyu.structmatcher.examples.bookstore;

import static nl.alexeyu.structmatcher.junit5.StructAssertions.assertMatches;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.alexeyu.structmatcher.assertj.StructMatcherAssertions;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.json.FeedbackArchives;
import nl.alexeyu.structmatcher.report.FeedbackAggregator;

/**
 * The bookstore responses echo the address of the user who ran the search, and a comparison prints
 * the values it compares: into a feedback tree, an assertion message, a stored batch.
 * {@link ContextTolerantSpec#maskedMatcher()} masks that one field and leaves the rule on it
 * alone, so two responses still have to name the same requester. Same fixtures as
 * {@link ResponseMatchingTest}, plus a copy of the baseline attributed to a second user.
 */
public class MaskedReportTest {

    private static final String OTHER_USER = "carol@example.com";

    private Path rootPath;

    private BookSearchResult desktopTest, mobileTest, mobileRegression, anotherRequester;

    @BeforeEach
    public void setUp() throws Exception {
        rootPath = Paths.get(MaskedReportTest.class.getResource("/").toURI())
                .resolve("../../../resources/test");
        var jsonMapper = new ObjectMapper();
        desktopTest = fromFile(jsonMapper, "response-on-smoke-for-desktop-test.json");
        mobileTest = fromFile(jsonMapper, "response-on-smoke-for-mobile-test.json");
        mobileRegression = fromFile(jsonMapper, "response-on-smoke-for-mobile-regression.json");
        anotherRequester = requestedBy(desktopTest, OTHER_USER);
    }

    private BookSearchResult fromFile(ObjectMapper mapper, String fileName) throws Exception {
        return mapper.readValue(rootPath.resolve(fileName).toFile(), BookSearchResult.class);
    }

    /** The same response run by another user, which is the field the spec compares strictly. */
    private static BookSearchResult requestedBy(BookSearchResult response, String user) {
        var metadata = response.metadata();
        return new BookSearchResult(new SearchMetadata(metadata.keywords(), metadata.booksFound(),
                metadata.processingTimeMs(), metadata.server(), metadata.platform(), user),
                response.books());
    }

    @Test
    public void withoutTheMaskTheAddressLandsInTheFeedback() {
        // The field takes the default equality here, so the mismatch spells out both addresses.
        var unmasked = ContextTolerantSpec.matcher().match(desktopTest, anotherRequester);

        assertTrue(unmasked.toString().contains(OTHER_USER), unmasked.toString());
    }

    @Test
    public void theMaskedRequesterBreaksAtItsOwnPathAndWithholdsItsValues() {
        var feedback = ContextTolerantSpec.maskedMatcher().match(desktopTest, anotherRequester);

        // The field still breaks at its own path, as it did unmasked, and the two addresses reach
        // the archive as hashes.
        var archive = FeedbackArchives.archive(feedback);
        assertEquals(List.of("Metadata.RequestedBy"), archive.brokenPaths());
        var leaf = archive.brokenLeaves().get(0);
        assertTrue(leaf.value().toString().startsWith("sha256:"), leaf.toString());
        assertFalse(FeedbackArchives.write(archive).contains("@example.com"));
    }

    @Test
    public void aHashedFieldStillCountsAndGroupsAcrossABatch() {
        var matcher = ContextTolerantSpec.maskedMatcher();
        List<FeedbackNode> batch = List.of(matcher.match(desktopTest, mobileTest),
                matcher.match(desktopTest, anotherRequester),
                matcher.match(desktopTest, requestedBy(mobileRegression, OTHER_USER)));

        var summary = FeedbackAggregator.summarize(batch);
        assertEquals(2, summary.failureCount("Metadata.RequestedBy"));
        assertEquals(1, summary.failureCount("Books[].Title"));

        // Both comparisons hashed the same address to the same text, so a report can still tell
        // one diverging value from many while holding none of them.
        assertEquals(brokenLeaf(batch.get(1)), brokenLeaf(batch.get(2)));
    }

    private static String brokenLeaf(FeedbackNode feedback) {
        return FeedbackArchives.archive(feedback).brokenLeaves().stream()
                .filter(leaf -> leaf.path().equals("Metadata.RequestedBy"))
                .map(leaf -> String.valueOf(leaf.value())).findFirst().orElseThrow();
    }

    @Test
    public void neitherAssertionBridgeRendersTheObjectsHoldingAMaskedField() {
        var spec = ContextTolerantSpec.maskedMatcher();

        var junitFailure = assertThrows(AssertionFailedError.class,
                () -> assertMatches(desktopTest, anotherRequester, spec));
        assertTrue(junitFailure.getMessage().contains("[Metadata.RequestedBy]"));
        assertFalse(junitFailure.getMessage().contains("@example.com"));
        // The comparison view a JUnit 5 IDE offers prints the whole responses, addresses and all,
        // so a masking spec hands it neither object.
        assertNull(junitFailure.getExpected());
        assertNull(junitFailure.getActual());

        var assertJFailure = assertThrows(AssertionError.class, () -> StructMatcherAssertions
                .assertThat(anotherRequester).matchesStructure(desktopTest, spec));
        assertTrue(assertJFailure.getMessage().contains("[Metadata.RequestedBy]"));
        assertFalse(assertJFailure.getMessage().contains("@example.com"));
    }

    @Test
    public void aStoredBatchCanKeepTheFindingsAndNoneOfTheData() {
        var matcher = ContextTolerantSpec.matcher();
        var batch = List.of(matcher.match(desktopTest, anotherRequester),
                matcher.match(desktopTest, mobileRegression));

        // Where the values may not be stored at all, an archive without them keeps the paths, so
        // the batch still rolls up into a report. It masks the tree, so the spec stays plain.
        var archives = batch.stream().map(FeedbackArchives::archiveWithoutValues).toList();
        assertEquals(List.of("Metadata.RequestedBy"), archives.get(0).brokenPaths());
        assertEquals(List.of("Metadata.BooksFound", "Books[0].Title"),
                archives.get(1).brokenPaths());

        var jsonLines = FeedbackArchives.writeLines(archives);
        assertFalse(jsonLines.contains("@example.com"), jsonLines);
        assertFalse(jsonLines.contains("Blood"), jsonLines);
        assertEquals(2, FeedbackArchives.fromJsonLines(jsonLines).size());
    }

}
