package nl.alexeyu.structmatcher.matcher;

import java.util.Map;
import java.util.Optional;

import nl.alexeyu.structmatcher.property.PropertyPathPattern;
import nl.alexeyu.structmatcher.property.PropertyPath;

final class WildcardMatcherResolver implements CustomMatcherResolver {

    private final Map<PropertyPathPattern, Matcher<?>> propertyToMatcher;
    
    private final WildcardPathChecker pathMatcher = new WildcardPathChecker();
    
    public WildcardMatcherResolver(Map<PropertyPathPattern, Matcher<?>> propertyToMatcher) {
        this.propertyToMatcher = propertyToMatcher;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> Optional<Matcher<V>> forPath(PropertyPath path) {
        return propertyToMatcher.entrySet().stream()
                .filter(e -> pathMatcher.test(e.getKey(), path))
                .map(e -> (Matcher<V>) e.getValue())
                .findFirst();
    }

}
