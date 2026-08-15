package nl.alexeyu.structmatcher.matcher;

import java.util.Map;
import java.util.Optional;

import nl.alexeyu.structmatcher.property.PropertyPath;
import nl.alexeyu.structmatcher.property.PropertyPathPattern;

/**
 * Picks the custom matcher for a path. You can register a wildcard rule for every 'Url' property
 * and an exact override for one of them, so when several patterns match, the most specific one
 * wins by {@link PropertyPathPattern#MOST_SPECIFIC_FIRST}.
 */
final class WildcardMatcherResolver implements CustomMatcherResolver {

    private final Map<PropertyPathPattern, Matcher<Object>> propertyToMatcher;

    private final WildcardPathChecker pathMatcher = new WildcardPathChecker();

    public WildcardMatcherResolver(Map<PropertyPathPattern, Matcher<Object>> propertyToMatcher) {
        this.propertyToMatcher = propertyToMatcher;
    }

    @Override
    public Optional<Matcher<Object>> forPath(PropertyPath path) {
        return propertyToMatcher.entrySet().stream().filter(e -> pathMatcher.test(e.getKey(), path))
                .min(Map.Entry.comparingByKey(PropertyPathPattern.MOST_SPECIFIC_FIRST))
                .map(Map.Entry::getValue);
    }

}
