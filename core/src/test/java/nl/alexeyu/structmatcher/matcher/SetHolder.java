package nl.alexeyu.structmatcher.matcher;

import java.util.Set;

/**
 * Test model with a set property, used to exercise set matching end to end and the {@code isSet()}
 * property detection.
 */
public record SetHolder(Set<String> tags) {
}
