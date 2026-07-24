package nl.alexeyu.structmatcher.examples.bookstore;

import static nl.alexeyu.structmatcher.junit5.StructAssertions.assertMatches;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * Shows the {@code junit5} assertion helpers in the bookstore scenario, without AssertJ. Using the
 * shared {@link ContextTolerantSpec}: the prod and mobile responses both match the baseline (one
 * differs only in execution context, the other only in presentation), while the regressed response
 * throws {@link AssertionFailedError} naming the two fields whose values genuinely changed - and
 * carrying both objects for the IDE's comparison view - with no tolerated field in sight. Same
 * fixtures as {@link ResponseMatchingTest}.
 */
public class JUnitAssertionExampleTest {

    private Path rootPath;

    private BookSearchResult desktopTest, desktopProd, mobileTest, mobileRegression;

    @BeforeEach
    public void setUp() throws Exception {
        rootPath = Paths.get(JUnitAssertionExampleTest.class.getResource("/").toURI())
                .resolve("../../../resources/test");
        var jsonMapper = new ObjectMapper();
        desktopTest = fromFile(jsonMapper, "response-on-smoke-for-desktop-test.json");
        mobileTest = fromFile(jsonMapper, "response-on-smoke-for-mobile-test.json");
        mobileRegression = fromFile(jsonMapper, "response-on-smoke-for-mobile-regression.json");
        desktopProd = fromFile(new XmlMapper(), "response-on-smoke-for-desktop-prod.xml");
    }

    private BookSearchResult fromFile(ObjectMapper mapper, String fileName) throws Exception {
        return mapper.readValue(rootPath.resolve(fileName).toFile(), BookSearchResult.class);
    }

    @Test
    public void prodIsEquivalentToTestUnderTolerantMetadata() {
        // baseline (expected) first, actual second - the same order as JUnit's assertEquals.
        assertMatches(desktopTest, desktopProd, ContextTolerantSpec.matcher());
    }

    @Test
    public void mobileIsEquivalentDespiteAbbreviatedPresentation() {
        // Initials instead of full first names, and no publishing details: the same answer, so
        // the assertion passes.
        assertMatches(desktopTest, mobileTest, ContextTolerantSpec.matcher());
    }

    @Test
    public void aGenuineRegressionThrowsAndTheMessageNamesTheField() {
        var error = assertThrows(AssertionFailedError.class,
                () -> assertMatches(desktopTest, mobileRegression, ContextTolerantSpec.matcher()));

        // Only the real divergence is reported: a changed title and a hit count that disagrees
        // with the books actually returned.
        var message = error.getMessage();
        assertTrue(message.contains("[Books[0].Title]"), message);
        assertTrue(message.contains("[Metadata.BooksFound]"), message);
        assertTrue(!message.contains("[Metadata.Server"), message); // tolerated context
        assertTrue(!message.contains("FirstName"), message); // tolerated abbreviation

        // Both responses ride along so a JUnit 5 IDE can render a comparison view.
        assertTrue(error.getExpected().getEphemeralValue() == desktopTest);
        assertTrue(error.getActual().getEphemeralValue() == mobileRegression);
    }

}
