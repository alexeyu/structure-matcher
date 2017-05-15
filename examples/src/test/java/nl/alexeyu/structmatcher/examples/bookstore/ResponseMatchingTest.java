package nl.alexeyu.structmatcher.examples.bookstore;

import static nl.alexeyu.structmatcher.matcher.IntegerMatchers.inRange;
import static nl.alexeyu.structmatcher.matcher.IntegerMatchers.oneOf;
import static nl.alexeyu.structmatcher.matcher.Matchers.and;
import static nl.alexeyu.structmatcher.matcher.PredicateMatchers.nonEmptyString;
import static nl.alexeyu.structmatcher.matcher.PredicateMatchers.nonNull;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.jayway.jsonpath.JsonPath;

import nl.alexeyu.structmatcher.ObjectMatcher;
import nl.alexeyu.structmatcher.examples.bookstore.model.BookSearchResult;
import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.json.Json;
import nl.alexeyu.structmatcher.matcher.Matcher;
import nl.alexeyu.structmatcher.matcher.PredicateMatchers;

public class ResponseMatchingTest {

    private static final String IPADDRESS_PATTERN =
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    private Matcher ipMatcher = PredicateMatchers.regex(IPADDRESS_PATTERN);
    
    private Path rootPath;
    
    private BookSearchResult desktopTest, desktopProd, mobileTest;
    
    @Before
    public void setUp() throws Exception {
        rootPath = Paths.get(ResponseMatchingTest.class.getResource("/").toURI()).resolve("../../resources/test");
        ObjectMapper jsonMapper = new ObjectMapper();
        desktopTest = fromFile(jsonMapper, "response-on-smoke-for-desktop-test.json");
        mobileTest = fromFile(jsonMapper, "response-on-smoke-for-mobile-test.json");
        desktopProd = fromFile(new XmlMapper(), "response-on-smoke-for-desktop-prod.xml");
    }
    
    private BookSearchResult fromFile(ObjectMapper mapper, String fileName) throws Exception {
        return mapper.readValue(rootPath.resolve(fileName).toFile(), BookSearchResult.class);
    }

    @Test
    public void feedbackShowsAllTheDifferences() throws Exception {
        FeedbackNode feedback = ObjectMatcher.forObject("response")
                .match(desktopTest, desktopProd);
        assertFalse(feedback.isEmpty());
        String json = Json.mapper().writeValueAsString(feedback);
        assertThat(12, is(equalTo(JsonPath.read(json, "$.Metadata.ProcessingTimeMs.expectation"))));
        assertThat(14, is(equalTo(JsonPath.read(json, "$.Metadata.ProcessingTimeMs.actual"))));
        assertThat("192.168.10.10", is(equalTo(JsonPath.read(json, "$.Metadata.Server.Ip.expectation"))));
        assertThat("192.168.10.14", is(equalTo(JsonPath.read(json, "$.Metadata.Server.Ip.actual"))));
        assertThat(8080, is(equalTo(JsonPath.read(json, "$.Metadata.Server.Port.expectation"))));
        assertThat(8081, is(equalTo(JsonPath.read(json, "$.Metadata.Server.Port.actual"))));
    }

    private ObjectMatcher withMetadataMatchers(ObjectMatcher matcher) {
        return matcher
                .with(ipMatcher, "Metadata.Server.Ip")
                .with(oneOf(8080, 8081, 8090, 8091), "Metadata.Server.Port")
                .with(inRange(2, 5000), "Metadata.ProcessingTimeMs");
    }
    
    @Test
    public void prodAndTestConsideredMatchingProvidedSanityChecksAreOk() throws Exception {
        FeedbackNode feedback = withMetadataMatchers(ObjectMatcher.forObject("response"))
                .match(desktopTest, desktopProd);
        assertTrue(feedback.isEmpty());
    }
    
    @Test
    public void desktopAndMobileConsideredMatchingProvidedSanityChecksAreOk() throws Exception {
        Matcher emptyYearMatcher = (prop, exp, act) -> Integer.valueOf(0).equals(act) ? Feedback.empty(prop) : Feedback.gotNonNull(prop, act);
        FeedbackNode feedback = withMetadataMatchers(ObjectMatcher.forObject("response"))
                .with(nonNull(), "Metadata.Platform")
                .with(and(nonNull(), nonEmptyString(), new InitialMatcher()),  "Books.Authors.FirstName")
                .with(emptyYearMatcher, "Books.YearPublished")
                .match(desktopTest, mobileTest);
        assertTrue(feedback.isEmpty());
    }

    private static class InitialMatcher implements Matcher {

        @Override
        public FeedbackNode match(String property, Object expected, Object actual) {
            String expectedString = expected.toString().substring(0,  1) + "."; 
            if (actual.equals(expectedString)) {
                return Feedback.empty(property);
            }
            return Feedback.doesNotConform(property, actual, expectedString);
        }
    }
    
}
