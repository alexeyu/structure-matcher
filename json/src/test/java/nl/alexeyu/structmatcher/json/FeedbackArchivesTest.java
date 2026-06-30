package nl.alexeyu.structmatcher.json;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.matcher.ObjectMatcher;

/**
 * Pins the stable persistence format produced by {@link FeedbackArchives}: that a comparison's feedback
 * survives a write/read round-trip, that the format carries a {@code schemaVersion}, and that the
 * reader is strict about versions it cannot understand yet tolerant of additive (unknown) fields.
 */
public class FeedbackArchivesTest {

    private FeedbackNode brokenTree() {
        return Feedback.composite("com.X",
                asList(Feedback.nonEqual("Color", "BLACK", "WHITE"), Feedback.composite("Sub",
                        singletonList(Feedback.nonEqual("Bool", "yes", "no")))));
    }

    @Test
    public void brokenComparisonRoundTrips() {
        var archive = FeedbackArchives.fromJson(FeedbackArchives.toJson(brokenTree()));
        assertEquals(FeedbackArchives.archive(brokenTree()), archive);
        assertFalse(archive.matched());
        assertEquals(asList("Color", "Sub.Bool"),
                archive.brokenLeaves().stream().map(ArchivedLeaf::path).collect(toList()));
    }

    @Test
    public void matchedComparisonHasNoBrokenLeaves() {
        var matched = Feedback.composite("com.X", singletonList(Feedback.empty("Color")));
        var archive = FeedbackArchives.fromJson(FeedbackArchives.toJson(matched));
        assertTrue(archive.matched());
        assertTrue(archive.brokenLeaves().isEmpty());
    }

    @Test
    public void documentCarriesCurrentSchemaVersion() {
        var archive = FeedbackArchives.fromJson(FeedbackArchives.toJson(brokenTree()));
        assertEquals(FeedbackArchives.CURRENT_SCHEMA_VERSION, archive.schemaVersion());
    }

    @Test
    public void rejectsAnUnsupportedSchemaVersion() {
        var future = "{ \"schemaVersion\": 999, \"matched\": false, \"brokenLeaves\": [] }";
        var ex = assertThrows(IllegalArgumentException.class, () -> FeedbackArchives.fromJson(future));
        assertTrue(ex.getMessage().contains("999"));
    }

    @Test
    public void ignoresUnknownFieldsForForwardCompatibility() {
        var json = "{ \"schemaVersion\": 1, \"matched\": false, \"future\": \"x\","
                + " \"brokenLeaves\": [ { \"path\": \"Color\", \"expectation\": \"a\","
                + " \"value\": \"b\", \"note\": \"ignored\" } ] }";
        var archive = FeedbackArchives.fromJson(json);
        assertEquals(1, archive.brokenLeaves().size());
        assertEquals("Color", archive.brokenLeaves().get(0).path());
    }

    @Test
    public void malformedJsonIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> FeedbackArchives.fromJson("not json"));
    }

    @Test
    public void archivesALiveComparisonWithCanonicalPaths() {
        var expected = new SampleStructure("black", asList("a"), new SampleSub(true));
        var actual = new SampleStructure("white", asList("b"), new SampleSub(false));
        var feedback = ObjectMatcher.forClass(SampleStructure.class).match(expected, actual);

        var archive = FeedbackArchives.fromJson(FeedbackArchives.toJson(feedback));
        var paths = archive.brokenLeaves().stream().map(ArchivedLeaf::path).collect(toSet());
        assertEquals(Set.of("Color", "Tags[0]", "Sub.Flag"), paths);
        assertFalse(archive.matched());
    }

}
