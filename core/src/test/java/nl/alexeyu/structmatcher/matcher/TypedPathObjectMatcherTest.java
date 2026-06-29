package nl.alexeyu.structmatcher.matcher;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Mirrors {@link ObjectMatcherTest} but registers matchers with type-safe accessor references
 * instead of string paths, and checks the two styles are interchangeable.
 */
public class TypedPathObjectMatcherTest {

    private final Structure expected = new Structure(Color.BLACK, asList("a"),
            new Substructure(true));

    private final Structure actual = new Structure(Color.BLACK, asList("b"),
            new Substructure(false));

    private final Matcher<Object> ignore = Matchers.anyValue();

    @Test
    public void ignoreTopLevelPropertyByReference() {
        var feedback = ObjectMatcher.forClass(Structure.class).with(ignore, Structure::getColor)
                .with(ignore, Structure::getStrings).with(ignore, Structure::getSub)
                .match(expected, actual);
        assertTrue(feedback.isEmpty());
    }

    @Test
    public void ignoreNestedPropertyByReferenceChain() {
        var feedback = ObjectMatcher.forClass(Structure.class).with(ignore, Structure::getColor)
                .with(ignore, Structure::getStrings)
                .with(ignore, Structure::getSub, Substructure::isBool).match(expected, actual);
        assertTrue(feedback.isEmpty());
    }

    @Test
    public void typedChainMatchesEquivalentStringPath() {
        var typed = ObjectMatcher.forClass(Structure.class)
                .with(ignore, Structure::getSub, Substructure::isBool).match(expected, actual);
        var stringly = ObjectMatcher.forClass(Structure.class).with(ignore, "Sub.Bool")
                .match(expected, actual);
        // Only Sub.Bool is ignored, so Strings still differs in both runs — equally.
        assertEquals(stringly.isEmpty(), typed.isEmpty());
        assertFalse(typed.isEmpty());
    }

    @Test
    public void unignoredMismatchStillReported() {
        var feedback = ObjectMatcher.forClass(Structure.class).with(ignore, Structure::getStrings)
                .match(expected, actual);
        // Color and Sub still differ, so feedback is non-empty.
        assertFalse(feedback.isEmpty());
    }

}
