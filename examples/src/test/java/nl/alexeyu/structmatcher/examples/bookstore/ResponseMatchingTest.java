package nl.alexeyu.structmatcher.examples.bookstore;

import static nl.alexeyu.structmatcher.matcher.IntegerMatchers.inRange;
import static nl.alexeyu.structmatcher.matcher.IntegerMatchers.oneOf;
import static nl.alexeyu.structmatcher.matcher.Matchers.constant;
import static nl.alexeyu.structmatcher.matcher.Matchers.valuesEqual;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.UnaryOperator;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.jayway.jsonpath.JsonPath;

import nl.alexeyu.structmatcher.json.Json;
import nl.alexeyu.structmatcher.matcher.Matcher;
import nl.alexeyu.structmatcher.matcher.Matchers;
import nl.alexeyu.structmatcher.matcher.ObjectMatcher;
import nl.alexeyu.structmatcher.matcher.StringMatchers;

public class ResponseMatchingTest {

    private static final String IPADDRESS_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    private Matcher<String> ipMatcher = StringMatchers.regex(IPADDRESS_PATTERN);

    private Path rootPath;

    private BookSearchResult desktopTest, desktopProd, mobileTest;

    @Before
    public void setUp() throws Exception {
        rootPath = Paths.get(ResponseMatchingTest.class.getResource("/").toURI())
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
    public void feedbackShowsAllTheDifferences() throws Exception {
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

    private ObjectMatcher<BookSearchResult> withMetadataMatchers(
            ObjectMatcher<BookSearchResult> matcher) {
        // Type-safe accessor chains instead of "Metadata.Server.Ip": each hop is checked by
        // the compiler and completed by the IDE. Every model class here is a record, so the hops
        // are component accessors (BookSearchResult::metadata, SearchMetadata::server, Server::ip).
        return matcher
                .with(ipMatcher, BookSearchResult::metadata, SearchMetadata::server, Server::ip)
                .with(oneOf(8080, 8081, 8090, 8091), BookSearchResult::metadata,
                        SearchMetadata::server, Server::port)
                .with(inRange(2, 5000), BookSearchResult::metadata,
                        SearchMetadata::processingTimeMs);
    }

    @Test
    public void prodAndTestConsideredMatchingProvidedSanityChecksAreOk() throws Exception {
        var feedback = withMetadataMatchers(ObjectMatcher.forClass(BookSearchResult.class))
                .match(desktopTest, desktopProd);
        assertTrue(feedback.isEmpty());
    }

    @Test
    public void desktopAndMobileConsideredMatchingProvidedSanityChecksAreOk() throws Exception {
        UnaryOperator<String> nameToInitial = name -> name.substring(0, 1) + ".";
        var feedback = withMetadataMatchers(ObjectMatcher.forClass(BookSearchResult.class))
                .with(constant(Platform.MOBILE), BookSearchResult::metadata,
                        SearchMetadata::platform)
                // Paths that traverse *into* list elements (Books -> each Author -> FirstName)
                // can't be expressed as an accessor chain, so the string path stays as the
                // escape hatch — typed and string registrations mix freely.
                .with(Matchers.and(Matchers.nonNull(), StringMatchers.nonEmpty(),
                        Matchers.normalizingBase(nameToInitial, valuesEqual())),
                        "Books.Authors.FirstName")
                .with(Matchers.constant(null), "Books.Meta").match(desktopTest, mobileTest);
        assertTrue(feedback.isEmpty());
    }

    @Test
    public void typedAndStringPathsRegisterTheSameMatcher() throws Exception {
        // The typed accessor chain and the dotted string resolve to the identical path, so the
        // serialized feedback is byte-for-byte the same regardless of how the matcher is registered.
        var viaTyped = ObjectMatcher.forClass(BookSearchResult.class)
                .with(ipMatcher, BookSearchResult::metadata, SearchMetadata::server, Server::ip)
                .match(desktopTest, desktopProd);
        var viaString = ObjectMatcher.forClass(BookSearchResult.class)
                .with(ipMatcher, "Metadata.Server.Ip").match(desktopTest, desktopProd);
        assertThat(Json.mapper().writeValueAsString(viaTyped),
                is(equalTo(Json.mapper().writeValueAsString(viaString))));
    }

}
