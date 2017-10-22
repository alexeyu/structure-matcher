package nl.alexeyu.structmatcher.matcher;

import java.util.Optional;

import nl.alexeyu.structmatcher.property.PropertyPath;

@FunctionalInterface
interface CustomMatcherResolver {

    <V> Optional<Matcher<V>> forPath(PropertyPath path);
}
