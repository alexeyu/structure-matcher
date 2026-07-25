package nl.alexeyu.structmatcher.matcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Test model whose collection properties are declared with concrete types rather than the
 * {@code List}/{@code Map}/{@code Set} interfaces.
 */
public record ConcreteCollectionsHolder(ArrayList<String> items,
        HashMap<String, Substructure> sections, HashSet<String> tags) {
}
