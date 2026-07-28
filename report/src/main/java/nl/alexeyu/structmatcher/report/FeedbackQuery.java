package nl.alexeyu.structmatcher.report;

import java.util.List;
import java.util.function.Predicate;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Query helpers over a single {@link FeedbackNode} tree: locate the broken leaves, filtered or not.
 * {@link FeedbackAggregator} rolls many comparisons into batch-level rates; these methods inspect
 * one comparison and answer "what broke, and where". Each result is a {@link BrokenLeaf} carrying
 * the canonical path along with the expectation and the offending value, sparing a renderer or an
 * assertion a second walk of the tree.
 *
 * <p>
 * Zero runtime dependencies. These are pure functions over the tree, keeping no state, so any
 * thread may call them.
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
        return brokenLeaves(root).stream().filter(predicate).toList();
    }

    /**
     * The broken leaves at or beneath the given path prefix. Matching runs on whole path segments,
     * so {@code "Books"} covers {@code Books}, {@code Books[0].Title} and {@code Books.Count} while
     * leaving {@code BooksCount} out, and {@code "Books[0]"} covers {@code Books[0].Title}. Pass an
     * exact leaf path to fetch that one leaf.
     */
    public static List<BrokenLeaf> mismatchesUnder(FeedbackNode root, String pathPrefix) {
        return find(root, leaf -> isUnder(leaf.path(), pathPrefix));
    }

    private static boolean isUnder(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + ".")
                || path.startsWith(prefix + "[");
    }

}
