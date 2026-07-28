package nl.alexeyu.structmatcher.report;

import java.util.ArrayList;
import java.util.List;

import nl.alexeyu.structmatcher.feedback.CompositeFeedbackNode;
import nl.alexeyu.structmatcher.feedback.ExpectationBroken;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Turns a {@link FeedbackNode} tree into the flat list of paths where an expectation broke. A path
 * reads the way a custom-matcher registration path does, so the two line up:
 * <ul>
 * <li>the root property, the matched class's name, drops out, leaving paths relative to it;
 * <li>a dot joins the sub-properties of a structure: {@code Metadata.Server.Ip};
 * <li>a collection element keeps the bracketed segment its matcher produced, without repeating the
 * collection's own name: {@code Strings[0]}, {@code Books[0].Authors[0].FirstName},
 * {@code Headers[Content-Type]}.
 * </ul>
 * {@link #toFieldPath} goes one step further and collapses every bracketed segment to {@code []},
 * grouping paths that differ only by index, key or element into one field for aggregation.
 */
public final class FeedbackPaths {

    private FeedbackPaths() {
    }

    /**
     * Returns the canonical paths of every broken leaf in the tree, in depth-first encounter order.
     * A fully matching tree yields an empty list.
     */
    public static List<String> brokenPaths(FeedbackNode root) {
        return brokenLeaves(root).stream().map(BrokenLeaf::path).toList();
    }

    /**
     * Returns every broken leaf in the tree paired with its canonical path, in depth-first
     * encounter order. This is the traversal {@link #brokenPaths} and {@link FeedbackQuery} share.
     * A fully matching tree yields an empty list.
     */
    static List<BrokenLeaf> brokenLeaves(FeedbackNode root) {
        var leaves = new ArrayList<BrokenLeaf>();
        if (root.isEmpty()) {
            return leaves;
        }
        if (root instanceof CompositeFeedbackNode composite) {
            for (var child : composite.getChildren()) {
                collect(child, "", composite.getProperty(), leaves);
            }
        } else {
            leaves.add(leaf(root.getProperty(), root));
        }
        return leaves;
    }

    /**
     * Normalizes a path so that entries differing only by collection index, map key or set element
     * collapse into one field: {@code Books[0].Authors[2].FirstName} and
     * {@code Books[1].Authors[0].FirstName} both become {@code Books[].Authors[].FirstName}.
     */
    public static String toFieldPath(String exactPath) {
        return exactPath.replaceAll("\\[[^\\]]*\\]", "[]");
    }

    private static void collect(FeedbackNode node, String parentPath, String parentName,
            List<BrokenLeaf> out) {
        if (node.isEmpty()) {
            return;
        }
        var path = childPath(parentPath, parentName, node.getProperty());
        if (node instanceof CompositeFeedbackNode composite) {
            for (var child : composite.getChildren()) {
                collect(child, path, node.getProperty(), out);
            }
        } else {
            out.add(leaf(path, node));
        }
    }

    private static BrokenLeaf leaf(String path, FeedbackNode node) {
        // A non-empty, non-composite node is always an ExpectationBroken (the only broken leaf).
        return new BrokenLeaf(path, (ExpectationBroken) node);
    }

    private static String childPath(String parentPath, String parentName, String childProperty) {
        if (parentPath.isEmpty()) {
            // Directly under the root, whose own name (the class name) drops out.
            return childProperty;
        }
        if (childProperty.startsWith(parentName + "[")) {
            // A collection element whose property already embeds the collection's name, so keep
            // the bracketed suffix alone rather than repeating it.
            return parentPath + childProperty.substring(parentName.length());
        }
        return parentPath + "." + childProperty;
    }

}
