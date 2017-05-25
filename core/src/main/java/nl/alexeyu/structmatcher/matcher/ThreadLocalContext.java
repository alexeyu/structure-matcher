package nl.alexeyu.structmatcher.matcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class ThreadLocalContext implements Context {

    private final ThreadLocal<List<String>> path = new ThreadLocal<>();
    
    private final Map<List<String>, Matcher<?>> propertyToMatcher = new HashMap<>();

    public ThreadLocalContext() {
        path.set(new ArrayList<>());
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <V> Optional<Matcher<V>> push(String property) {
        path.get().add(property);
        Matcher matcher = propertyToMatcher.get(path.get());
        return Optional.ofNullable(matcher);
    }

    @Override
    public void pop() {
        if (!path.get().isEmpty()) {
            path.get().remove(path.get().size() - 1);
        }
    }

    @Override
    public <V> void register(Matcher<V> matcher, String... propertyPath) {
        propertyToMatcher.put(Arrays.asList(propertyPath), matcher);        
    }

}
