package nl.alexeyu.structmatcher.matcher;

import java.util.Optional;

import nl.alexeyu.structmatcher.property.PropertyPath;

interface CustomMatcherResolver {

    <V> Optional<Matcher<V>> forPath(PropertyPath path);
}
