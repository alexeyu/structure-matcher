package nl.alexeyu.structmatcher.matcher;

import java.util.List;

/**
 * Record counterpart of {@link Structure}. Its components yield the property names
 * "Color", "Strings" and "Sub" — identical to the bean getters of {@link Structure}.
 */
public record RecordStructure(Color color, List<String> strings, RecordSubstructure sub) {
}
