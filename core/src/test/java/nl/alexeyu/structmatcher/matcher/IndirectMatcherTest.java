package nl.alexeyu.structmatcher.matcher;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class IndirectMatcherTest {

    @Test
    public void comparesAccordingToFunctionsResults() {
        var oldStructure = new Structure(null, Arrays.asList("black"), null);
        var newStructure = new Structure(Color.BLACK, Arrays.asList(), null);
        IndirectMatcher<Structure, String> matcher = new IndirectMatcher<>(
                "string representation of color vs color object", new ValuesEqualMatcher<>(),
                s -> s.getStrings().get(0).toUpperCase(), s -> s.getColor().toString());
        assertTrue(matcher
                .match("string representation of color vs color object", oldStructure, newStructure)
                .isEmpty());
    }

}
