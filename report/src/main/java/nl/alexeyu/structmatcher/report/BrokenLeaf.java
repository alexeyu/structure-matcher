package nl.alexeyu.structmatcher.report;

import nl.alexeyu.structmatcher.feedback.ExpectationBroken;

/**
 * A single broken expectation located in a {@link nl.alexeyu.structmatcher.feedback.FeedbackNode}
 * tree: its canonical {@link FeedbackPaths registration-style path} paired with the leaf node that
 * carries the expected and actual detail. {@link FeedbackQuery} returns these, so a renderer or a
 * filter learns <em>where</em> a comparison broke and <em>why</em> without walking the tree again.
 *
 * @param path
 *            the canonical path to the broken leaf, e.g. {@code Books[0].Authors[0].FirstName}, in
 *            the style {@link FeedbackPaths#brokenPaths} produces.
 * @param feedback
 *            the broken leaf node, holding the expectation and the offending value.
 */
public record BrokenLeaf(String path, ExpectationBroken feedback) {

    /** The {@link FeedbackPaths#toFieldPath normalized} path, with collection indices collapsed. */
    public String fieldPath() {
        return FeedbackPaths.toFieldPath(path);
    }

    /**
     * What the matcher expected: a value, or a spec such as {@code "Non-null"}.
     */
    public Object expectation() {
        return feedback.expectation();
    }

    /** The value that broke it. */
    public Object value() {
        return feedback.value();
    }

}
