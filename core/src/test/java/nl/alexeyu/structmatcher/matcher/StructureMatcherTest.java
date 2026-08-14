package nl.alexeyu.structmatcher.matcher;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.FutureTask;

import org.junit.jupiter.api.Test;

import nl.alexeyu.structmatcher.feedback.Feedback;

public class StructureMatcherTest {

    private Matcher<Structure> matcher = Matchers.structuresEqual();

    @Test
    public void nullsMatch() throws Exception {
        assertTrue(matcher.match("struct", null, null).isEmpty());
    }

    @Test
    public void allMatch() throws Exception {
        var actual = new Structure(Color.WHITE, asList("white color"), new Substructure(false));
        var expected = new Structure(Color.WHITE, asList("white color"), new Substructure(false));
        assertTrue(matcher.match("struct", actual, expected).isEmpty());
    }

    @Test
    public void allDontMatch() throws Exception {
        var expected = new Structure(Color.WHITE, asList("white color"), new Substructure(true));
        var actual = new Structure(Color.BLACK, asList("black color"), new Substructure(false));
        var feedback = matcher.match("struct", expected, actual);

        var expSubstructureFeedback = Feedback.composite("Sub",
                asList(Feedback.nonEqual("Bool", true, false)));
        var expCcolorListFeedback = Feedback.composite("Strings",
                asList(Feedback.nonEqual("Strings[0]", "white color", "black color")));
        var expectedFeedback = Feedback.composite("struct",
                asList(Feedback.nonEqual("Color", Color.WHITE, Color.BLACK), expCcolorListFeedback,
                        expSubstructureFeedback));

        assertEquals(expectedFeedback, feedback);
    }

    /**
     * Used on its own, off any {@code ObjectMatcher.match}, the matcher falls back to a bare stack
     * on whichever thread it runs.
     */
    @Test
    public void runsOutsideAnObjectMatcherOnAnyThread() throws Exception {
        var expected = new Structure(Color.WHITE, asList("white color"), new Substructure(true));
        var actual = new Structure(Color.WHITE, asList("white color"), new Substructure(false));
        var comparison = new FutureTask<>(() -> matcher.match("struct", expected, actual));
        new Thread(comparison).start();
        assertEquals(
                Feedback.composite("struct",
                        asList(Feedback.composite("Sub",
                                asList(Feedback.nonEqual("Bool", true, false))))),
                comparison.get());
    }

}
