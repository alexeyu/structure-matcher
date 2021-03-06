package nl.alexeyu.structmatcher.matcher;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

public class StructureMatcherTest {
    
    private Matcher<Structure> matcher = Matchers.structuresEqual();

    @Test
    public void nullsMatch() throws Exception {
        assertTrue(matcher.match("struct", null, null).isEmpty());
    }

    @Test
    public void allMatch() throws Exception {
        Structure actual = new Structure(Color.WHITE, asList("white color"), new Substructure(false));
        Structure expected = new Structure(Color.WHITE, asList("white color"), new Substructure(false));
        assertTrue(matcher.match("struct", actual, expected).isEmpty());
    }

    @Test
    public void allDontMatch() throws Exception {
        Structure expected = new Structure(Color.WHITE, asList("white color"), new Substructure(true));
        Structure actual = new Structure(Color.BLACK, asList("black color"), new Substructure(false));
        FeedbackNode feedback = matcher.match("struct", expected, actual);

        FeedbackNode expSubstructureFeedback = Feedback.composite("Sub", asList(Feedback.nonEqual("Bool", true, false)));
        FeedbackNode expCcolorListFeedback = Feedback.composite("Strings",
                asList(Feedback.nonEqual("Strings[0]", "white color", "black color")));
        FeedbackNode expectedFeedback = Feedback.composite("struct", asList(
                Feedback.nonEqual("Color", Color.WHITE, Color.BLACK),
                expCcolorListFeedback,
                expSubstructureFeedback));
        
        assertEquals(expectedFeedback, feedback);
    }

}
