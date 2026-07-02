package nl.alexeyu.structmatcher.matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

import nl.alexeyu.structmatcher.property.PropertyPath;
import nl.alexeyu.structmatcher.property.PropertyPathPattern;

/**
 * Property-based checks for {@link WildcardPathChecker}, complementing the example-based
 * {@link WildcardPathCheckerTest} with generated paths. They pin the structural invariants of
 * wildcard matching: a literal pattern matches a path iff they are equal, and a {@code *} absorbs
 * any run of segments at either end.
 */
class WildcardPathCheckerProperties {

    private final WildcardPathChecker checker = new WildcardPathChecker();

    /** Non-empty paths of short alphabetic segments (no {@code "*"}, so patterns stay literal). */
    @Provide
    Arbitrary<List<String>> segments() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(4).list().ofMinSize(1)
                .ofMaxSize(6);
    }

    @Property
    void aLiteralPatternMatchesItsOwnPath(@ForAll("segments") List<String> segments) {
        assertTrue(checker.test(pattern(segments), path(segments)));
    }

    @Property
    void aLiteralPatternMatchesAPathIffTheyAreEqual(
            @ForAll("segments") List<String> patternSegments,
            @ForAll("segments") List<String> pathSegments) {
        assertEquals(patternSegments.equals(pathSegments),
                checker.test(pattern(patternSegments), path(pathSegments)));
    }

    @Property
    void aTrailingWildcardAbsorbsAnySuffix(@ForAll("segments") List<String> segments,
            @ForAll @IntRange(min = 0, max = 6) int rawCut) {
        int keep = rawCut % (segments.size() + 1); // a literal prefix of length 0..len
        var patternSegments = new ArrayList<>(segments.subList(0, keep));
        patternSegments.add("*");
        assertTrue(checker.test(pattern(patternSegments), path(segments)));
    }

    @Property
    void aLeadingWildcardAbsorbsAnyPrefix(@ForAll("segments") List<String> segments,
            @ForAll @IntRange(min = 0, max = 6) int rawCut) {
        int drop = rawCut % (segments.size() + 1); // absorb the first 0..len segments
        var patternSegments = new ArrayList<String>();
        patternSegments.add("*");
        patternSegments.addAll(segments.subList(drop, segments.size()));
        assertTrue(checker.test(pattern(patternSegments), path(segments)));
    }

    private static PropertyPathPattern pattern(List<String> segments) {
        return new PropertyPathPattern(segments);
    }

    private static PropertyPath path(List<String> segments) {
        return new PropertyPath(segments);
    }

}
