package nl.alexeyu.structmatcher.matcher;

import java.util.Optional;

/**
 * Supports usage of custom matchers for certain properties. A matcher can be
 * registered for a property of any nesting level. When the matching algorithm
 * traverses over a structure, it pushes every property to the stack, building a
 * current path in the structure being verified. If a custom matcher is
 * registered for the current path, the system calls this matcher. Otherwise it
 * falls back to a default matcher.
 */
interface MatchingStack {

    <V> Optional<Matcher<V>> push(String property);

    void pop();

}