package nl.alexeyu.structmatcher.matcher;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Randomized checks for the list matchers, expressed as plain JUnit 5
 * {@code @ParameterizedTest} + {@code @MethodSource}. {@link ListMatcher} is order- and
 * size-sensitive; {@link IgnoreOrderListMatcher} accepts any permutation (same multiset) but still
 * rejects a genuinely different one. Cases are a fixed-seed random sample over integer lists (a
 * simple, null-free element type, so the default value matcher applies) plus a few explicit edge
 * cases. The seed is fixed so any failure reproduces exactly.
 */
class ListMatcherRandomizedTest {

    private static final int OUTSIDE_RANGE = 1000; // never produced by the generator below

    /** Fixed-seed random integer lists (0..100, up to 8 elements) plus explicit edge cases. */
    static Stream<List<Integer>> intLists() {
        var random = new Random(20240703L);
        var generated = Stream.generate(() -> randomIntList(random)).limit(200);
        var edgeCases = Stream.<List<Integer>>of(List.of(), List.of(7), List.of(7, 7));
        return Stream.concat(edgeCases, generated);
    }

    private static List<Integer> randomIntList(Random random) {
        var xs = new ArrayList<Integer>();
        int size = random.nextInt(9); // 0..8
        for (int i = 0; i < size; i++) {
            xs.add(random.nextInt(101)); // 0..100
        }
        return xs;
    }

    @ParameterizedTest
    @MethodSource("intLists")
    void aListMatchesACopyOfItself(List<Integer> xs) {
        assertTrue(new ListMatcher<Integer>().match("p", xs, new ArrayList<>(xs)).isEmpty(),
                () -> "expected a list to match a copy of itself: " + xs);
    }

    @ParameterizedTest
    @MethodSource("intLists")
    void listsOfDifferentSizeNeverMatch(List<Integer> xs) {
        var longer = new ArrayList<>(xs);
        longer.add(OUTSIDE_RANGE);
        assertFalse(new ListMatcher<Integer>().match("p", xs, longer).isEmpty(),
                () -> "expected lists of different size not to match: " + xs);
    }

    @ParameterizedTest
    @MethodSource("intLists")
    void ignoreOrderAcceptsAnyPermutation(List<Integer> xs) {
        var permuted = new ArrayList<>(xs);
        Collections.shuffle(permuted, new Random(xs.hashCode()));
        var matcher = new IgnoreOrderListMatcher<Integer>(Comparator.naturalOrder());
        assertTrue(matcher.match("p", xs, permuted).isEmpty(),
                () -> "expected any permutation to match: " + xs + " vs " + permuted);
    }

    @ParameterizedTest
    @MethodSource("intLists")
    void ignoreOrderStillRejectsADifferentMultiset(List<Integer> xs) {
        if (xs.isEmpty()) {
            return; // an added/dropped element is covered by the size-sensitivity checks above
        }
        var changed = new ArrayList<>(xs);
        changed.set(0, OUTSIDE_RANGE); // drop one element, introduce a value not in xs
        var matcher = new IgnoreOrderListMatcher<Integer>(Comparator.naturalOrder());
        assertFalse(matcher.match("p", xs, changed).isEmpty(),
                () -> "expected a different multiset not to match: " + xs + " vs " + changed);
    }

}
