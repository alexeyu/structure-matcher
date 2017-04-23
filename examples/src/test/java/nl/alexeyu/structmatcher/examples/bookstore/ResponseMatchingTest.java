package nl.alexeyu.structmatcher.examples.bookstore;

import static java.util.Arrays.asList;
import static nl.alexeyu.structmatcher.matcher.PredicateMatchers.integerInRange;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.alexeyu.structmatcher.ObjectMatcher;
import nl.alexeyu.structmatcher.examples.bookstore.model.Author;
import nl.alexeyu.structmatcher.examples.bookstore.model.Book;
import nl.alexeyu.structmatcher.examples.bookstore.model.BookSearchResult;
import nl.alexeyu.structmatcher.examples.bookstore.model.SearchMetadata;
import nl.alexeyu.structmatcher.examples.bookstore.model.Server;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.json.Json;
import nl.alexeyu.structmatcher.matcher.Matcher;
import nl.alexeyu.structmatcher.matcher.PredicateMatchers;

public class ResponseMatchingTest {

    private static List<Book> books = asList(
                new Book("Blood and Smoke", asList(new Author("Stephen", "King")), 1999),
                new Book("Summer and smoke", asList(new Author("Tennessee", "Williams")), 1950));
    
    private static Server alpha = new Server("192.168.10.1", 8080);
    private static Server omega = new Server("192.168.10.14", 8081);
    
    private static final String IPADDRESS_PATTERN =
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    private Matcher ipMatcher = PredicateMatchers.regex(IPADDRESS_PATTERN);
    
    private static BookSearchResult resultFromTest = new BookSearchResult(
                    new SearchMetadata(asList("smoke"), books.size(), alpha, 12),
                    books);
    private static BookSearchResult resultFromProd = new BookSearchResult(
                new SearchMetadata(asList("smoke"), books.size(), omega, 14),
                books);

    @Test
    public void resultsConsideredMatchingProvidedSanityChecksAreOk() {
        FeedbackNode feedback = ObjectMatcher.forObject("response")
                .withMatcher(ipMatcher, "Metadata", "Server", "Ip")
                .withMatcher(integerInRange(0, 65536), "Metadata", "Server", "Port")
                .withMatcher(integerInRange(2, 5000), "Metadata", "ProcessingTimeMs")
                .match(resultFromTest, resultFromProd);
        assertTrue(feedback.isEmpty());
    }

    public static void main(String[] args) throws Exception {
        FeedbackNode feedback = ObjectMatcher.forObject("response")
                .match(resultFromTest, resultFromProd);
        ObjectMapper mapper  = Json.mapper();
        mapper.writeValue(System.out, feedback);
    }
    
}
