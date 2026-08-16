package nl.alexeyu.structmatcher.matcher;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import nl.alexeyu.structmatcher.feedback.Feedback;

/**
 * A property whose declared type leaves room for two unrelated runtime types. Reading the base
 * structure's accessors off such a value used to throw; the matcher now reports the clash.
 */
public class MismatchedTypeMatchingTest {

    private final Matcher<Object> matcher = Matchers.structuresEqual();

    @Test
    public void twoUnrelatedTypesAreReportedAsAMismatch() {
        var feedback = matcher.match("item", new Book("Carrie", 199), new Bookmark("Carrie"));
        assertEquals(Feedback.differentTypes("item", Book.class, Bookmark.class), feedback);
    }

    /** The leaf carries the property name, so a nested clash stays localized in the report. */
    @Test
    public void aNestedTypeClashIsReportedUnderItsProperty() {
        var feedback = ObjectMatcher.forClass(Shelf.class).match(new Shelf(new Book("Carrie", 199)),
                new Shelf(new Bookmark("Carrie")));
        assertEquals(
                Feedback.composite(Shelf.class.getName(),
                        asList(Feedback.differentTypes("Item", Book.class, Bookmark.class))),
                feedback);
    }

    /** Half an overlap: the actual structure carries the title and stops there. */
    @Test
    public void aPartiallyOverlappingTypeIsAMismatchToo() {
        var feedback = matcher.match("item", new Book("Carrie", 199), new Booklet("Carrie"));
        assertEquals(Feedback.differentTypes("item", Book.class, Booklet.class), feedback);
    }

    /**
     * The other way round: the actual structure carries the one base property plus a page count of
     * its own, and the comparison runs on the property they share.
     */
    @Test
    public void anActualStructureCarryingEveryBasePropertyIsCompared() {
        var feedback = matcher.match("item", new Booklet("Carrie"), new Book("Carrie", 199));
        assertTrue(feedback.isEmpty(), "the shared property matches: " + feedback);
    }

    /** A subtype is an instance of the base type, so every accessor fits and the fields compare. */
    @Test
    public void aSubtypeOfTheBaseTypeIsCompared() {
        var feedback = matcher.match("item", new Book("Carrie", 199),
                new SignedBook("Misery", 199, "King"));
        assertEquals(
                Feedback.composite("item", asList(Feedback.nonEqual("Title", "Carrie", "Misery"))),
                feedback);
    }

    /**
     * A property that kept its name and changed its shape. The default matcher comes from the base
     * declaration, so a list matcher used to meet a string and throw; all five shapes now report
     * the clash.
     */
    @ParameterizedTest(name = "{2} against {3}")
    @MethodSource("shapeClashes")
    public void aPropertyThatChangedShapeIsReportedAsAMismatch(Object expected, Object actual,
            Class<?> expectedValueType, Class<?> actualValueType) {
        var feedback = matcher.match("item", expected, actual);
        assertEquals(Feedback.composite("item",
                asList(Feedback.differentTypes("Tags", expectedValueType, actualValueType))),
                feedback);
    }

    static Stream<Arguments> shapeClashes() {
        return Stream.of(
                arguments(new ListOfTags(new ArrayList<>(List.of("crime"))), new OneTag("crime"),
                        ArrayList.class, String.class),
                arguments(new ArrayOfTags(new String[] {"crime"}), new OneTag("crime"),
                        String[].class, String.class),
                arguments(new MapOfTags(new HashMap<>(Map.of("genre", "crime"))),
                        new ListOfTags(new ArrayList<>(List.of("crime"))), HashMap.class,
                        ArrayList.class),
                arguments(new SetOfTags(new HashSet<>(Set.of("crime"))),
                        new ListOfTags(new ArrayList<>(List.of("crime"))), HashSet.class,
                        ArrayList.class),
                arguments(new OptionalTag(Optional.of("crime")), new OneTag("crime"),
                        Optional.class, String.class));
    }

    /** A custom matcher replaces the default, so it takes the raw values and no check runs. */
    @Test
    public void aCustomMatcherStillSeesAValueOfAnotherShape() {
        var feedback = ObjectMatcher.forClass(Object.class)
                .with(Matchers.constant("crime"), "Tags")
                .match(new ListOfTags(new ArrayList<>(List.of("crime"))), new OneTag("crime"));
        assertTrue(feedback.isEmpty(), "the custom matcher decides: " + feedback);
    }

    public static class Book {

        private final String title;

        private final int pages;

        public Book(String title, int pages) {
            this.title = title;
            this.pages = pages;
        }

        public String getTitle() {
            return title;
        }

        public int getPages() {
            return pages;
        }

    }

    public static final class SignedBook extends Book {

        private final String signature;

        public SignedBook(String title, int pages, String signature) {
            super(title, pages);
            this.signature = signature;
        }

        public String getSignature() {
            return signature;
        }

    }

    /** Carries the title of a book, but not its pages. */
    public static final class Booklet {

        private final String title;

        public Booklet(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

    }

    /** Shares no property with a book. */
    public record Bookmark(String owner) {
    }

    public record Shelf(Object item) {
    }

    /** Six models that name one property and hold it in a different shape, as v1 and v2 would. */
    public record ListOfTags(List<String> tags) {
    }

    public record ArrayOfTags(String[] tags) {
    }

    public record MapOfTags(Map<String, String> tags) {
    }

    public record SetOfTags(Set<String> tags) {
    }

    public record OptionalTag(Optional<String> tags) {
    }

    public record OneTag(String tags) {
    }

}
