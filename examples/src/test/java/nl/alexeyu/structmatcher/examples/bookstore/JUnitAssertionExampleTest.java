package nl.alexeyu.structmatcher.examples.bookstore;

import static nl.alexeyu.structmatcher.junit5.StructAssertions.assertMatches;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;
import org.opentest4j.AssertionFailedError;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * Shows the {@code junit5} assertion helpers in the bookstore scenario, without AssertJ. Using the
 * shared {@link ContextTolerantSpec} (metadata tolerated, book payload strict): the prod response
 * matches the baseline because only its execution context differs, while the mobile response throws
 * {@link AssertionFailedError} — its book payload genuinely changed, so the message names the
 * offending {@code Books[...]} fields (and carries both objects for the IDE's comparison view),
 * with no tolerated metadata field in sight. Same fixtures as {@link ResponseMatchingTest}.
 */
public class JUnitAssertionExampleTest {

    private Path rootPath;

    private BookSearchResult desktopTest, desktopProd, mobileTest;

    @Before
    public void setUp() throws Exception {
        rootPath = Paths.get(JUnitAssertionExampleTest.class.getResource("/").toURI())
                .resolve("../../../resources/test");
        var jsonMapper = new ObjectMapper();
        desktopTest = fromFile(jsonMapper, "response-on-smoke-for-desktop-test.json");
        mobileTest = fromFile(jsonMapper, "response-on-smoke-for-mobile-test.json");
        desktopProd = fromFile(new XmlMapper(), "response-on-smoke-for-desktop-prod.xml");
    }

    private BookSearchResult fromFile(ObjectMapper mapper, String fileName) throws Exception {
        return mapper.readValue(rootPath.resolve(fileName).toFile(), BookSearchResult.class);
    }

    @Test
    public void prodIsEquivalentToTestUnderTolerantMetadata() {
        // baseline (expected) first, actual second — the same order as JUnit's assertEquals.
        assertMatches(desktopTest, desktopProd, ContextTolerantSpec.matcher());
    }

    @Test
    public void mobileFailsOnTheBookPayloadDespiteTolerantMetadata() {
        var error = assertThrows(AssertionFailedError.class,
                () -> assertMatches(desktopTest, mobileTest, ContextTolerantSpec.matcher()));

        // The metadata is tolerated; only the genuine book-payload changes are reported.
        var message = error.getMessage();
        assertTrue(message, message.contains("Books[0].Authors[0].FirstName"));
        assertTrue(message, message.contains("Books[0].Meta"));
        assertTrue(message, !message.contains("[Metadata")); // no tolerated metadata path

        // Both responses ride along so a JUnit 5 IDE can render a comparison view.
        assertTrue(error.getExpected().getEphemeralValue() == desktopTest);
        assertTrue(error.getActual().getEphemeralValue() == mobileTest);
    }

}
