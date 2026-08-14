package nl.alexeyu.structmatcher.matcher;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import nl.alexeyu.structmatcher.feedback.CompositeFeedbackNode;
import nl.alexeyu.structmatcher.feedback.Feedback;

public class MapMatcherTest {

    private final MapMatcher<String, Object> matcher = Matchers.mapsEqual();

    @Test
    public void emptyMapsMatch() {
        assertTrue(matcher.match("empty", map(), map()).isEmpty());
    }

    @Test
    public void mapsWithEqualSimpleEntriesMatch() {
        assertTrue(matcher.match("map", map("a", 1, "b", 2), map("a", 1, "b", 2)).isEmpty());
    }

    @Test
    public void mapsWithNullValuesMatch() {
        assertTrue(matcher.match("map", map("a", null), map("a", null)).isEmpty());
    }

    @Test
    public void differentValueIsReportedUnderTheKeyedProperty() {
        var feedback = matcher.match("map", map("a", 1), map("a", 2));
        assertFalse(feedback.isEmpty());
        assertEquals(Feedback.composite("map", asList(Feedback.nonEqual("map[a]", 1, 2))),
                feedback);
    }

    @Test
    public void keyMissingFromTheActualMapIsReported() {
        var feedback = matcher.match("map", map("a", 1, "b", 2), map("a", 1));
        assertFalse(feedback.isEmpty());
        assertEquals(Feedback.composite("map", asList(Feedback.gotNull("map[b]", 2))), feedback);
    }

    @Test
    public void keyExtraInTheActualMapIsReported() {
        var feedback = matcher.match("map", map("a", 1), map("a", 1, "b", 2));
        assertFalse(feedback.isEmpty());
        assertEquals(Feedback.composite("map", asList(Feedback.gotNonNull("map[b]", 2))), feedback);
    }

    @Test
    public void mapsWithEqualComplexValuesMatch() {
        var expected = map("x", new Substructure(true));
        var actual = map("x", new Substructure(true));
        assertTrue(matcher.match("map", expected, actual).isEmpty());
    }

    @Test
    public void mapsWithDifferentComplexValuesDoNotMatch() {
        var expected = map("x", new Substructure(true));
        var actual = map("x", new Substructure(false));
        var feedback = matcher.match("map", expected, actual);
        var expectedFeedback = Feedback.composite("map", asList(
                Feedback.composite("map[x]", asList(Feedback.nonEqual("Bool", true, false)))));
        assertEquals(expectedFeedback, feedback);
    }

    @Test
    public void mapPropertyIsMatchedEndToEndThroughObjectMatcher() {
        var expected = new MapHolder(Map.of("s1", new Substructure(true)));
        var actual = new MapHolder(Map.of("s1", new Substructure(false)));
        var feedback = ObjectMatcher.forClass(MapHolder.class).match(expected, actual);
        var expectedFeedback = Feedback.composite(MapHolder.class.getName(),
                asList(Feedback.composite("Sections", asList(Feedback.composite("Sections[s1]",
                        asList(Feedback.nonEqual("Bool", true, false)))))));
        assertEquals(expectedFeedback, feedback);
    }

    /**
     * Two keys that print alike give their entries one node name, and an equal mismatch on each
     * leaves two identical children. Drop one and the report counts a single broken column.
     */
    @Test
    public void keysThatPrintAlikeKeepOneNodeEach() {
        MapMatcher<Column, String> columnMatcher = Matchers.mapsEqual();
        var feedback = columnMatcher.match("cols", Map.of(new Column(1), "a", new Column(2), "a"),
                Map.of(new Column(1), "b", new Column(2), "b"));
        var children = ((CompositeFeedbackNode) feedback).getChildren();
        assertEquals(2, children.size());
        children.forEach(child -> assertEquals(Feedback.nonEqual("cols[col]", "a", "b"), child));
    }

    /** Two ids, one printed form, and {@code MapMatcher} names the node after the print. */
    private record Column(int id) {

        @Override
        public String toString() {
            return "col";
        }

    }

    private static Map<String, Object> map(Object... keysAndValues) {
        var m = new LinkedHashMap<String, Object>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            m.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return m;
    }
}
