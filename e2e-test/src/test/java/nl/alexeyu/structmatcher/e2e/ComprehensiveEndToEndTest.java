package nl.alexeyu.structmatcher.e2e;

import static nl.alexeyu.structmatcher.junit5.StructAssertions.assertMatches;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.AssertionFailedError;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import nl.alexeyu.structmatcher.assertj.StructMatcherAssertions;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.json.ArchivedLeaf;
import nl.alexeyu.structmatcher.json.FeedbackArchive;
import nl.alexeyu.structmatcher.json.FeedbackArchives;
import nl.alexeyu.structmatcher.json.Json;
import nl.alexeyu.structmatcher.report.FeedbackAggregator;

/**
 * A consumer's whole path through the library, from matching two snapshots to rolling a
 * persisted batch back up.
 */
public class ComprehensiveEndToEndTest {

    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new Jdk8Module());

    private static final ObjectMapper XML =
            XmlMapper.builder().addModule(new Jdk8Module()).build();

    /** Paths the spec tolerates in the rules regression, so neither bridge may name one. */
    private static final List<String> TOLERATED_PATHS =
            List.of("Services[0].Instances[1].LatencyMs", "Services[0].Tags[0]",
                    "Services[1].Endpoint.Url", "Services[1].Endpoint.Port");

    @TempDir
    private Path temporaryDirectory;

    @Test
    public void equivalentJsonAndXmlSnapshotsMatchThroughBothAssertionAdapters()
            throws IOException {
        var baseline = read("baseline.json");
        var equivalent = read("equivalent.xml");
        var matcher = DeploymentSnapshotSpec.matcher();

        assertMatches(baseline, equivalent, matcher);
        assertMatches(equivalent, baseline, matcher);
        StructMatcherAssertions.assertThat(equivalent).matchesStructure(baseline, matcher);
        StructMatcherAssertions.assertThat(baseline).matchesStructure(equivalent, matcher);
    }

    @Test
    public void theSameJsonAndXmlRegressionProducesIdenticalCompleteFeedback()
            throws IOException {
        var baseline = read("baseline.json");
        var matcher = DeploymentSnapshotSpec.matcher();
        var jsonFeedback = matcher.match(baseline, read("structural-regression.json"));
        var xmlFeedback = matcher.match(baseline, read("structural-regression.xml"));

        assertFalse(jsonFeedback.isEmpty());
        assertEquals(Json.mapper().writeValueAsString(jsonFeedback),
                Json.mapper().writeValueAsString(xmlFeedback));

        // Compare as a list: the canonical order is what lets you diff two stored archives.
        var archive = FeedbackArchives.archive(jsonFeedback);
        assertEquals(List.of(
                new ArchivedLeaf("Services[0].Aliases[1]", "catalog-v2", "catalog-v3"),
                new ArchivedLeaf("Services[0].Instances[1].Version", "2.4.0", "2.5.0"),
                new ArchivedLeaf("Services[0].Metrics[p95].Value", 120, 180),
                new ArchivedLeaf("ShardPlan[2]", 1, 2)), archive.brokenLeaves());
        assertEquals(archive, FeedbackArchives.fromJson(FeedbackArchives.toJson(jsonFeedback)));
    }

    @Test
    public void collectionAndRuleRegressionsAreDiagnosedAtEveryExpectedPath()
            throws IOException {
        var baseline = read("baseline.json");
        var matcher = DeploymentSnapshotSpec.matcher();

        assertEquals(List.of("Approval", "Capabilities[experimental]",
                "Capabilities[recommendations]", "Labels[owner]", "Labels[team]",
                "Labels[tier]", "Services[0].Metrics[errors]", "Services[0].Metrics[p95]",
                "Services[0].Notes", "Services[0].Regions[ap-south]",
                "Services[0].Regions[us-east]"),
                brokenPaths(matcher.match(baseline, read("membership-regression.json"))));

        // The '*.Url' rule reaches Metadata.Url and the exact registration overrides it there.
        // This fixture moves the control plane to a URL the regex accepts, so the path fails
        // only if that override wins.
        assertEquals(List.of("Approval.Reviewer", "Metadata.RequestId", "Metadata.GeneratedAt",
                "Metadata.Url", "Metadata.ReportedInstances", "Services[0].Endpoint.Url",
                "Services[0].Endpoint.Port", "Services[0].Instances[0].LatencyMs",
                "Services[0].Tags[1]", "Services[2].Instances"),
                brokenPaths(matcher.match(baseline, read("rules-regression.xml"))));
    }

    @Test
    public void bothAssertionBridgesNameEveryDivergedFieldAndNothingTolerated()
            throws IOException {
        var baseline = read("baseline.json");
        var regression = read("rules-regression.xml");
        var matcher = DeploymentSnapshotSpec.matcher();

        AssertionFailedError junitFailure = assertThrows(AssertionFailedError.class,
                () -> assertMatches(baseline, regression, matcher));
        assertSame(baseline, junitFailure.getExpected().getEphemeralValue());
        assertSame(regression, junitFailure.getActual().getEphemeralValue());

        AssertionError assertJFailure = assertThrows(AssertionError.class,
                () -> StructMatcherAssertions.assertThat(regression)
                        .matchesStructure(baseline, matcher));

        for (String message : List.of(junitFailure.getMessage(), assertJFailure.getMessage())) {
            assertTrue(message.contains("[Metadata.Url]"), message);
            assertTrue(message.contains("[Services[0].Endpoint.Url]"), message);
            TOLERATED_PATHS.forEach(path -> assertFalse(message.contains(path), message));
        }
    }

    @Test
    public void aConcurrentMixedFormatBatchRoundTripsAndAggregatesAfterReload()
            throws IOException {
        var baseline = read("baseline.json");
        var actuals = List.of(read("equivalent.xml"), read("structural-regression.json"),
                read("structural-regression.xml"), read("membership-regression.json"),
                read("rules-regression.xml"));
        var matcher = DeploymentSnapshotSpec.matcher();
        List<FeedbackNode> feedbacks = actuals.parallelStream()
                .map(actual -> matcher.match(baseline, actual)).toList();

        Path archiveFile = temporaryDirectory.resolve("mixed-format-feedback.jsonl");
        Files.writeString(archiveFile, FeedbackArchives.toJsonLines(feedbacks));
        List<FeedbackArchive> archives =
                FeedbackArchives.fromJsonLines(Files.readString(archiveFile));

        assertEquals(List.of(true, false, false, false, false),
                archives.stream().map(FeedbackArchive::matched).toList());
        assertTrue(archives.stream().allMatch(archive -> archive.schemaVersion()
                == FeedbackArchives.CURRENT_SCHEMA_VERSION));
        // The values survive the write and the read: rewrite what came back and you get the
        // same document. Object equality would be the stronger check, but a leaf holding a
        // whole structure reloads as a Map, so it holds only for the scalar-leaf archive
        // asserted above.
        assertEquals(FeedbackArchives.toJsonLines(feedbacks),
                FeedbackArchives.writeLines(archives));

        var aggregator = new FeedbackAggregator();
        archives.forEach(archive -> aggregator.addBrokenPaths(archive.brokenPaths()));
        var summary = aggregator.summary();

        assertEquals(5, summary.total());
        assertEquals(1, summary.matched());
        assertEquals(4, summary.mismatched());
        assertEquals(0.8, summary.mismatchRate());
        assertEquals(expectedFailureCounts(), summary.failuresByField());
        assertEquals(List.of("Services[].Aliases[]", "Services[].Instances[].Version",
                "Services[].Metrics[].Value", "ShardPlan[]"),
                summary.topMismatchingFields(4));
    }

    private List<String> brokenPaths(FeedbackNode feedback) {
        return FeedbackArchives.archive(feedback).brokenPaths();
    }

    private Map<String, Integer> expectedFailureCounts() {
        return Map.ofEntries(Map.entry("Services[].Aliases[]", 2),
                Map.entry("Services[].Instances[].Version", 2),
                Map.entry("Services[].Metrics[].Value", 2), Map.entry("ShardPlan[]", 2),
                Map.entry("Approval", 1), Map.entry("Capabilities[]", 1),
                Map.entry("Labels[]", 1), Map.entry("Services[].Metrics[]", 1),
                Map.entry("Services[].Notes", 1), Map.entry("Services[].Regions[]", 1),
                Map.entry("Approval.Reviewer", 1), Map.entry("Metadata.Url", 1),
                Map.entry("Metadata.GeneratedAt", 1),
                Map.entry("Metadata.ReportedInstances", 1),
                Map.entry("Metadata.RequestId", 1),
                Map.entry("Services[].Endpoint.Port", 1),
                Map.entry("Services[].Endpoint.Url", 1),
                Map.entry("Services[].Instances[].LatencyMs", 1),
                Map.entry("Services[].Tags[]", 1), Map.entry("Services[].Instances", 1));
    }

    private DeploymentSnapshot read(String name) throws IOException {
        ObjectMapper mapper = name.endsWith(".xml") ? XML : JSON;
        String resource = "/e2e/" + name;
        InputStream resourceStream = getClass().getResourceAsStream(resource);
        try (InputStream input = Objects.requireNonNull(resourceStream,
                "Missing test resource: " + resource)) {
            return mapper.readValue(input, DeploymentSnapshot.class);
        }
    }

}
