package nl.alexeyu.structmatcher.matcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class DefaultMatchingStack implements MatchingStack {
    
    public static final MatchingStack BARE = new DefaultMatchingStack(Collections.emptyMap());

    private final List<String> path = new ArrayList<>();
    
    private final CustomMatcherResolver customMatcherResolver;

    public DefaultMatchingStack(Map<List<String>, Matcher<?>> propertyToMatcher) {
        this.customMatcherResolver = new WildcardCustomMatcherResolver(propertyToMatcher);
    }

    @Override
    public <V> Optional<Matcher<V>> push(String property) {
        path.add(property);
        return customMatcherResolver.forPath(path);
    }

    @Override
    public void pop() {
        if (!path.isEmpty()) {
            path.remove(path.size() - 1);
        }
    }

}
