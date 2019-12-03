package nl.alexeyu.structmatcher.matcher;

import java.util.Optional;

import nl.alexeyu.structmatcher.property.PropertyPath;

@FunctionalInterface
interface CustomMatcherResolver {

    Optional<Matcher<Object>> forPath(PropertyPath path);
}
