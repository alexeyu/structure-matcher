package nl.alexeyu.structmatcher.matcher;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import nl.alexeyu.structmatcher.feedback.Feedback;

/**
 * Mirrors {@link StructureMatcherTest} but with {@code record} models. The
 * resulting feedback is identical, proving record components are matched exactly
 * like the equivalent bean getters (including the capitalized property names).
 */
public class RecordMatcherTest {

    private final Matcher<RecordStructure> matcher = Matchers.structuresEqual();

    @Test
    public void nullsMatch() {
        assertTrue(matcher.match("struct", null, null).isEmpty());
    }

    @Test
    public void allMatch() {
        var actual = new RecordStructure(Color.WHITE, asList("white color"), new RecordSubstructure(false));
        var expected = new RecordStructure(Color.WHITE, asList("white color"), new RecordSubstructure(false));
        assertTrue(matcher.match("struct", actual, expected).isEmpty());
    }

    @Test
    public void allDontMatch() {
        var expected = new RecordStructure(Color.WHITE, asList("white color"), new RecordSubstructure(true));
        var actual = new RecordStructure(Color.BLACK, asList("black color"), new RecordSubstructure(false));
        var feedback = matcher.match("struct", expected, actual);

        var expSubstructureFeedback = Feedback.composite("Sub", asList(Feedback.nonEqual("Bool", true, false)));
        var expColorListFeedback = Feedback.composite("Strings",
                asList(Feedback.nonEqual("Strings[0]", "white color", "black color")));
        var expectedFeedback = Feedback.composite("struct", asList(
                Feedback.nonEqual("Color", Color.WHITE, Color.BLACK),
                expColorListFeedback,
                expSubstructureFeedback));

        assertEquals(expectedFeedback, feedback);
    }

    @Test
    public void customMatcherAppliesToNestedRecordComponentByPath() {
        var expected = new RecordStructure(Color.WHITE, asList("white color"), new RecordSubstructure(true));
        var actual = new RecordStructure(Color.WHITE, asList("white color"), new RecordSubstructure(false));
        var feedback = ObjectMatcher.forClass(RecordStructure.class)
                .with(Matchers.anyValue(), "Sub.Bool")
                .match(expected, actual);
        assertTrue(feedback.isEmpty());
    }
}
