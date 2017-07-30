package nl.alexeyu.structmatcher.matcher;

import java.util.List;
import java.util.Optional;

interface CustomMatcherResolver {

    <V> Optional<Matcher<V>> forPath(List<String> path);
}
