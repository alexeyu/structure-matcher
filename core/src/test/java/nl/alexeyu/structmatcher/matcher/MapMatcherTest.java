package nl.alexeyu.structmatcher.matcher;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

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
        FeedbackNode feedback = matcher.match("map", map("a", 1), map("a", 2));
        assertFalse(feedback.isEmpty());
        assertEquals(Feedback.composite("map", asList(Feedback.nonEqual("map[a]", 1, 2))), feedback);
    }

    @Test
    public void keyMissingFromTheActualMapIsReported() {
        FeedbackNode feedback = matcher.match("map", map("a", 1, "b", 2), map("a", 1));
        assertFalse(feedback.isEmpty());
        assertEquals(Feedback.composite("map", asList(Feedback.gotNull("map[b]", 2))), feedback);
    }

    @Test
    public void keyExtraInTheActualMapIsReported() {
        FeedbackNode feedback = matcher.match("map", map("a", 1), map("a", 1, "b", 2));
        assertFalse(feedback.isEmpty());
        assertEquals(Feedback.composite("map", asList(Feedback.gotNonNull("map[b]", 2))), feedback);
    }

    @Test
    public void mapsWithEqualComplexValuesMatch() {
        Map<String, Object> expected = map("x", new Substructure(true));
        Map<String, Object> actual = map("x", new Substructure(true));
        assertTrue(matcher.match("map", expected, actual).isEmpty());
    }

    @Test
    public void mapsWithDifferentComplexValuesDoNotMatch() {
        Map<String, Object> expected = map("x", new Substructure(true));
        Map<String, Object> actual = map("x", new Substructure(false));
        FeedbackNode feedback = matcher.match("map", expected, actual);
        FeedbackNode expectedFeedback = Feedback.composite("map",
                asList(Feedback.composite("map[x]", asList(Feedback.nonEqual("Bool", true, false)))));
        assertEquals(expectedFeedback, feedback);
    }

    @Test
    public void mapPropertyIsMatchedEndToEndThroughObjectMatcher() {
        MapHolder expected = new MapHolder(Map.of("s1", new Substructure(true)));
        MapHolder actual = new MapHolder(Map.of("s1", new Substructure(false)));
        FeedbackNode feedback = ObjectMatcher.forClass(MapHolder.class).match(expected, actual);
        FeedbackNode expectedFeedback = Feedback.composite(MapHolder.class.getName(), asList(
                Feedback.composite("Sections", asList(
                        Feedback.composite("Sections[s1]", asList(
                                Feedback.nonEqual("Bool", true, false)))))));
        assertEquals(expectedFeedback, feedback);
    }

    private static Map<String, Object> map(Object... keysAndValues) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            m.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return m;
    }
}
