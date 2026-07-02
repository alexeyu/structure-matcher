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
 * comparison's feedback. This is the format to <em>store</em> (one comparison per document) and
 * load back to aggregate/query a batch — distinct from {@link Json#mapper()}, which renders the
 * nested tree for humans.
 *
 * <p>
 * The mapper is configured for forward compatibility: unknown JSON properties are ignored, so a
 * document written by a newer minor revision (extra fields, same
 * {@link #CURRENT_SCHEMA_VERSION}) still parses. A document whose {@code schemaVersion} this build
 * does not understand is rejected by {@link #fromJson} rather than silently mis-read.
 */
public final class FeedbackArchives {

    /**
     * The schema version this build writes and accepts. Bump it on any breaking change to
     * {@link FeedbackArchive} / {@link ArchivedLeaf} (renamed/removed field, changed meaning);
     * additive, backward-compatible changes do not require a bump.
     *
     * <p>
     * <strong>Bumping is not free.</strong> {@link #fromJson} accepts only this exact version,
     * so the moment this constant becomes {@code 2} every document already persisted at version 1
     * stops parsing. The version field marks <em>which</em> schema produced a document; it does
     * not by itself teach the reader how to read an older one. So whoever raises this must, in the
     * same change, add read support for the prior version(s) — branch in {@link #fromJson} on the
     * parsed {@code schemaVersion}, or migrate the old shape forward before constructing the
     * record — otherwise older corpora become unreadable.
     */
    public static final int CURRENT_SCHEMA_VERSION = 1;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    /**
     * A compact (single-line) writer for the JSON Lines batch format: each archive must occupy
     * exactly one line, so indentation is disabled here even though single-document output is
     * pretty-printed.
     */
    private static final ObjectWriter LINE_WRITER = MAPPER.writer()
            .without(SerializationFeature.INDENT_OUTPUT);

    private FeedbackArchives() {
    }

    /**
     * Reduces a comparison's feedback tree to its flat, versioned archive (no I/O). The broken
     * leaves are taken via {@link FeedbackQuery}, so their paths are the canonical
     * registration-style paths.
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
            throw new IllegalArgumentException("Unsupported feedback archive schemaVersion "
                    + archive.schemaVersion() + "; this build understands "
                    + CURRENT_SCHEMA_VERSION);
        }
        return archive;
    }

    /**
     * Serializes a whole batch of comparisons to <a href="https://jsonlines.org">JSON Lines</a> —
     * one compact {@link FeedbackArchive} per line. This is the format to persist a batch as a
     * single document (or append to incrementally) and reload with {@link #fromJsonLines} to roll
     * up into a report. The lines are in iteration order; an empty batch yields an empty string.
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
     * Parses a JSON Lines batch back into archives, one per non-blank line, each validated the
     * same way as {@link #fromJson} (unsupported {@code schemaVersion} and malformed lines are
     * rejected). Blank lines are skipped, so a trailing newline is fine.
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
