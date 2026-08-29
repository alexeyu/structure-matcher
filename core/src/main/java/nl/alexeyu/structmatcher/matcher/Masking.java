package nl.alexeyu.structmatcher.matcher;

import java.util.List;

import nl.alexeyu.structmatcher.feedback.CompositeFeedbackNode;
import nl.alexeyu.structmatcher.feedback.ExpectationBroken;
import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.feedback.ValueSlots;
import nl.alexeyu.structmatcher.property.ClassProperty;

/**
 * Withholds the values a finished feedback tree carries. {@link Matcher#masking} applies this to
 * the feedback of the property it decorates; call it directly to redact a whole tree, e.g. before
 * handing one to a renderer or a store.
 * <p>
 * A value lives in two places, and both are covered: the slots of a broken leaf, and the key of a
 * <code>property[key]</code> node, which a map or a set names after the entry. A numeric index is
 * left alone, since it locates the mismatch and holds no data.
 */
public final class Masking {

    private Masking() {
    }

    /**
     * The tree with its values replaced by what {@code masker} returns for each. The shape, the
     * property names and the conditions a matcher stated in words survive, so the feedback still
     * says which fields broke and against what rule.
     */
    public static FeedbackNode mask(FeedbackNode feedback, Masker masker) {
        return mask(feedback, maskCollectionKey(feedback.getProperty(), masker), masker);
    }

    /**
     * The tree with only the values that landed in a leaf <em>whole</em> masked: a structure or a
     * collection the matcher reported without descending into it, which happens when one side is
     * null or the two clash. Such a leaf carries every field of the value, the masked ones
     * included, and no rule registered below it ever ran.
     * <p>
     * A leaf holding a simple value is left alone: it is one field, whose own rule already decided
     * what to hide.
     */
    static FeedbackNode maskWholeValues(FeedbackNode feedback, Masker masker) {
        if (feedback instanceof CompositeFeedbackNode composite) {
            return Feedback.composite(composite.getProperty(),
                    composite.getChildren().stream().map(child -> maskChild(child, masker))
                            .toList());
        }
        if (feedback instanceof ExpectationBroken leaf && carriesWholeValue(leaf)) {
            // The key stays: no rule sits below one, so it only locates the entry that vanished.
            return maskLeaf(leaf, leaf.getProperty(), masker);
        }
        return feedback;
    }

    /** Decorates a matcher with {@link #maskWholeValues} over the feedback it returns. */
    static Matcher<Object> maskingWholeValues(Matcher<Object> delegate, Masker masker) {
        return (property, expected, actual) -> maskWholeValues(
                delegate.match(property, expected, actual), masker);
    }

    /**
     * Descends into a collection element only. Every other child was pushed onto the matching
     * stack under a path of its own and has answered for the rules below it; an element of a list,
     * a map or a set never was.
     */
    private static FeedbackNode maskChild(FeedbackNode child, Masker masker) {
        return isCollectionElement(child.getProperty()) ? maskWholeValues(child, masker) : child;
    }

    private static boolean isCollectionElement(String property) {
        return property.indexOf('[') >= 0 && property.endsWith("]");
    }

    /** Whether a slot of this leaf holds an object rather than one simple value. */
    private static boolean carriesWholeValue(ExpectationBroken leaf) {
        return switch (leaf.valueSlots()) {
            case NONE -> false;
            case VALUE -> isWhole(leaf.value());
            case BOTH -> isWhole(leaf.value()) || isWhole(leaf.expectation());
        };
    }

    private static boolean isWhole(Object value) {
        return value != null && !ClassProperty.isSimple(value.getClass());
    }

    private static FeedbackNode mask(FeedbackNode node, String property, Masker masker) {
        if (node instanceof CompositeFeedbackNode composite) {
            return Feedback.composite(property, maskChildren(composite, masker));
        }
        if (node instanceof ExpectationBroken leaf) {
            return maskLeaf(leaf, property, masker);
        }
        return Feedback.empty(property);
    }

    private static List<FeedbackNode> maskChildren(CompositeFeedbackNode composite, Masker masker) {
        return composite.getChildren().stream()
                .map(child -> mask(child, maskCollectionKey(child.getProperty(), masker), masker))
                .toList();
    }

    private static FeedbackNode maskLeaf(ExpectationBroken leaf, String property, Masker masker) {
        var slots = leaf.valueSlots();
        var expectation = slots == ValueSlots.BOTH ? maskValue(leaf.expectation(), masker)
                : leaf.expectation();
        var value = slots == ValueSlots.NONE ? leaf.value() : maskValue(leaf.value(), masker);
        return new ExpectationBroken(property, expectation, value, slots);
    }

    private static Object maskValue(Object value, Masker masker) {
        return value == null ? null : masker.mask(value);
    }

    /** Masks the key of a <code>property[key]</code> node; leaves anything else as it is. */
    private static String maskCollectionKey(String property, Masker masker) {
        var opening = property.indexOf('[');
        if (opening < 0 || !property.endsWith("]")) {
            return property;
        }
        var key = property.substring(opening + 1, property.length() - 1);
        if (isIndex(key)) {
            return property;
        }
        return property.substring(0, opening) + "[" + masker.mask(key) + "]";
    }

    private static boolean isIndex(String key) {
        return !key.isEmpty() && key.chars().allMatch(Character::isDigit);
    }

}
