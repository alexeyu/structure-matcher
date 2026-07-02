package nl.alexeyu.structmatcher.examples.bookstore;

import static nl.alexeyu.structmatcher.assertj.StructMatcherAssertions.assertThat;
import static nl.alexeyu.structmatcher.matcher.IntegerMatchers.inRange;
import static nl.alexeyu.structmatcher.matcher.IntegerMatchers.oneOf;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import nl.alexeyu.structmatcher.matcher.ObjectMatcher;
import nl.alexeyu.structmatcher.matcher.StringMatchers;

/**
 * Shows the {@code assertj} bridge in the bookstore scenario: assert straight from an existing
 * AssertJ test that a v1 production response is <em>equivalent enough</em> to the v1 test baseline
 * under tolerant per-field rules, and that on a raw comparison the failure names exactly which
 * fields diverged. Same fixtures as {@link ResponseMatchingTest}.
 */
public class AssertJExampleTest {

    private static final String IP_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    private BookSearchResult desktopTest, desktopProd;

    @Before
    public void setUp() throws Exception {
        Path rootPath = Paths.get(AssertJExampleTest.class.getResource("/").toURI())
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
        assertThat(desktopProd).matchesStructure(desktopTest, tolerantSpec());
    }

    @Test
    public void rawComparisonFailsWithAStructuredPerFieldDiff() {
        // With no rules the responses genuinely differ; the AssertJ failure localizes each field.
        assertThatExceptionOfType(AssertionError.class)
                .isThrownBy(() -> assertThat(desktopProd).matchesStructure(desktopTest))
                .withMessageContaining("Metadata.ProcessingTimeMs")
                .withMessageContaining("Metadata.Server.Ip")
                .withMessageContaining("Metadata.Server.Port");
    }

}
