package nl.alexeyu.structmatcher.examples.bookstore;

import static nl.alexeyu.structmatcher.matcher.IntegerMatchers.inRange;
import static nl.alexeyu.structmatcher.matcher.IntegerMatchers.oneOf;
import static nl.alexeyu.structmatcher.matcher.Matchers.anyValue;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

import nl.alexeyu.structmatcher.matcher.Matcher;
import nl.alexeyu.structmatcher.matcher.Matchers;
import nl.alexeyu.structmatcher.matcher.ObjectMatcher;
import nl.alexeyu.structmatcher.matcher.StringMatchers;

/**
 * The heart of the bookstore examples — the one spec every response-level example shares.
 *
 * <p>
 * A {@code BookSearchResult} carries two very different kinds of data. The {@code metadata} block
 * describes <em>how and where</em> the search ran — the server that answered it, how long it took,
 * the calling platform — and that varies from one environment or request to the next. The
 * {@code books} list is the <em>answer</em>: the actual search result, which is the contract two
 * systems must agree on.
 *
 * <p>
 * So the fields fall into three tiers of strictness:
 * <ul>
 * <li><b>Execution context — tolerated</b> (server IP, port, processing time, platform), each with
 * a rule that still means something (a valid IP, a port from the known pool, a sane duration).
 * <li><b>Keywords — tolerated in form, strict in content</b>: the {@code keywords} list is
 * semantically a <em>set of search terms</em>, so element order and letter case are ignored, but
 * the terms themselves are part of the request and an added or dropped keyword is a mismatch.
 * <li><b>The answer — strict</b>: the {@code booksFound} count and every field under {@code books}
 * must match exactly.
 * </ul>
 * Two responses are then "equivalent enough" iff they ran the same search and returned the same
 * books, regardless of which server produced them or how long it took. Crucially, tolerance is
 * <em>scoped</em>: a genuine change in the answer still fails and is localized to its path (e.g.
 * {@code Books[...]}), never hidden by the metadata rules.
 */
final class ContextTolerantSpec {

    /** A dotted-quad IPv4 — the server IP varies per environment but must still be valid. */
    static final String IP_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    private ContextTolerantSpec() {
    }

    /** A fresh matcher that tolerates the context metadata and compares the answer strictly. */
    static ObjectMatcher<BookSearchResult> matcher() {
        // Only the context and keywords get custom rules; booksFound and every field under books
        // keep the default strict equality — that strict-by-default answer is the whole point.
        return ObjectMatcher.forClass(BookSearchResult.class)
                // Execution-context fields — each tolerated by a rule that still means something.
                .with(StringMatchers.regex(IP_PATTERN), BookSearchResult::metadata,
                        SearchMetadata::server, Server::ip)
                .with(oneOf(8080, 8081, 8090, 8091), BookSearchResult::metadata,
                        SearchMetadata::server, Server::port)
                .with(inRange(2, 5000), BookSearchResult::metadata,
                        SearchMetadata::processingTimeMs)
                .with(anyValue(), BookSearchResult::metadata, SearchMetadata::platform)
                // Keywords: order- and case-insensitive, but the set of terms must still match.
                .with(keywordsMatcher(), BookSearchResult::metadata, SearchMetadata::keywords);
    }

    /**
     * Compares the {@code keywords} list as a case-insensitive, order-insensitive set. Order is
     * handled by {@link Matchers#listsHaveEqualElements}, which sorts before matching; case is
     * handled by lower-casing every element first — order-insensitivity alone would <em>not</em>
     * tolerate case, because elements are still compared for equality after the sort. What survives
     * both is content: a missing, extra or genuinely different keyword still fails.
     */
    private static Matcher<List<String>> keywordsMatcher() {
        UnaryOperator<List<String>> toLowerCase =
                keywords -> keywords.stream().map(k -> k.toLowerCase(Locale.ROOT)).toList();
        return Matchers.listsHaveEqualElements(Comparator.<String>naturalOrder())
                .normalizingBoth(toLowerCase);
    }

}
