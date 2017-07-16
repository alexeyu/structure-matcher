package nl.alexeyu.structmatcher.matcher;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Comparator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

public class IgnoreOrderListMatcherTest {
    
    private IgnoreOrderListMatcher<String> matcher;
    
    @Before
    public void setUp() {
        matcher = new IgnoreOrderListMatcher<>(Comparator.comparing(String::toString));
    }
    
    @Test
    public void emptyListsMatch() {
        assertTrue(matcher.match("empty", asList(), asList()).isEmpty());
    }

    @Test
    public void listsWithNullsMatch() {
        List<String> expectedList = asList((String) null);
        List<String> actualList = asList((String) null);
        assertTrue(matcher.match("empty", expectedList, actualList).isEmpty());
    }

    @Test
    public void listsWithDifferentSizesDoNotMatch() {
        FeedbackNode feedback = matcher.match("list", asList("a"), asList("a", "b"));
        assertFalse(feedback.isEmpty());
        assertEquals(Feedback.differentCollectionSizes("list", 1, 2), feedback);
    }

    @Test
    public void listsWithDifferentOrderMatch() {
        FeedbackNode feedback = matcher.match("list", asList("a", "b"), asList("b", "a"));
        assertTrue(feedback.isEmpty());
    }

}
