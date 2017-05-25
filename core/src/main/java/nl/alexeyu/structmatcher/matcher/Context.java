package nl.alexeyu.structmatcher.matcher;

import java.util.Optional;

/**
 * Supports usage of custom matchers for certain properties. A matcher can be
 * registered for a property of any nesting level by specifying its path as a
 * var-argument. When the matching algorithm traverses over a structure, it
 * "pushes" every property to the stack, building a current path in the
 * structure being verified. If a custom matcher is registered for a current
 * path, it should be used.
 */
interface Context {

    <V> void register(Matcher<V> matcher, String... propertyPath);

    <V> Optional<Matcher<V>> push(String property);

    void pop();

}