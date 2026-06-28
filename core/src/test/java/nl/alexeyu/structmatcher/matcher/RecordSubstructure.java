package nl.alexeyu.structmatcher.matcher;

/**
 * Record counterpart of {@link Substructure}. Its boolean component {@code bool} yields the
 * property name "Bool", exactly like {@code Substructure.isBool()}.
 */
public record RecordSubstructure(boolean bool) {
}
