package nl.alexeyu.structmatcher.examples.bookstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.report.FeedbackPaths;

/**
 * Proves the rules {@link ContextTolerantSpec} applies to the two metadata fields that carry more
 * than execution context: {@code keywords}, a list standing for a set, where order and case fall
 * away but the terms have to match, and {@code booksFound}, which stays strict. Each response is
 * hand-built to vary the field under test alone, so any feedback belongs to that field.
 */
public class MetadataFieldRulesTest {

    private final BookSearchResult baseline = response(List.of("smoke"), 2);

    /** A response differing from {@link #baseline} in its keywords, its booksFound, or both. */
    private BookSearchResult response(List<String> keywords, int booksFound) {
        var metadata = new SearchMetadata(keywords, booksFound, 12,
                new Server("192.168.10.10", 8080), Platform.DESKTOP);
        var books = List.of(
                new Book("Blood and Smoke", List.of(new Author("Stephen", "King")), "1999", null));
        return new BookSearchResult(metadata, books);
    }

    private FeedbackNode matchAgainstBaseline(BookSearchResult actual) {
        return ContextTolerantSpec.matcher().match(baseline, actual);
    }

    @Test
    public void keywordOrderIsIgnored() {
        var base = response(List.of("smoke", "fire"), 2);
        var reordered = response(List.of("fire", "smoke"), 2);
        assertTrue(ContextTolerantSpec.matcher().match(base, reordered).isEmpty());
    }

    @Test
    public void keywordCaseIsTolerated() {
        assertTrue(matchAgainstBaseline(response(List.of("SMOKE"), 2)).isEmpty());

        // Order and case tolerated together.
        var base = response(List.of("Smoke", "Fire"), 2);
        var actual = response(List.of("fire", "smoke"), 2);
        assertTrue(ContextTolerantSpec.matcher().match(base, actual).isEmpty());
    }

    @Test
    public void anAddedKeywordIsAMismatch() {
        var feedback = matchAgainstBaseline(response(List.of("smoke", "fire"), 2));
        assertFalse(feedback.isEmpty());
        assertTrue(FeedbackPaths.brokenPaths(feedback).stream()
                .allMatch(path -> path.startsWith("Metadata.Keywords")));
    }

    @Test
    public void aDifferentKeywordIsAMismatch() {
        var feedback = matchAgainstBaseline(response(List.of("blaze"), 2));
        assertFalse(feedback.isEmpty());
        assertTrue(FeedbackPaths.brokenPaths(feedback).stream()
                .allMatch(path -> path.startsWith("Metadata.Keywords")));
    }

    @Test
    public void booksFoundMustMatch() {
        var feedback = matchAgainstBaseline(response(List.of("smoke"), 3));
        assertFalse(feedback.isEmpty());
        assertEquals(List.of("Metadata.BooksFound"), FeedbackPaths.brokenPaths(feedback));
    }

}
