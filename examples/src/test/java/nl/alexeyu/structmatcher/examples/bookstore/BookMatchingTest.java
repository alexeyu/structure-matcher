package nl.alexeyu.structmatcher.examples.bookstore;

import static nl.alexeyu.structmatcher.matcher.Matchers.valuesEqual;
import static org.apache.commons.lang3.StringUtils.stripAccents;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.matcher.Matchers;
import nl.alexeyu.structmatcher.matcher.ObjectMatcher;

public class BookMatchingTest {

    private final Author francoiseSagan = new Author("Françoise", "Sagan");
    private final Author francoiseSaganNormalized = new Author("Francoise", "Sagan");

    @Test
    public void notNormalizedStringsDoNotMatchByDefault() {
        var feedback = ObjectMatcher.forClass(Author.class).match(francoiseSaganNormalized,
                francoiseSagan);
        assertFalse(feedback.isEmpty());
    }

    @Test
    public void notNormalizedStringsDoMatchWithNormalizationAwareMatcher() {
        // Typed path: Author is a record, so the accessor is Author::firstName. A rename
        // of the component is now a compile error instead of a silently stale "FirstName".
        var feedback = ObjectMatcher.forClass(Author.class)
                .with(Matchers.<String>valuesEqual().normalizing(name -> stripAccents(name)),
                        Author::firstName)
                .match(francoiseSaganNormalized, francoiseSagan);
        assertTrue(feedback.isEmpty());
    }

    @Test
    public void structuralDifferencesCanBeMatchedByIndirectMatcher() {
        var oldVersion = new Book("Summer and Smoke",
                Arrays.asList(new Author("Tenessee", "Williams")),
                "Signet, 1961, Paperback, 127 pages", null);
        var newVersion = new Book("Summer and Smoke",
                Arrays.asList(new Author("Tenessee", "Williams")), "Paperback",
                new PublishingInfo("Signet", 1961, 127));

        var publishingMatcher = Matchers.indirectMatcher(
                "Old, unstructured metadata to new, structured one", valuesEqual(),
                new V1MetadataExtractor(), Book::publishingInfo);

        var feedback = ObjectMatcher.forClass(Book.class)
                // PublishingInfo is validated indirectly (below) from the old unstructured Meta,
                // so ignore the direct field comparison, where the old value is simply null.
                .withMatcher((p, e, a) -> Feedback.empty(p), "PublishingInfo")
                .withMatcher(publishingMatcher, "Meta").match(oldVersion, newVersion);
        assertTrue(feedback.isEmpty());
    }

    private static class V1MetadataExtractor implements Function<Book, PublishingInfo> {
        @Override
        public PublishingInfo apply(Book book) {
            var meta = book.meta().split(",");
            var pages = meta[3].trim();
            var numPages = Integer.valueOf(pages.split(" ")[0]);
            return new PublishingInfo(meta[0].trim(), Integer.valueOf(meta[1].trim()), numPages);
        }
    }

}
