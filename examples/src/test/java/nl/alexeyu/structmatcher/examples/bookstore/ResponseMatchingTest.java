package nl.alexeyu.structmatcher.examples.bookstore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.jayway.jsonpath.JsonPath;

import nl.alexeyu.structmatcher.json.Json;
import nl.alexeyu.structmatcher.matcher.Matcher;
import nl.alexeyu.structmatcher.matcher.ObjectMatcher;
import nl.alexeyu.structmatcher.matcher.StringMatchers;
import nl.alexeyu.structmatcher.report.FeedbackPaths;

/**
 * The core single-comparison example. A {@code BookSearchResult} mixes two kinds of data:
 * execution-context {@code metadata} (which server answered, how fast, from which platform) that
 * legitimately varies, and the {@code books} payload that is the actual answer and must not. These
 * tests show the raw comparison drowning in context noise; then the {@link ContextTolerantSpec}
 * making both the prod response (different environment) and the mobile response (abbreviated
 * presentation) "equivalent enough"; and finally that the same spec still fails on a response whose
 * answer genuinely changed, localized to the two fields that changed and no others.
 */
public class ResponseMatchingTest {

    private final Matcher<String> ipMatcher = StringMatchers.regex(ContextTolerantSpec.IP_PATTERN);

    private Path rootPath;

    private BookSearchResult desktopTest, desktopProd, mobileTest, mobileRegression;

    @BeforeEach
    public void setUp() throws Exception {
        rootPath = Paths.get(ResponseMatchingTest.class.getResource("/").toURI())
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
    public void rawComparisonDrownsInExecutionContextNoise() throws Exception {
        // Without any rules, the identical book payloads are buried under metadata that simply
        // reflects a different server and timing - the motivation for ContextTolerantSpec.
        var feedback = ObjectMatcher.forClass(BookSearchResult.class).match(desktopTest,
                desktopProd);
        assertFalse(feedback.isEmpty());
        var json = Json.mapper().writeValueAsString(feedback);
        assertThat(12, is(equalTo(JsonPath.read(json, "$.Metadata.ProcessingTimeMs.expectation"))));
        assertThat(14, is(equalTo(JsonPath.read(json, "$.Metadata.ProcessingTimeMs.value"))));
        assertThat("192.168.10.10",
                is(equalTo(JsonPath.read(json, "$.Metadata.Server.Ip.expectation"))));
        assertThat("192.168.10.14", is(equalTo(JsonPath.read(json, "$.Metadata.Server.Ip.value"))));
        assertThat(8080, is(equalTo(JsonPath.read(json, "$.Metadata.Server.Port.expectation"))));
        assertThat(8081, is(equalTo(JsonPath.read(json, "$.Metadata.Server.Port.value"))));
    }

    @Test
    public void prodMatchesTestBecauseOnlyExecutionContextDiffers() throws Exception {
        // Same search, different environment: the server, port and timing differ, but the books
        // are identical. Tolerating the metadata makes the two responses equivalent.
        var feedback = ContextTolerantSpec.matcher().match(desktopTest, desktopProd);
        assertTrue(feedback.isEmpty());
    }

    @Test
    public void mobileIsEquivalentBecauseAbbreviationsCarryTheSameAnswer() throws Exception {
        // The mobile response ran elsewhere (tolerated), abbreviates each author's first name to
        // an initial and omits the publishing details - both presentation choices for a small
        // screen, not a change in which books were found. It found the same two books, so it is
        // equivalent.
        var feedback = ContextTolerantSpec.matcher().match(desktopTest, mobileTest);
        assertTrue(feedback.isEmpty());
    }

    @Test
    public void aGenuineRegressionStillFailsAndIsLocalized() throws Exception {
        // Same tolerated presentation as above, but this response claims three hits while
        // returning two, and renders the first title differently. That is a real divergence in the
        // answer, so it surfaces - and only it does: no tolerated field rides along.
        var feedback = ContextTolerantSpec.matcher().match(desktopTest, mobileRegression);

        assertFalse(feedback.isEmpty());
        assertEquals(Set.of("Metadata.BooksFound", "Books[0].Title"),
                Set.copyOf(FeedbackPaths.brokenPaths(feedback)));
    }

    @Test
    public void typedAndStringPathsRegisterTheSameMatcher() throws Exception {
        // The typed accessor chain and the dotted string resolve to the identical path, so the
        // serialized feedback is byte-for-byte the same regardless of how the matcher is
        // registered.
        var viaTyped = ObjectMatcher.forClass(BookSearchResult.class)
                .with(ipMatcher, BookSearchResult::metadata, SearchMetadata::server, Server::ip)
                .match(desktopTest, desktopProd);
        var viaString = ObjectMatcher.forClass(BookSearchResult.class)
                .with(ipMatcher, "Metadata.Server.Ip").match(desktopTest, desktopProd);
        assertThat(Json.mapper().writeValueAsString(viaTyped),
                is(equalTo(Json.mapper().writeValueAsString(viaString))));
    }

}
