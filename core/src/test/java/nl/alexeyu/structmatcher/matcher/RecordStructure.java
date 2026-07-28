package nl.alexeyu.structmatcher.matcher;

import java.util.List;

/**
 * Record counterpart of {@link Structure}. Its components yield the property names "Color",
 * "Strings" and "Sub", the same names the bean getters of {@link Structure} yield.
 */
public record RecordStructure(Color color, List<String> strings, RecordSubstructure sub) {
}
