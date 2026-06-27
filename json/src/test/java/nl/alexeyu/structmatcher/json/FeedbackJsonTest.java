package nl.alexeyu.structmatcher.json;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Characterization tests pinning the exact JSON that {@link Json#mapper()} produces
 * for feedback trees. These exist to make any change to the serialized output a
 * conscious decision — they guard the shape across refactorings such as the move of
 * the feedback value types to {@code record}s, which alters how Jackson discovers
 * their properties.
 */
public class FeedbackJsonTest {

    private final ObjectMapper mapper = Json.mapper();

    // Shapes the matching pipeline actually emits (composites of non-empty nodes).

    @Test
    public void brokenLeafOmitsPropertyAndKeepsExpectationAndValue() {
        String expected = """
                {
                  "expectation" : "white",
                  "value" : "black"
                }""";
        assertEquals(expected, json(Feedback.nonEqual("color", "white", "black")));
    }

    @Test
    public void brokenLeafRendersNullValue() {
        String expected = """
                {
                  "expectation" : "white",
                  "value" : null
                }""";
        assertEquals(expected, json(Feedback.gotNull("color", "white")));
    }

    @Test
    public void brokenLeafRendersNumbersAsNumbers() {
        String expected = """
                {
                  "expectation" : 15,
                  "value" : 17
                }""";
        assertEquals(expected, json(Feedback.nonEqual("qty", 15, 17)));
    }

    @Test
    public void compositeIsKeyedByChildPropertyNamesAndDropsItsOwnName() {
        String expected = """
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
        FeedbackNode composite = Feedback.composite("primary", asList(
                Feedback.nonEqual("color", "white", "black"),
                Feedback.nonEqual("qty", 15, 17)));
        assertEquals(expected, json(composite));
    }

    @Test
    public void nestedCompositesNestAsNestedObjects() {
        String expected = """
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
        FeedbackNode nested = Feedback.composite("primary", asList(
                Feedback.nonEqual("color", "white", "black"),
                Feedback.composite("secondary", asList(
                        Feedback.nonEqual("shade", "ivory", "noir")))));
        assertEquals(expected, json(nested));
    }

    // Structural assertions documenting the contract explicitly

    @Test
    public void brokenLeafHasNoPropertyField() throws Exception {
        JsonNode node = mapper.readTree(json(Feedback.nonEqual("color", "white", "black")));
        assertFalse("the 'property' name is carried by the composite key, not the leaf", node.has("property"));
        assertTrue(node.has("expectation"));
        assertTrue(node.has("value"));
    }

    @Test
    public void compositeUsesChildPropertyAsKeyNotAChildrenArray() throws Exception {
        JsonNode node = mapper.readTree(json(Feedback.composite("primary", asList(
                Feedback.nonEqual("color", "white", "black")))));
        assertFalse(node.has("children"));
        assertFalse(node.has("primary"));
        assertTrue(node.has("color"));
    }

    // A "met" (empty) node carries nothing to report, so it renders as an empty object —
    // consistent with an empty composite, which already renders as "{ }". (Before the value
    // types became records this threw an empty-bean error; the records made it consistent.)

    @Test
    public void standaloneMetNodeRendersAsEmptyObject() {
        assertEquals("{ }", json(Feedback.empty("test")));
    }

    @Test
    public void compositeWithAMetChildRendersThatChildAsEmptyObject() {
        String expected = """
                {
                  "color" : { },
                  "qty" : {
                    "expectation" : 15,
                    "value" : 17
                  }
                }""";
        FeedbackNode composite = Feedback.composite("primary", asList(
                Feedback.empty("color"),
                Feedback.nonEqual("qty", 15, 17)));
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
