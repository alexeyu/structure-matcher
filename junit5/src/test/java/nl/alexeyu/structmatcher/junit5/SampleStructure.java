package nl.alexeyu.structmatcher.junit5;

import java.util.List;

/**
 * A small local model for the junit5 module's tests, since a module cannot see another module's
 * test fixtures. A simple field, a list and a nested structure cover the canonical paths a failure
 * message has to render.
 */
public record SampleStructure(String color, List<String> tags, SampleSub sub) {
}
