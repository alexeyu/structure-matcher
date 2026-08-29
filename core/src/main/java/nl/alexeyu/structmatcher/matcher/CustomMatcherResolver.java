package nl.alexeyu.structmatcher.matcher;

import java.util.Optional;

import nl.alexeyu.structmatcher.property.PropertyPath;

interface CustomMatcherResolver {

    Optional<Matcher<Object>> forPath(PropertyPath path);

    /**
     * The masker of a matcher registered strictly below this path, if any. A mismatch reported at
     * an ancestor carries the whole value, the masked field included, so the ancestor withholds it
     * too.
     */
    Optional<Masker> maskerBelow(PropertyPath path);
}
