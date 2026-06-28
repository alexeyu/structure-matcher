package nl.alexeyu.structmatcher.matcher;

/**
 * Test model with an array property, used to exercise array matching end to end and the
 * {@code isArray()} property detection.
 */
public record ArrayHolder(String[] tags) {
}
