package nl.alexeyu.structmatcher.matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import nl.alexeyu.structmatcher.property.PropertyPath;
import nl.alexeyu.structmatcher.property.PropertyPathPattern;

/**
 * Randomized checks for {@link WildcardPathChecker}, expressed as plain JUnit 5
 * {@code @ParameterizedTest} + {@code @MethodSource} and complementing the example-based
 * {@link WildcardPathCheckerTest}. They pin the structural invariants of wildcard
 * matching: a literal pattern matches a path iff they are equal, and a {@code *} absorbs any run of
 * segments at either end. Cases are a fixed-seed random sample over short alphabetic segment lists
 * plus explicit edge cases — notably {@code ["A", "A"]}, which guards the wildcard-backtracking fix
 * ({@code *,A} must match {@code A,A}). The seed is fixed so any failure reproduces exactly.
 */
class WildcardPathCheckerRandomizedTest {

    private final WildcardPathChecker checker = new WildcardPathChecker();

    /** Fixed-seed random paths of short alphabetic segments plus explicit edge cases. */
    static Stream<List<String>> segments() {
        var random = new Random(20240703L);
        var generated = Stream.generate(() -> randomSegments(random)).limit(200);
        var edgeCases = Stream.<List<String>>of(List.of("a"), List.of("a", "a"), List.of("A", "A"));
        return Stream.concat(edgeCases, generated);
    }

    /**
     * Pairs of segment lists for the literal-equality check: half are two independent random lists
     * (usually unequal), half pair a list with an equal copy of itself, so both branches of the
     * {@code iff} are exercised.
     */
    static Stream<Arguments> segmentPairs() {
        var random = new Random(19750101L);
        return Stream.generate(() -> {
            var left = randomSegments(random);
            var right = random.nextBoolean() ? new ArrayList<>(left) : randomSegments(random);
            return Arguments.of(left, right);
        }).limit(200);
    }

    /** 1..6 alphabetic segments of length 1..4 (no {@code "*"}, so patterns stay literal). */
    private static List<String> randomSegments(Random random) {
        var segments = new ArrayList<String>();
        int size = 1 + random.nextInt(6); // 1..6
        for (int i = 0; i < size; i++) {
            int length = 1 + random.nextInt(4); // 1..4
            var sb = new StringBuilder();
            for (int c = 0; c < length; c++) {
                sb.append((char) ('a' + random.nextInt(26)));
            }
            segments.add(sb.toString());
        }
        return segments;
    }

    @ParameterizedTest
    @MethodSource("segments")
    void aLiteralPatternMatchesItsOwnPath(List<String> segments) {
        assertTrue(checker.test(pattern(segments), path(segments)),
                () -> "a literal pattern must match its own path: " + segments);
    }

    @ParameterizedTest
    @MethodSource("segmentPairs")
    void aLiteralPatternMatchesAPathIffTheyAreEqual(List<String> patternSegments,
            List<String> pathSegments) {
        assertEquals(patternSegments.equals(pathSegments),
                checker.test(pattern(patternSegments), path(pathSegments)),
                () -> "literal match must equal list equality: " + patternSegments + " vs "
                        + pathSegments);
    }

    @ParameterizedTest
    @MethodSource("segments")
    void aTrailingWildcardAbsorbsAnySuffix(List<String> segments) {
        int keep = new Random(segments.hashCode()).nextInt(segments.size() + 1); // prefix 0..len
        var patternSegments = new ArrayList<>(segments.subList(0, keep));
        patternSegments.add("*");
        assertTrue(checker.test(pattern(patternSegments), path(segments)),
                () -> "a trailing wildcard must absorb the suffix: " + patternSegments + " vs "
                        + segments);
    }

    @ParameterizedTest
    @MethodSource("segments")
    void aLeadingWildcardAbsorbsAnyPrefix(List<String> segments) {
        int drop = new Random(segments.hashCode()).nextInt(segments.size() + 1); // absorb 0..len
        var patternSegments = new ArrayList<String>();
        patternSegments.add("*");
        patternSegments.addAll(segments.subList(drop, segments.size()));
        assertTrue(checker.test(pattern(patternSegments), path(segments)),
                () -> "a leading wildcard must absorb the prefix: " + patternSegments + " vs "
                        + segments);
    }

    private static PropertyPathPattern pattern(List<String> segments) {
        return new PropertyPathPattern(segments);
    }

    private static PropertyPath path(List<String> segments) {
        return new PropertyPath(segments);
    }

}
