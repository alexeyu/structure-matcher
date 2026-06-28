package nl.alexeyu.structmatcher.matcher;

import java.util.Map;

/**
 * Test model with a map property, used to exercise map matching end to end and the {@code isMap()}
 * property detection.
 */
public record MapHolder(Map<String, Substructure> sections) {
}
