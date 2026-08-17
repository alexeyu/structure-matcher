package nl.alexeyu.structmatcher.matcher;

import java.util.function.Supplier;

/**
 * Routes each property to its matcher. You can register a custom matcher for a property at any
 * nesting level; as the matching descends a structure, it pushes every property onto the stack,
 * which spells out the current path. A custom matcher registered for that path wins, and the
 * default for the property type serves otherwise.
 */
interface MatchingStack<T> {

    /**
     * Pushes a property onto the stack and returns the matcher for the resulting path: the custom
     * one registered for it, or the fallback. Call this before matching the property.
     */
    Matcher<Object> push(String property, Supplier<Matcher<Object>> fallbackSupplier);

    /** Drops the top property off the stack. Call this once the property has been matched. */
    void pop();

    /** Returns the top-level base structure of this comparison. */
    T getBaseStructure();

    /** Returns the top-level target structure of this comparison. */
    T getActualStructure();

}
