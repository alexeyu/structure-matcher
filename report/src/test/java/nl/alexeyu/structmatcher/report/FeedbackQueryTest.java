package nl.alexeyu.structmatcher.report;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.matcher.ObjectMatcher;

public class FeedbackQueryTest {

    private FeedbackNode tree() {
        var author = Feedback.composite("Authors[0]",
                singletonList(Feedback.nonEqual("FirstName", "S.", "Stephen")));
        var authors = Feedback.composite("Authors", singletonList(author));
        var book = Feedback.composite("Books[0]", singletonList(authors));
        var books = Feedback.composite("Books", singletonList(book));
        var sub = Feedback.composite("Sub", singletonList(Feedback.nonEqual("Bool", true, false)));
        return Feedback.composite("com.X",
                asList(Feedback.nonEqual("Color", "BLACK", "WHITE"), sub, books));
    }

    @Test
    public void emptyTreeHasNoBrokenLeaves() {
        var root = Feedback.composite("com.X", singletonList(Feedback.empty("Color")));
        assertTrue(FeedbackQuery.brokenLeaves(root).isEmpty());
    }

    @Test
    public void brokenLeavesCarryPathAndExpectationDetail() {
        var leaves = FeedbackQuery.brokenLeaves(tree());
        assertEquals(asList("Color", "Sub.Bool", "Books[0].Authors[0].FirstName"),
                leaves.stream().map(BrokenLeaf::path).collect(Collectors.toList()));
        var color = leaves.get(0);
        assertEquals("BLACK", color.expectation());
        assertEquals("WHITE", color.value());
    }

    @Test
    public void fieldPathCollapsesCollectionIndices() {
        var leaf = FeedbackQuery.brokenLeaves(tree()).get(2);
        assertEquals("Books[].Authors[].FirstName", leaf.fieldPath());
    }

    @Test
    public void findFiltersByPredicate() {
        var matched = FeedbackQuery.find(tree(), leaf -> "WHITE".equals(leaf.value()));
        assertEquals(singletonList("Color"),
                matched.stream().map(BrokenLeaf::path).collect(Collectors.toList()));
    }

    @Test
    public void mismatchesUnderMatchesWholeSegments() {
        var underBooks = FeedbackQuery.mismatchesUnder(tree(), "Books");
        assertEquals(singletonList("Books[0].Authors[0].FirstName"),
                underBooks.stream().map(BrokenLeaf::path).collect(Collectors.toList()));
    }

    @Test
    public void mismatchesUnderDoesNotMatchAPrefixThatStopsMidSegment() {
        // "Boo" is a textual prefix of "Books..." but not a path segment, so nothing is under it.
        assertTrue(FeedbackQuery.mismatchesUnder(tree(), "Boo").isEmpty());
    }

    @Test
    public void mismatchesUnderAcceptsAnExactLeafPath() {
        var exact = FeedbackQuery.mismatchesUnder(tree(), "Sub.Bool");
        assertEquals(singletonList("Sub.Bool"),
                exact.stream().map(BrokenLeaf::path).collect(Collectors.toList()));
    }

    @Test
    public void queriesALiveComparisonTree() {
        var expected = new SampleStructure("black", asList("a"), new SampleSub(true));
        var actual = new SampleStructure("white", asList("b"), new SampleSub(false));
        var feedback = ObjectMatcher.forClass(SampleStructure.class).match(expected, actual);

        List<BrokenLeaf> tagLeaves = FeedbackQuery.mismatchesUnder(feedback, "Tags");
        assertEquals(singletonList("Tags[0]"),
                tagLeaves.stream().map(BrokenLeaf::path).collect(Collectors.toList()));
        assertEquals("b", tagLeaves.get(0).value());
    }

}
