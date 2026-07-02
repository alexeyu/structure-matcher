package nl.alexeyu.structmatcher.examples.bookstore;

import static nl.alexeyu.structmatcher.junit5.StructAssertions.assertMatches;
import static nl.alexeyu.structmatcher.matcher.IntegerMatchers.inRange;
import static nl.alexeyu.structmatcher.matcher.IntegerMatchers.oneOf;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;
import org.opentest4j.AssertionFailedError;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import nl.alexeyu.structmatcher.matcher.ObjectMatcher;
import nl.alexeyu.structmatcher.matcher.StringMatchers;

/**
 * Shows the {@code junit5} assertion helpers in the bookstore scenario, without AssertJ: assert
 * that a v1 production response is <em>equivalent enough</em> to the v1 test baseline under
 * tolerant per-field rules, and that a raw comparison throws {@link AssertionFailedError} naming
 * each diverging field (and carrying both objects for the IDE's comparison view). Same fixtures as
 * {@link ResponseMatchingTest}.
 */
public class JUnitAssertionExampleTest {

    private static final String IP_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    private BookSearchResult desktopTest, desktopProd;

    @Before
    public void setUp() throws Exception {
        Path rootPath = Paths.get(JUnitAssertionExampleTest.class.getResource("/").toURI())
                .resolve("../../../resources/test");
        desktopTest = new ObjectMapper().readValue(
                rootPath.resolve("response-on-smoke-for-desktop-test.json").toFile(),
                BookSearchResult.class);
        desktopProd = new XmlMapper().readValue(
                rootPath.resolve("response-on-smoke-for-desktop-prod.xml").toFile(),
                BookSearchResult.class);
    }

    /** A spec that tolerates the expected test-vs-prod drift (dynamic IP, port pool, timing). */
    private ObjectMatcher<BookSearchResult> tolerantSpec() {
        return ObjectMatcher.forClass(BookSearchResult.class)
                .with(StringMatchers.regex(IP_PATTERN), BookSearchResult::metadata,
                        SearchMetadata::server, Server::ip)
                .with(oneOf(8080, 8081, 8090, 8091), BookSearchResult::metadata,
                        SearchMetadata::server, Server::port)
                .with(inRange(2, 5000), BookSearchResult::metadata,
                        SearchMetadata::processingTimeMs);
    }

    @Test
    public void prodIsEquivalentToTestUnderTolerantRules() {
        assertMatches(desktopTest, desktopProd, tolerantSpec());
    }

    @Test
    public void rawComparisonThrowsWithAStructuredPerFieldDiff() {
        // baseline (expected) first, actual second — the same order as JUnit's assertEquals.
        var error = assertThrows(AssertionFailedError.class,
                () -> assertMatches(desktopTest, desktopProd));

        var message = error.getMessage();
        assertTrue(message, message.contains("Metadata.ProcessingTimeMs"));
        assertTrue(message, message.contains("Metadata.Server.Ip"));
        assertTrue(message, message.contains("Metadata.Server.Port"));

        // The two responses ride along so a JUnit 5 IDE can render a comparison view.
        assertTrue(error.getExpected().getEphemeralValue() == desktopTest);
        assertTrue(error.getActual().getEphemeralValue() == desktopProd);
    }

}
