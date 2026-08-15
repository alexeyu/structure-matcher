package nl.alexeyu.structmatcher.json;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.matcher.MapMatcher;
import nl.alexeyu.structmatcher.matcher.Matchers;

/**
 * Characterization tests pinning the exact JSON that {@link Json#mapper()} produces for feedback
 * trees. They turn any change to the serialized output into a deliberate one, holding the shape
 * across refactorings like the move of the feedback value types to {@code record}s, which changed
 * how Jackson discovers their properties.
 */
public class FeedbackJsonTest {

    private final ObjectMapper mapper = Json.mapper();

    // The shapes the matching pipeline emits: composites of non-empty nodes.

    @Test
    public void brokenLeafOmitsPropertyAndKeepsExpectationAndValue() {
        var expected = """
                {
                  "expectation" : "white",
                  "value" : "black"
                }""";
        assertEquals(expected, json(Feedback.nonEqual("color", "white", "black")));
    }

    @Test
    public void brokenLeafRendersNullValue() {
        var expected = """
                {
                  "expectation" : "white",
                  "value" : null
                }""";
        assertEquals(expected, json(Feedback.gotNull("color", "white")));
    }

    @Test
    public void brokenLeafRendersNumbersAsNumbers() {
        var expected = """
                {
                  "expectation" : 15,
                  "value" : 17
                }""";
        assertEquals(expected, json(Feedback.nonEqual("qty", 15, 17)));
    }

    @Test
    public void compositeIsKeyedByChildPropertyNamesAndDropsItsOwnName() {
        var expected = """
                {
                  "color" : {
                    "expectation" : "white",
                    "value" : "black"
                  },
                  "qty" : {
                    "expectation" : 15,
                    "value" : 17
                  }
                }""";
        var composite = Feedback.composite("primary", asList(
                Feedback.nonEqual("color", "white", "black"), Feedback.nonEqual("qty", 15, 17)));
        assertEquals(expected, json(composite));
    }

    @Test
    public void nestedCompositesNestAsNestedObjects() {
        var expected = """
                {
                  "color" : {
                    "expectation" : "white",
                    "value" : "black"
                  },
                  "secondary" : {
                    "shade" : {
                      "expectation" : "ivory",
                      "value" : "noir"
                    }
                  }
                }""";
        var nested = Feedback.composite("primary",
                asList(Feedback.nonEqual("color", "white", "black"), Feedback.composite("secondary",
                        asList(Feedback.nonEqual("shade", "ivory", "noir")))));
        assertEquals(expected, json(nested));
    }

    // Structural assertions spelling the contract out

    @Test
    public void brokenLeafHasNoPropertyField() throws Exception {
        var node = mapper.readTree(json(Feedback.nonEqual("color", "white", "black")));
        assertFalse(node.has("property"),
                "the 'property' name is carried by the composite key, not the leaf");
        assertTrue(node.has("expectation"));
        assertTrue(node.has("value"));
    }

    @Test
    public void compositeUsesChildPropertyAsKeyNotAChildrenArray() throws Exception {
        var node = mapper.readTree(json(Feedback.composite("primary",
                asList(Feedback.nonEqual("color", "white", "black")))));
        assertFalse(node.has("children"));
        assertFalse(node.has("primary"));
        assertTrue(node.has("color"));
    }

    /**
     * Two map keys that print alike give their nodes one name, and a JSON object cannot carry that
     * name twice: a parser keeps the last field and drops the other mismatch. The namesakes share
     * one field holding an array of both.
     */
    @Test
    public void childrenSharingANameRenderAsAnArrayUnderThatName() throws Exception {
        MapMatcher<Column, String> matcher = Matchers.mapsEqual();
        var feedback = matcher.match("cols", Map.of(new Column(1), "a", new Column(2), "b"),
                Map.of(new Column(1), "x", new Column(2), "y"));

        var node = mapper.readTree(json(feedback));
        assertEquals(1, node.size());
        var namesakes = node.get("cols[col]");
        assertTrue(namesakes.isArray(), "children that share a name are grouped, not repeated");
        assertEquals(2, namesakes.size());
        assertEquals(Set.of("a", "b"), expectations(namesakes));
        assertEquals(Set.of("x", "y"), values(namesakes));
    }

    /** Two ids, one printed form, so {@code MapMatcher} gives both entries the same node name. */
    private record Column(int id) {

        @Override
        public String toString() {
            return "col";
        }

    }

    private Set<String> expectations(JsonNode namesakes) {
        return fieldValues(namesakes, "expectation");
    }

    private Set<String> values(JsonNode namesakes) {
        return fieldValues(namesakes, "value");
    }

    private Set<String> fieldValues(JsonNode namesakes, String field) {
        return StreamSupport.stream(namesakes.spliterator(), false)
                .map(namesake -> namesake.get(field).asText()).collect(toSet());
    }

    // A "met" node carries nothing to report, so it renders as the empty object, matching an
    // empty composite. Before the value types became records this threw an empty-bean error.

    @Test
    public void standaloneMetNodeRendersAsEmptyObject() {
        assertEquals("{ }", json(Feedback.empty("test")));
    }

    @Test
    public void compositeWithAMetChildRendersThatChildAsEmptyObject() {
        var expected = """
                {
                  "color" : { },
                  "qty" : {
                    "expectation" : 15,
                    "value" : 17
                  }
                }""";
        var composite = Feedback.composite("primary",
                asList(Feedback.empty("color"), Feedback.nonEqual("qty", 15, 17)));
        assertEquals(expected, json(composite));
    }

    private String json(FeedbackNode node) {
        try {
            return mapper.writeValueAsString(node).replace("\r\n", "\n");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
