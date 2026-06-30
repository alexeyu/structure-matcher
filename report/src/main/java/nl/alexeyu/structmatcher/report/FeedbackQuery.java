package nl.alexeyu.structmatcher.report;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Query helpers over a single {@link FeedbackNode} tree: locate the broken leaves, optionally
 * filtered. Where {@link FeedbackAggregator} rolls many comparisons into batch-level rates, this
 * inspects one comparison and answers "what broke, and where". Each result is a {@link BrokenLeaf}
 * carrying both the canonical path and the offending expectation/value, so a renderer or assertion
 * needs no second walk of the tree.
 *
 * <p>
 * Zero runtime dependencies; pure functions over the tree (no shared state, thread-safe).
 */
public final class FeedbackQuery {

    private FeedbackQuery() {
    }

    /** Every broken leaf in the tree, in depth-first order. Empty for a fully matching tree. */
    public static List<BrokenLeaf> brokenLeaves(FeedbackNode root) {
        return FeedbackPaths.brokenLeaves(root);
    }

    /** The broken leaves matching the predicate, in depth-first order. */
    public static List<BrokenLeaf> find(FeedbackNode root, Predicate<BrokenLeaf> predicate) {
        return brokenLeaves(root).stream().filter(predicate).collect(Collectors.toList());
    }

    /**
     * The broken leaves at or beneath the given path prefix. The prefix is matched on whole path
     * segments, so {@code "Books"} matches {@code Books}, {@code Books[0].Title} and
     * {@code Books.Count} but not {@code BooksCount}; {@code "Books[0]"} matches
     * {@code Books[0].Title}. Pass an exact leaf path to fetch just that leaf.
     */
    public static List<BrokenLeaf> mismatchesUnder(FeedbackNode root, String pathPrefix) {
        return find(root, leaf -> isUnder(leaf.path(), pathPrefix));
    }

    private static boolean isUnder(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + ".") || path.startsWith(prefix + "[");
    }

}
