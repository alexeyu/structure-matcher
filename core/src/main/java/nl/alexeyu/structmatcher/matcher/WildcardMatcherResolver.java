package nl.alexeyu.structmatcher.matcher;

import java.util.Map;
import java.util.Optional;

import nl.alexeyu.structmatcher.property.PropertyPathPattern;
import nl.alexeyu.structmatcher.property.PropertyPath;

final class WildcardMatcherResolver implements CustomMatcherResolver {

    private final Map<PropertyPathPattern, Matcher<Object>> propertyToMatcher;
    
    private final WildcardPathChecker pathMatcher = new WildcardPathChecker();
    
    public WildcardMatcherResolver(Map<PropertyPathPattern, Matcher<Object>> propertyToMatcher) {
        this.propertyToMatcher = propertyToMatcher;
    }

    @Override
    public Optional<Matcher<Object>> forPath(PropertyPath path) {
        return propertyToMatcher.entrySet().stream()
                .filter(e -> pathMatcher.test(e.getKey(), path))
                .map(e -> e.getValue())
                .findFirst();
    }

}
