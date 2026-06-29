package nl.alexeyu.structmatcher.report;

import java.util.ArrayList;
import java.util.List;

import nl.alexeyu.structmatcher.feedback.CompositeFeedbackNode;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Turns a {@link FeedbackNode} tree into the flat list of paths at which an expectation was broken.
 * A path reads the way a custom-matcher registration path does, so the two line up:
 * <ul>
 * <li>the root property (the matched class's name) is dropped — paths are relative to it;
 * <li>structure sub-properties are joined with a dot: {@code Metadata.Server.Ip};
 * <li>collection elements keep the bracketed segment their matcher produced, without repeating the
 * collection's own name: {@code Strings[0]}, {@code Books[0].Authors[0].FirstName},
 * {@code Headers[Content-Type]}.
 * </ul>
 * {@link #toFieldPath} additionally collapses every bracketed segment to {@code []} so paths that
 * differ only by index/key/element group together as one field for aggregation.
 */
public final class FeedbackPaths {

    private FeedbackPaths() {
    }

    /**
     * Returns the canonical paths of every broken leaf in the tree, in depth-first encounter order.
     * An empty (fully matching) tree yields an empty list.
     */
    public static List<String> brokenPaths(FeedbackNode root) {
        var paths = new ArrayList<String>();
        if (root.isEmpty()) {
            return paths;
        }
        if (root instanceof CompositeFeedbackNode composite) {
            for (var child : composite.getChildren()) {
                collect(child, "", composite.getProperty(), paths);
            }
        } else {
            paths.add(root.getProperty());
        }
        return paths;
    }

    /**
     * Normalizes a path so that entries differing only by collection index, map key or set element
     * become one field, e.g. {@code Books[0].Authors[2].FirstName} and
     * {@code Books[1].Authors[0].FirstName} both become {@code Books[].Authors[].FirstName}.
     */
    public static String toFieldPath(String exactPath) {
        return exactPath.replaceAll("\\[[^\\]]*\\]", "[]");
    }

    private static void collect(FeedbackNode node, String parentPath, String parentName,
            List<String> out) {
        if (node.isEmpty()) {
            return;
        }
        var path = childPath(parentPath, parentName, node.getProperty());
        if (node instanceof CompositeFeedbackNode composite) {
            for (var child : composite.getChildren()) {
                collect(child, path, node.getProperty(), out);
            }
        } else {
            out.add(path);
        }
    }

    private static String childPath(String parentPath, String parentName, String childProperty) {
        if (parentPath.isEmpty()) {
            // Directly under the root: the root's own name (the class name) is dropped.
            return childProperty;
        }
        if (childProperty.startsWith(parentName + "[")) {
            // A collection element whose property already embeds the collection's name; keep only
            // the bracketed suffix so the name is not repeated.
            return parentPath + childProperty.substring(parentName.length());
        }
        return parentPath + "." + childProperty;
    }

}
