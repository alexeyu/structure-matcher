package nl.alexeyu.structmatcher.matcher;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

/**
 * Property-based checks for the list matchers. {@link ListMatcher} is order-sensitive and
 * size-sensitive; {@link IgnoreOrderListMatcher} accepts any permutation (same multiset) but still
 * rejects a genuinely different one. Generated over integer lists (a simple, null-free element
 * type, so the default value matcher applies).
 */
class ListMatcherProperties {

    private static final int OUTSIDE_RANGE = 1000; // never produced by the generator below

    @Provide
    Arbitrary<List<Integer>> intLists() {
        return Arbitraries.integers().between(0, 100).list().ofMaxSize(8);
    }

    @Property
    void aListMatchesACopyOfItself(@ForAll("intLists") List<Integer> xs) {
        assertTrue(new ListMatcher<Integer>().match("p", xs, new ArrayList<>(xs)).isEmpty());
    }

    @Property
    void listsOfDifferentSizeNeverMatch(@ForAll("intLists") List<Integer> xs) {
        var longer = new ArrayList<>(xs);
        longer.add(OUTSIDE_RANGE);
        assertFalse(new ListMatcher<Integer>().match("p", xs, longer).isEmpty());
    }

    @Property
    void ignoreOrderAcceptsAnyPermutation(@ForAll("intLists") List<Integer> xs, @ForAll long seed) {
        var permuted = new ArrayList<>(xs);
        Collections.shuffle(permuted, new Random(seed));
        var matcher = new IgnoreOrderListMatcher<Integer>(Comparator.naturalOrder());
        assertTrue(matcher.match("p", xs, permuted).isEmpty());
    }

    @Property
    void ignoreOrderStillRejectsADifferentMultiset(@ForAll("intLists") List<Integer> xs) {
        Assume.that(!xs.isEmpty());
        var changed = new ArrayList<>(xs);
        changed.set(0, OUTSIDE_RANGE); // drop one element, introduce a value not in xs
        var matcher = new IgnoreOrderListMatcher<Integer>(Comparator.naturalOrder());
        assertFalse(matcher.match("p", xs, changed).isEmpty());
    }

}
