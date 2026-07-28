package nl.alexeyu.structmatcher.json;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.report.FeedbackQuery;

/**
 * Reads and writes the stable, versioned {@link FeedbackArchive persistence format} for a
 * comparison's feedback. Use this to <em>store</em> a comparison (one per document) and load it
 * back to aggregate or query a batch; {@link Json#mapper()} renders the nested tree for humans
 * instead.
 *
 * <p>
 * The mapper ignores unknown JSON properties, so a document written by a newer minor revision
 * (extra fields, same {@link #CURRENT_SCHEMA_VERSION}) still parses. {@link #fromJson} rejects a
 * {@code schemaVersion} this build does not understand rather than mis-reading the document.
 */
public final class FeedbackArchives {

    /**
     * The schema version this build writes and accepts. Bump it on any breaking change to
     * {@link FeedbackArchive} / {@link ArchivedLeaf} (renamed/removed field, changed meaning);
     * additive, backward-compatible changes do not require a bump.
     *
     * <p>
     * <strong>Bumping is not free.</strong> {@link #fromJson} accepts only this exact version, so
     * the moment this constant becomes {@code 2} every document already persisted at version 1
     * stops parsing. The version field marks <em>which</em> schema produced a document; on its own
     * it teaches the reader nothing about reading an older one. Whoever raises this must add read
     * support for the prior versions in the same change, either by branching in {@link #fromJson}
     * on the parsed {@code schemaVersion} or by migrating the old shape forward before
     * constructing the record. Skip that and older corpora become unreadable.
     */
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    /**
     * A compact writer for the JSON Lines batch format. Each archive has to occupy one line, so
     * this writer drops the indentation that single-document output keeps.
     */
    private static final ObjectWriter LINE_WRITER = MAPPER.writer()
            .without(SerializationFeature.INDENT_OUTPUT);

    private FeedbackArchives() {
    }

    /**
     * Reduces a comparison's feedback tree to its flat, versioned archive, without touching any
     * I/O. It collects the broken leaves through {@link FeedbackQuery}, so each path is the
     * canonical registration-style one.
     */
    public static FeedbackArchive archive(FeedbackNode feedback) {
        var leaves = FeedbackQuery.brokenLeaves(feedback).stream()
                .map(leaf -> new ArchivedLeaf(leaf.path(), leaf.expectation(), leaf.value()))
                .toList();
        return new FeedbackArchive(CURRENT_SCHEMA_VERSION, leaves.isEmpty(), leaves);
    }

    /** Serializes a comparison's feedback to the archive JSON. */
    public static String toJson(FeedbackNode feedback) {
        return write(archive(feedback));
    }

    /** Serializes an already-built archive to JSON. */
    public static String write(FeedbackArchive archive) {
        try {
            return MAPPER.writeValueAsString(archive);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize feedback archive", e);
        }
    }

    /**
     * Parses archive JSON, rejecting a {@code schemaVersion} this build does not understand.
     *
     * @throws IllegalArgumentException
     *             if the JSON is malformed or its {@code schemaVersion} is unsupported.
     */
    public static FeedbackArchive fromJson(String json) {
        FeedbackArchive archive;
        try {
            archive = MAPPER.readValue(json, FeedbackArchive.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not parse feedback archive", e);
        }
        if (archive.schemaVersion() != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported feedback archive schemaVersion %d; this build understands %d"
                            .formatted(archive.schemaVersion(), CURRENT_SCHEMA_VERSION));
        }
        return archive;
    }

    /**
     * Serializes a whole batch of comparisons to <a href="https://jsonlines.org">JSON Lines</a>,
     * one compact {@link FeedbackArchive} per line. Use it to persist a batch as a single document
     * (or to append to one), then reload with {@link #fromJsonLines} and roll the batch up into a
     * report. The lines follow iteration order, and an empty batch yields an empty string.
     */
    public static String toJsonLines(Collection<? extends FeedbackNode> feedbacks) {
        return writeLines(feedbacks.stream().map(FeedbackArchives::archive).toList());
    }

    /** Serializes already-built archives to JSON Lines (one compact archive per line). */
    public static String writeLines(Collection<FeedbackArchive> archives) {
        var sb = new StringBuilder();
        for (var archive : archives) {
            sb.append(writeLine(archive)).append('\n');
        }
        return sb.toString();
    }

    /**
     * Parses a JSON Lines batch back into archives, one per non-blank line, validating each line
     * the way {@link #fromJson} does: it rejects a malformed line or an unsupported
     * {@code schemaVersion}. It skips blank lines, so a trailing newline is fine.
     */
    public static List<FeedbackArchive> fromJsonLines(String jsonLines) {
        return jsonLines.lines()
                .filter(line -> !line.isBlank())
                .map(FeedbackArchives::fromJson)
                .toList();
    }

    private static String writeLine(FeedbackArchive archive) {
        try {
            return LINE_WRITER.writeValueAsString(archive);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialize feedback archive", e);
        }
    }

}
