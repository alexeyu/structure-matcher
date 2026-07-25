package nl.alexeyu.structmatcher.matcher;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Matching driven by a property's declared type where that type is not the plain
 * {@code List}/{@code Map}/{@code Set} interface: a concrete collection type must still be matched
 * as a collection, and the bridge method of a covariant accessor must not be taken for a property.
 * Both used to be classified wrongly - the first reported differing collections as matching, the
 * second failed with a {@code ClassCastException}.
 */
public class DeclaredTypeMatchingTest {

    @Test
    public void equalConcreteCollectionsMatch() {
        var feedback = match(holder(items("a"), sections(true), tags("x")),
                holder(items("a"), sections(true), tags("x")));
        assertTrue(feedback.isEmpty());
    }

    @Test
    public void differingArrayListIsReportedElementWise() {
        var feedback = match(holder(items("a"), sections(true), tags("x")),
                holder(items("b"), sections(true), tags("x")));
        assertFalse(feedback.isEmpty());
        assertEquals(
                rootFeedback(Feedback.composite("Items",
                        asList(Feedback.nonEqual("Items[0]", "a", "b")))),
                feedback);
    }

    @Test
    public void differingArrayListSizeIsReported() {
        var feedback = match(holder(items("a"), sections(true), tags("x")),
                holder(items("a", "b"), sections(true), tags("x")));
        assertFalse(feedback.isEmpty());
        assertEquals(rootFeedback(Feedback.differentCollectionSizes("Items", 1, 2)), feedback);
    }

    @Test
    public void differingHashSetIsReportedByMembership() {
        var feedback = match(holder(items("a"), sections(true), tags("x")),
                holder(items("a"), sections(true), tags("y")));
        assertFalse(feedback.isEmpty());
        assertEquals(
                rootFeedback(Feedback.composite("Tags",
                        asList(Feedback.gotNull("Tags[x]", "x"),
                                Feedback.gotNonNull("Tags[y]", "y")))),
                feedback);
    }

    @Test
    public void differingHashMapValueIsReportedUnderItsKey() {
        var feedback = match(holder(items("a"), sections(true), tags("x")),
                holder(items("a"), sections(false), tags("x")));
        assertFalse(feedback.isEmpty());
        assertEquals(
                rootFeedback(Feedback.composite("Sections",
                        asList(Feedback.composite("Sections[s]",
                                asList(Feedback.nonEqual("Bool", true, false)))))),
                feedback);
    }

    @Test
    public void equalValuesBehindAGenericAccessorMatch() {
        var feedback = ObjectMatcher.forClass(StringValueBox.class)
                .match(new StringValueBox("x"), new StringValueBox("x"));
        assertTrue(feedback.isEmpty());
    }

    @Test
    public void differingValueBehindAGenericAccessorIsReportedOnce() {
        var feedback = ObjectMatcher.forClass(StringValueBox.class)
                .match(new StringValueBox("x"), new StringValueBox("y"));
        assertFalse(feedback.isEmpty());
        assertEquals(Feedback.composite(StringValueBox.class.getName(),
                asList(Feedback.nonEqual("Value", "x", "y"))), feedback);
    }

    private FeedbackNode match(ConcreteCollectionsHolder expected,
            ConcreteCollectionsHolder actual) {
        return ObjectMatcher.forClass(ConcreteCollectionsHolder.class).match(expected, actual);
    }

    /** The whole-object feedback for a single broken property. */
    private static FeedbackNode rootFeedback(FeedbackNode brokenProperty) {
        return Feedback.composite(ConcreteCollectionsHolder.class.getName(),
                asList(brokenProperty));
    }

    private static ConcreteCollectionsHolder holder(ArrayList<String> items,
            HashMap<String, Substructure> sections, HashSet<String> tags) {
        return new ConcreteCollectionsHolder(items, sections, tags);
    }

    private static ArrayList<String> items(String... values) {
        return new ArrayList<>(List.of(values));
    }

    private static HashMap<String, Substructure> sections(boolean bool) {
        return new HashMap<>(Map.of("s", new Substructure(bool)));
    }

    private static HashSet<String> tags(String... values) {
        return new HashSet<>(Set.of(values));
    }

}
