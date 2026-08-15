package nl.alexeyu.structmatcher.matcher;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.FutureTask;

import org.junit.jupiter.api.Test;

public class ObjectMatcherTest {

    private final Structure expected = new Structure(Color.BLACK, asList("a"),
            new Substructure(true));

    private final Structure actual = new Structure(Color.BLACK, asList("b"),
            new Substructure(false));

    private final Matcher<Object> ignore = Matchers.anyValue();

    @Test
    public void ignoreAllPropertiesOnTheHighestLevel() {
        var feedback = ObjectMatcher.forClass(Structure.class).withMatcher(ignore, "Color")
                .withMatcher(ignore, "Strings").withMatcher(ignore, "Sub").match(expected, actual);
        assertTrue(feedback.isEmpty());
    }

    @Test
    public void ignoreAllPropertiesOnTheLowestLevel() {
        var feedback = ObjectMatcher.forClass(Structure.class).withMatcher(ignore, "Color")
                .withMatcher(ignore, "Strings").withMatcher(ignore, "Sub", "Bool")
                .match(expected, actual);
        assertTrue(feedback.isEmpty());
    }

    /**
     * A custom matcher can run a comparison of its own, and the outer rules have to survive it.
     * Records give properties in declaration order, so the nesting matcher on Color runs before
     * the two rules it used to clobber.
     */
    @Test
    public void aNestedComparisonLeavesTheOuterSpecIntact() {
        Matcher<Object> runsItsOwnComparison = (name, base, target) -> ObjectMatcher
                .forClass(RecordSubstructure.class)
                .match(new RecordSubstructure(true), new RecordSubstructure(true));
        var feedback = ObjectMatcher.forClass(RecordStructure.class)
                .withMatcher(runsItsOwnComparison, "Color").withMatcher(ignore, "Strings")
                .withMatcher(ignore, "Sub")
                .match(new RecordStructure(Color.BLACK, asList("a"), new RecordSubstructure(true)),
                        new RecordStructure(Color.BLACK, asList("b"),
                                new RecordSubstructure(false)));
        assertTrue(feedback.isEmpty());
    }

    /**
     * A wildcard rule loosens a property everywhere it occurs, and an exact path takes one of
     * them back. Both patterns match {@code Sub.Bool}, so the exact one has to win.
     */
    @Test
    public void anExactPathOverridesAWildcardOnTheSameProperty() {
        var base = new Structure(Color.BLACK, asList("a"), new Substructure(true));
        var boolDiffers = new Structure(Color.BLACK, asList("a"), new Substructure(false));
        assertTrue(ObjectMatcher.forClass(Structure.class).with(ignore, "*.Bool")
                .match(base, boolDiffers).isEmpty());
        assertFalse(ObjectMatcher.forClass(Structure.class).with(ignore, "*.Bool")
                .with(Matchers.valuesEqual(), "Sub.Bool").match(base, boolDiffers).isEmpty());
    }

    /** Run a batch in parallel one comparison per thread: each call keeps its own stack. */
    @Test
    public void runsOnAThreadOfItsOwn() throws Exception {
        var comparison = new FutureTask<>(() -> ObjectMatcher.forClass(Structure.class)
                .withMatcher(ignore, "Strings").withMatcher(ignore, "Sub").match(expected, actual));
        new Thread(comparison).start();
        assertTrue(comparison.get().isEmpty());
    }

}
