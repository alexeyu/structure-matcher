package nl.alexeyu.structmatcher.junit5;

import java.util.List;

/**
 * A small local model for the junit5 module's tests (a module can't see another module's test
 * fixtures). A structure with a simple field, a list, and a nested structure — enough to exercise
 * canonical paths in failure messages.
 */
public record SampleStructure(String color, List<String> tags, SampleSub sub) {
}
