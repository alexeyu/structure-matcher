package nl.alexeyu.structmatcher.json;

import java.util.List;

/**
 * The stable, versioned persistence shape of a single comparison's feedback: the format you store
 * to disk or a database and load back to aggregate or query across a batch. Treat it as an API,
 * since the field names and {@code schemaVersion} form a contract.
 *
 * <p>
 * The shape stays <em>flat</em> where {@link Json#mapper()} nests. A comparison reduces to the list
 * of {@link ArchivedLeaf broken leaves}, each carrying its canonical path, which leaves a reader no
 * node types to guess at and no keys to collide, and round-trips cleanly through JSON. A fully
 * matching comparison has {@code matched == true} and an empty {@code brokenLeaves} list.
 * {@link FeedbackArchives} produces and parses it.
 *
 * @param schemaVersion
 *            the format version this document conforms to; see
 *            {@link FeedbackArchives#CURRENT_SCHEMA_VERSION}.
 * @param matched
 *            {@code true} iff the comparison fully matched (equivalently, {@code brokenLeaves} is
 *            empty).
 * @param brokenLeaves
 *            every broken expectation, in depth-first encounter order; empty for a match.
 */
public record FeedbackArchive(int schemaVersion, boolean matched, List<ArchivedLeaf> brokenLeaves) {

    /**
     * The canonical paths at which this comparison broke, empty for a match, in the form a report
     * aggregator consumes. Reload a persisted batch and feed each archive's paths to
     * {@code FeedbackAggregator.addBrokenPaths} to roll the stored comparisons into a summary.
     */
    public List<String> brokenPaths() {
        return brokenLeaves.stream().map(ArchivedLeaf::path).toList();
    }

}
