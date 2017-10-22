package nl.alexeyu.structmatcher.matcher;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import nl.alexeyu.structmatcher.property.PropertyPathPattern;
import nl.alexeyu.structmatcher.property.PropertyPath;

final class DefaultMatchingStack implements MatchingStack {
    
    public static final MatchingStack BARE = new DefaultMatchingStack(Collections.emptyMap());

    private final PropertyPath path = new PropertyPath();
    
    private final CustomMatcherResolver customMatcherResolver;

    public DefaultMatchingStack(Map<PropertyPathPattern, Matcher<?>> propertyToMatcher) {
        this.customMatcherResolver = new WildcardMatcherResolver(propertyToMatcher);
    }

    @Override
    public <V> Optional<Matcher<V>> push(String property) {
        path.push(property);
        return customMatcherResolver.forPath(path);
    }

    @Override
    public void pop() {
        path.pop();
    }

}
