package nl.alexeyu.structmatcher.matcher;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

public class ListMatcherTest {

    private ListMatcher<Object> matcher = Matchers.listsEqual();

    @Test
    public void emptyListsMatch() {
        assertTrue(matcher.match("empty", asList(), asList()).isEmpty());
    }

    @Test
    public void listsWithNullsMatch() {
        List<Object> expectedList = asList((Object) null);
        List<Object> actualList = asList((Object) null);
        assertTrue(matcher.match("empty", expectedList, actualList).isEmpty());
    }

    @Test
    public void listsWithDifferentSizesDoNotMatch() {
        FeedbackNode feedback = matcher.match("list", asList("a"), asList("a", "b"));
        assertFalse(feedback.isEmpty());
        assertEquals(Feedback.differentCollectionSizes("list", 1, 2), feedback);
    }

    @Test
    public void listsWithEqualSimpleObjectsMatch() {
        assertTrue(matcher.match("list", asList(1), asList(1)).isEmpty());
    }

    @Test
    public void listsWithDifferentSimpleObjectsDoNotMatch() {
        FeedbackNode feedback = matcher.match("list", asList(1), asList(2));
        assertFalse(feedback.isEmpty());
        assertEquals(Feedback.composite("list", asList(Feedback.nonEqual("list[0]", 1, 2))), feedback);
    }

    @Test
    public void longerListsWithDifferentSimpleObjectsDoNotMatch() {
        FeedbackNode feedback = matcher.match("list", asList(1, 2, 3), asList(1, 3, 4));
        assertFalse(feedback.isEmpty());
        assertEquals(
                Feedback.composite("list", asList(Feedback.nonEqual("list[1]", 2, 3), Feedback.nonEqual("list[2]", 3, 4))),
                feedback);
    }

    @Test
    public void listsWithDifferentComplexObjectsDoNotMatch() {
        Substructure s1 = new Substructure(true);
        Substructure s2 = new Substructure(false);
        FeedbackNode feedback = matcher.match("list", asList(s1), asList(s2));
        assertFalse(feedback.isEmpty());
        FeedbackNode expectedFeedback = Feedback.composite("list",
                asList(Feedback.composite("list[0]", asList(Feedback.nonEqual("Bool", true, false)))));
        assertEquals(expectedFeedback, feedback);
    }

}
