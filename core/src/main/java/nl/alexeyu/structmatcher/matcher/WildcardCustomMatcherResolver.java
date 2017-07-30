package nl.alexeyu.structmatcher.matcher;

import java.util.List;
import java.util.Map;
import java.util.Optional;

class WildcardCustomMatcherResolver implements CustomMatcherResolver {

    private final Map<List<String>, Matcher<?>> propertyToMatcher;
    
    private final WildcardPathChecker pathMatcher = new WildcardPathChecker();
    
    public WildcardCustomMatcherResolver(Map<List<String>, Matcher<?>> propertyToMatcher) {
        this.propertyToMatcher = propertyToMatcher;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> Optional<Matcher<V>> forPath(List<String> path) {
        return propertyToMatcher.entrySet().stream()
                .filter(e -> pathMatcher.test(e.getKey(), path))
                .map(e -> (Matcher<V>) e.getValue())
                .findFirst();
    }

}
