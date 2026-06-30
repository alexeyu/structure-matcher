package nl.alexeyu.structmatcher.report;

import nl.alexeyu.structmatcher.feedback.ExpectationBroken;

/**
 * A single broken expectation located in a {@link nl.alexeyu.structmatcher.feedback.FeedbackNode}
 * tree: its canonical {@link FeedbackPaths registration-style path} paired with the leaf node that
 * carries the expected/actual detail. Returned by {@link FeedbackQuery} so callers (renderers,
 * filters) get both <em>where</em> a comparison broke and <em>why</em>, without re-walking the tree.
 *
 * @param path
 *            the canonical path to the broken leaf (e.g. {@code Books[0].Authors[0].FirstName}), in
 *            the same style as {@link FeedbackPaths#brokenPaths}.
 * @param feedback
 *            the broken leaf node, holding the expectation and the offending value.
 */
public record BrokenLeaf(String path, ExpectationBroken feedback) {

    /** The {@link FeedbackPaths#toFieldPath normalized} path (collection indices collapsed). */
    public String fieldPath() {
        return FeedbackPaths.toFieldPath(path);
    }

    /** The expectation that was not met (e.g. the expected value, or a spec like {@code "Non-null"}). */
    public Object expectation() {
        return feedback.expectation();
    }

    /** The actual value that failed the expectation. */
    public Object value() {
        return feedback.value();
    }

}
