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

    /**
     * Pushes a property with a given name to a stack and returns a custom
     * matcher which has been registered for a current path (the previous stack
     * plus a specified property), if any. If no custom matcher was registered,
     * a verification algorithm should pick up a standard matcher according to a
     * type of data being verified. Must be called before verification to help
     * to select a proper matcher.
     * 
     * @param property
     *            current property to be pushed to the stack.
     * @return <code>Optional</code> which contains a custom matcher if such
     *         matcher was registered for the current traversal path. An empty
     *         <code>Optional</code> otherwise.
     */
    <V> Optional<Matcher<V>> push(String property);

    /**
     * Removes a top property out of the stack. A verification algorithm must
     * call this method after a verification of any property.
     */
    void pop();

}