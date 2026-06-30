package nl.alexeyu.structmatcher.json;

import java.util.List;

/**
 * The stable, versioned persistence shape of a single comparison's feedback — the format you store
 * to disk/DB and load back to aggregate or query across a batch. Treat it as an API: the field
 * names and {@code schemaVersion} are a contract.
 *
 * <p>
 * It is deliberately <em>flat</em> rather than the nested tree of {@link Json#mapper()}: a
 * comparison is reduced to the list of {@link ArchivedLeaf broken leaves} (each carrying its
 * canonical path), which is unambiguous (no node-type guessing, no key collisions) and round-trips
 * cleanly through JSON. A fully matching comparison has {@code matched == true} and an empty
 * {@code brokenLeaves} list. Produced and parsed by {@link FeedbackArchives}.
 *
 * @param schemaVersion
 *            the format version this document conforms to; see {@link FeedbackArchives#CURRENT_SCHEMA_VERSION}.
 * @param matched
 *            {@code true} iff the comparison fully matched (equivalently, {@code brokenLeaves} is
 *            empty).
 * @param brokenLeaves
 *            every broken expectation, in depth-first encounter order; empty for a match.
 */
public record FeedbackArchive(int schemaVersion, boolean matched, List<ArchivedLeaf> brokenLeaves) {

    /**
     * The canonical paths at which this comparison broke (empty for a match) — the form a report
     * aggregator consumes. Reload a persisted batch and feed each archive's paths to
     * {@code FeedbackAggregator.addBrokenPaths} to roll the stored comparisons into a summary.
     */
    public List<String> brokenPaths() {
        return brokenLeaves.stream().map(ArchivedLeaf::path).toList();
    }

}
