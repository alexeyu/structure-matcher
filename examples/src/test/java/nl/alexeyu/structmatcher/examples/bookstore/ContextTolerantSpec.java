package nl.alexeyu.structmatcher.examples.bookstore;

import static nl.alexeyu.structmatcher.matcher.IntegerMatchers.inRange;
import static nl.alexeyu.structmatcher.matcher.IntegerMatchers.oneOf;
import static nl.alexeyu.structmatcher.matcher.Matchers.anyValue;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.matcher.Matcher;
import nl.alexeyu.structmatcher.matcher.Matchers;
import nl.alexeyu.structmatcher.matcher.ObjectMatcher;
import nl.alexeyu.structmatcher.matcher.StringMatchers;

/**
 * The one spec every response-level example in the bookstore package shares.
 *
 * <p>
 * A {@code BookSearchResult} carries two kinds of data. The {@code metadata} block describes
 * <em>how and where</em> the search ran (the server that answered it, how long it took, the calling
 * platform), which shifts between environments and between requests. The {@code books} list is the
 * <em>answer</em>, the contract two systems have to agree on.
 *
 * <p>
 * The fields fall into four tiers of strictness:
 * <ul>
 * <li><b>Execution context, tolerated</b> (server IP, port, processing time, platform), each under
 * a rule that still means something: a valid IP, a port from the known pool, a sane duration.
 * <li><b>Keywords, tolerated in form and strict in content</b>: the {@code keywords} list stands
 * for a <em>set of search terms</em>, so element order and letter case fall away, while the terms
 * belong to the request, and an added or dropped keyword breaks the match.
 * <li><b>Presentation of the answer, tolerated</b>: the mobile response serves a small screen, so
 * it abbreviates an author's first name to an initial and drops the publishing details. Neither
 * changes <em>which</em> books the search found. Both rules stay narrow: {@code Stephen} matches
 * {@code S.} because both reduce to the same initial, and neither matches {@code T.}; an omitted
 * {@code publishingInfo} passes, while a present one still has to be right.
 * <li><b>The answer, strict</b>: the {@code booksFound} count, every book title and every author
 * surname have to match.
 * </ul>
 * Two responses are "equivalent enough" iff they ran the same search and returned the same books,
 * whichever server produced them, however long it took, and however much of each book the target
 * device renders. The tolerance stays <em>scoped</em>: a real change in the answer, a different
 * title or a different number of hits, still fails, at its own path, where the tolerant rules
 * cannot hide it.
 */
final class ContextTolerantSpec {

    /** A dotted-quad IPv4: the server IP shifts per environment but still has to be valid. */
    static final String IP_PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    private ContextTolerantSpec() {
    }

    /** A fresh matcher that tolerates the context metadata and compares the answer strictly. */
    static ObjectMatcher<BookSearchResult> matcher() {
        // Custom rules cover the context and the keywords. booksFound and every field under books
        // keep the default equality, which is what makes the answer itself strict.
        return ObjectMatcher.forClass(BookSearchResult.class)
                // Execution-context fields, each under a rule that still means something.
                .with(StringMatchers.regex(IP_PATTERN), BookSearchResult::metadata,
                        SearchMetadata::server, Server::ip)
                .with(oneOf(8080, 8081, 8090, 8091), BookSearchResult::metadata,
                        SearchMetadata::server, Server::port)
                .with(inRange(2, 5000), BookSearchResult::metadata,
                        SearchMetadata::processingTimeMs)
                .with(anyValue(), BookSearchResult::metadata, SearchMetadata::platform)
                // Keywords: order- and case-insensitive, though the set of terms has to match.
                .with(keywordsMatcher(), BookSearchResult::metadata, SearchMetadata::keywords)
                // Presentation of the answer. String paths, because both descend into the
                // elements of the books list, which a typed accessor chain cannot express.
                .with(firstNameMatcher(), "Books.Authors.FirstName")
                .with(optionalPublishingInfo(), "Books.PublishingInfo");
    }

    /**
     * Compares an author's first name by initial, so the desktop {@code Stephen} and the mobile
     * {@code S.} come out as the same author. Normalizing <em>both</em> sides, rather than the
     * baseline alone, lets one spec serve every pairing: full-vs-full and full-vs-abbreviated both
     * reduce to {@code S.}, while a different author still fails. The {@code nonEmpty} guard runs
     * first and short-circuits, keeping {@code charAt(0)} away from an empty string.
     */
    private static Matcher<String> firstNameMatcher() {
        UnaryOperator<String> toInitial = name -> name.charAt(0) + ".";
        return Matchers.<String>nonNull().and(StringMatchers.nonEmpty())
                .and(Matchers.<String>valuesEqual().normalizingBoth(toInitial));
    }

    /**
     * Accepts a response that omits {@code publishingInfo}, since the mobile client has no room for
     * it, and compares the block field by field whenever it is there, so a wrong year or publisher
     * still fails. This tolerates <em>absence</em> alone, not <em>anything</em>.
     */
    private static Matcher<PublishingInfo> optionalPublishingInfo() {
        return (property, expected, actual) -> actual == null ? Feedback.empty(property)
                : Matchers.<PublishingInfo>structuresEqual().match(property, expected, actual);
    }

    /**
     * Compares the {@code keywords} list as a case-insensitive, order-insensitive set.
     * {@link Matchers#listsHaveEqualElements} sorts both lists to take care of order, and
     * lower-casing every element first takes care of case: sorting alone would <em>not</em>
     * tolerate case, since the elements still meet through equality after the sort. Content
     * survives both steps, so a missing, extra or different keyword still fails.
     */
    private static Matcher<List<String>> keywordsMatcher() {
        UnaryOperator<List<String>> toLowerCase =
                keywords -> keywords.stream().map(k -> k.toLowerCase(Locale.ROOT)).toList();
        return Matchers.listsHaveEqualElements(Comparator.<String>naturalOrder())
                .normalizingBoth(toLowerCase);
    }

}
