package nl.alexeyu.structmatcher.assertj;

import java.util.List;

/**
 * A small local model for the assertj module's tests, since a module cannot see another module's
 * test fixtures. A simple field, a list and a nested structure cover the canonical paths a failure
 * message has to render.
 */
public record SampleStructure(String color, List<String> tags, SampleSub sub) {
}
