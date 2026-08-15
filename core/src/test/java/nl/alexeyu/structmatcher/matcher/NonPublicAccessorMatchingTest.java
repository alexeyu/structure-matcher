package nl.alexeyu.structmatcher.matcher;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.property.InaccessibleAccessorException;
import nl.alexeyu.structmatcher.property.PropertyRefs;

/**
 * Models whose accessors the library cannot call where the model declared them. These classes sit
 * in {@code structmatcher.matcher} and {@code ClassProperty} calls from
 * {@code structmatcher.property}, so package-private here puts a model out of reach the way a
 * user's own model would be.
 */
public class NonPublicAccessorMatchingTest {

    /**
     * A public model inheriting a getter from a package-private base. The compiler puts the getter
     * on the subclass as a synthetic bridge, the one declaration anything can call, so filtering
     * bridges dropped the property and two books with different titles compared as equal.
     */
    @Test
    public void aGetterInheritedFromANonPublicBaseIsCompared() {
        var feedback = ObjectMatcher.forClass(Hardcover.class).match(new Hardcover("Carrie", 199),
                new Hardcover("Misery", 199));
        assertFalse(feedback.isEmpty(), "a differing title has to be reported");
        assertEquals(Feedback.composite(Hardcover.class.getName(),
                asList(Feedback.nonEqual("Title", "Carrie", "Misery"))), feedback);
    }

    @Test
    public void aGetterInheritedFromANonPublicBaseStillMatchesWhenEqual() {
        assertTrue(ObjectMatcher.forClass(Hardcover.class)
                .match(new Hardcover("Carrie", 199), new Hardcover("Carrie", 199)).isEmpty());
    }

    /**
     * The AutoValue and Immutables shape: a public type the user registers, a package-private
     * generated subclass at runtime. {@code StructureMatcher} introspects the runtime class, so
     * the accessor arrives declared on the subclass, and we call it through the public supertype.
     */
    @Test
    public void anAccessorOfANonPublicImplementationIsCalledThroughItsPublicSupertype() {
        Author expected = new GeneratedAuthor("King");
        Author actual = new GeneratedAuthor("Straub");
        var feedback = ObjectMatcher.forClass(Author.class).match(expected, actual);
        assertEquals(Feedback.composite(Author.class.getName(),
                asList(Feedback.nonEqual("Surname", "King", "Straub"))), feedback);
    }

    /** The same route for a package-private record behind a public interface. */
    @Test
    public void aNonPublicRecordIsComparedThroughTheInterfaceItImplements() {
        var feedback = ObjectMatcher.forClass(Publication.class).match(new Ebook("Carrie"),
                new Ebook("Misery"));
        assertEquals(Feedback.composite(Publication.class.getName(),
                asList(Feedback.nonEqual("Title", "Carrie", "Misery"))), feedback);
    }

    /**
     * Properties are discovered off the base structure, so its declarations meet the actual one.
     * The bridge belongs to {@code Paperback} alone: calling it on any other implementation of the
     * interface fails, so a bridged accessor is called through the interface instead.
     */
    @Test
    public void twoImplementationsAreComparedThroughTheInterfaceTheyShare() {
        var feedback = ObjectMatcher.forClass(Printed.class).match(new Paperback("Carrie"),
                new Audiobook("Misery"));
        assertEquals(Feedback.composite(Printed.class.getName(),
                asList(Feedback.nonEqual("Title", "Carrie", "Misery"))), feedback);
    }

    /** The same pair the other way round: an own declaration fits only its own implementation. */
    @Test
    public void anImplementationDeclaringItsOwnAccessorReadsTheOtherThroughItsOwn() {
        var feedback = ObjectMatcher.forClass(Printed.class).match(new Audiobook("Carrie"),
                new Paperback("Misery"));
        assertEquals(Feedback.composite(Printed.class.getName(),
                asList(Feedback.nonEqual("Title", "Carrie", "Misery"))), feedback);
    }

    /** Naming a property is not reading it, so an unreadable one can still be registered. */
    @Test
    public void anUnreadableAccessorStillNamesItsProperty() {
        assertEquals("Title", PropertyRefs.nameOf(Manuscript::title));
    }

    /** The remedy the exception suggests: claiming the enclosing path stops the recursion. */
    @Test
    public void aMatcherOnTheEnclosingPropertyKeepsAnUnreadableOneOutOfTheComparison() {
        var feedback = ObjectMatcher.forClass(Drawer.class)
                .with(Matchers.anyValue(), Drawer::getManuscript)
                .match(new Drawer(new Manuscript("Carrie")), new Drawer(new Manuscript("Misery")));
        assertTrue(feedback.isEmpty(), "the unreadable property is never reached");
    }

    /**
     * With no public declaration anywhere, the library cannot read the value. It names the
     * accessor and the remedy instead of letting a bare {@code IllegalAccessException} out.
     */
    @Test
    public void aModelWithNoPublicDeclarationIsRejectedByName() {
        var thrown = assertThrows(InaccessibleAccessorException.class,
                () -> ObjectMatcher.forClass(Manuscript.class).match(new Manuscript("Carrie"),
                        new Manuscript("Misery")));
        assertTrue(thrown.getMessage().contains(Manuscript.class.getName()), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("title()"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("custom matcher"), thrown.getMessage());
    }

    /** A base holding the shared fields, kept out of the public API. Ordinary layering. */
    abstract static class AbstractBook {

        private final String title;

        AbstractBook(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

    }

    public static final class Hardcover extends AbstractBook {

        private final int pages;

        public Hardcover(String title, int pages) {
            super(title);
            this.pages = pages;
        }

        public int getPages() {
            return pages;
        }

    }

    public abstract static class Author {

        public abstract String getSurname();

    }

    /** Stands in for the package-private {@code AutoValue_Author} the generator would write. */
    static final class GeneratedAuthor extends Author {

        private final String surname;

        GeneratedAuthor(String surname) {
            this.surname = surname;
        }

        @Override
        public String getSurname() {
            return surname;
        }

    }

    public interface Publication {

        String title();

    }

    record Ebook(String title) implements Publication {
    }

    /** Unpublished, in both senses: no public supertype declares the accessor. */
    record Manuscript(String title) {
    }

    public static final class Drawer {

        private final Manuscript manuscript;

        Drawer(Manuscript manuscript) {
            this.manuscript = manuscript;
        }

        public Manuscript getManuscript() {
            return manuscript;
        }

    }

    /** One interface, implemented two ways: the pair a comparison can be handed. */
    public interface Printed {

        String getTitle();

    }

    /** Meets the interface with the getter of its package-private base, hence with a bridge. */
    public static final class Paperback extends AbstractBook implements Printed {

        Paperback(String title) {
            super(title);
        }

    }

    public static final class Audiobook implements Printed {

        private final String title;

        Audiobook(String title) {
            this.title = title;
        }

        @Override
        public String getTitle() {
            return title;
        }

    }

}
