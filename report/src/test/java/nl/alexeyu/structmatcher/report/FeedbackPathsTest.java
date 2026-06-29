package nl.alexeyu.structmatcher.report;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

public class FeedbackPathsTest {

    @Test
    public void emptyTreeHasNoBrokenPaths() {
        var root = Feedback.composite("com.X", singletonList(Feedback.empty("Color")));
        assertTrue(FeedbackPaths.brokenPaths(root).isEmpty());
    }

    @Test
    public void dropsRootNameAndJoinsStructureChildrenWithDots() {
        var root = Feedback.composite("com.X", asList(Feedback.nonEqual("Color", "BLACK", "WHITE"),
                Feedback.composite("Sub", singletonList(Feedback.nonEqual("Bool", true, false)))));
        assertEquals(asList("Color", "Sub.Bool"), FeedbackPaths.brokenPaths(root));
    }

    @Test
    public void collectionElementKeepsBracketWithoutRepeatingTheCollectionName() {
        var strings = Feedback.composite("Strings",
                singletonList(Feedback.nonEqual("Strings[0]", "a", "b")));
        var root = Feedback.composite("com.X", singletonList(strings));
        assertEquals(singletonList("Strings[0]"), FeedbackPaths.brokenPaths(root));
    }

    @Test
    public void nestsThroughListElementsThatAreStructures() {
        var author = Feedback.composite("Authors[0]",
                singletonList(Feedback.nonEqual("FirstName", "S.", "Stephen")));
        var authors = Feedback.composite("Authors", singletonList(author));
        var book = Feedback.composite("Books[0]", singletonList(authors));
        var books = Feedback.composite("Books", singletonList(book));
        var root = Feedback.composite("com.X", singletonList(books));
        assertEquals(singletonList("Books[0].Authors[0].FirstName"),
                FeedbackPaths.brokenPaths(root));
    }

    @Test
    public void skipsMatchingChildrenAmongBrokenOnes() {
        FeedbackNode root = Feedback.composite("com.X", asList(Feedback.empty("Color"),
                Feedback.nonEqual("Port", 8080, 8081), Feedback.empty("Ip")));
        assertEquals(singletonList("Port"), FeedbackPaths.brokenPaths(root));
    }

    @Test
    public void toFieldPathCollapsesEveryBracketedSegment() {
        assertEquals("Books[].Authors[].FirstName",
                FeedbackPaths.toFieldPath("Books[0].Authors[2].FirstName"));
        assertEquals("Headers[]", FeedbackPaths.toFieldPath("Headers[Content-Type]"));
        assertEquals("Metadata.Server.Ip", FeedbackPaths.toFieldPath("Metadata.Server.Ip"));
    }

}
