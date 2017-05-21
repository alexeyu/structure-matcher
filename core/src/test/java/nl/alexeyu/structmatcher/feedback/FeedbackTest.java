package nl.alexeyu.structmatcher.feedback;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

public class FeedbackTest {

    private ObjectMapper mapper;

    @Before
    public void init() {
        mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
    }

    @Test
    public void emptyFeedbackContainsOnlyPropertyName() throws Exception {
        FeedbackNode feedback = Feedback.empty("test");
        String json = mapper.writeValueAsString(feedback);
        assertEquals("test", JsonPath.read(json, "$.property"));
    }

    @Test
    public void gotNullContainsExpectedValueAndNullAsActualValue() throws Exception {
        FeedbackNode feedback = Feedback.gotNull("color", "white");
        String json = mapper.writeValueAsString(feedback);
        assertEquals("color", JsonPath.read(json, "$.property"));
        assertNull("null", JsonPath.read(json, "$.value"));
        assertEquals("white", JsonPath.read(json, "$.expectation"));
    }

    @Test
    public void gotNonNullContainsExpectedValueAndNullAsActualValue() throws Exception {
        FeedbackNode feedback = Feedback.gotNonNull("color", "black");
        String json = mapper.writeValueAsString(feedback);
        assertEquals("color", JsonPath.read(json, "$.property"));
        assertEquals("null", JsonPath.read(json, "$.expectation"));
        assertEquals("black", JsonPath.read(json, "$.value"));
    }

    @Test
    public void nonEqualPropertyFeedbackContainsPropertyInfo() throws Exception {
        FeedbackNode feedback = Feedback.nonEqual("color", "white", "black");
        String json = mapper.writeValueAsString(feedback);
        assertEquals("color", JsonPath.read(json, "$.property"));
        assertEquals("white", JsonPath.read(json, "$.expectation"));
        assertEquals("black", JsonPath.read(json, "$.value"));
    }

    @Test
    public void compositeFeedbackRemainsEmptyWhileAllItsChildrenAreEmpty() {
        CompositeFeedbackNode feedback = Feedback.composite("primary",
                asList(Feedback.empty("color"), Feedback.composite("", asList())));
        assertTrue(feedback.isEmpty());
    }

    @Test
    public void compositeFeedbackIsNoLongerEmptyWhenOneChildIsNotEmpty() {
        CompositeFeedbackNode feedback = Feedback.composite("primary", asList(Feedback.empty("color"),
                Feedback.composite("secondary", asList(Feedback.nonEqual("letter", "a", "b")))));
        assertFalse(feedback.isEmpty());
    }

    @Test
    public void compositeFeedbackAccumulatesOthers() throws Exception {
        CompositeFeedbackNode feedback = Feedback.composite("primary",
                asList(Feedback.nonEqual("color", "white", "black"), Feedback.nonEqual("qty", 15, 17)));
        String json = mapper.writeValueAsString(feedback);
        assertEquals(2, getChildrenLength(json));
        assertEquals("color", JsonPath.read(json, "$.children[0].property"));
        assertEquals("qty", JsonPath.read(json, "$.children[1].property"));
    }

    @Test
    public void feedbackCanBeNested() throws Exception {
        FeedbackNode feedback = Feedback.composite("primary", asList(
                Feedback.nonEqual("color", "white", "black"),
                Feedback.composite("secondary", asList(Feedback.nonEqual("shade", "ivory", "noir")))));
        String json = mapper.writeValueAsString(feedback);
        assertEquals(2, getChildrenLength(json));
        assertEquals("color", JsonPath.read(json, "$.children[0].property"));
        assertEquals("shade", JsonPath.read(json, "$.children[1].children[0].property"));
    }

    private int getChildrenLength(String json) {
        return JsonPath.read(json, "$.children.length()");
    }

}
