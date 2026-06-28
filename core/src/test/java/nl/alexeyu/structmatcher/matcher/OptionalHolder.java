package nl.alexeyu.structmatcher.matcher;

import java.util.Optional;

/**
 * Test model with an {@link Optional} property, used to exercise optional matching end to end and
 * the {@code isOptional()} property detection.
 */
public record OptionalHolder(Optional<String> nickname) {
}
