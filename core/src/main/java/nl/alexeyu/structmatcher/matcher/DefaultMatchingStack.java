package nl.alexeyu.structmatcher.matcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class DefaultMatchingStack implements MatchingStack {
    
    public static final MatchingStack BARE = new DefaultMatchingStack(Collections.emptyMap());

    private final List<String> path = new ArrayList<>();
    
    private final Map<List<String>, Matcher<?>> propertyToMatcher;

    public DefaultMatchingStack(Map<List<String>, Matcher<?>> propertyToMatcher) {
        this.propertyToMatcher = propertyToMatcher;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <V> Optional<Matcher<V>> push(String property) {
        path.add(property);
        Matcher matcher = propertyToMatcher.get(path);
        return Optional.ofNullable(matcher);
    }

    @Override
    public void pop() {
        if (!path.isEmpty()) {
            path.remove(path.size() - 1);
        }
    }

}
