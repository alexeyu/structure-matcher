package nl.alexeyu.structmatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import nl.alexeyu.structmatcher.matcher.Matcher;

public final class ThreadLocalContext implements Context {

    private final ThreadLocal<List<String>> path = new ThreadLocal<>();
    
    private final Map<List<String>, Matcher> propertyToMatcher = new HashMap<>();

    public ThreadLocalContext() {
        path.set(new ArrayList<>());
    }

    @Override
    public void push(String property) {
        path.get().add(property);
    }

    @Override
    public void pop() {
        if (!path.get().isEmpty()) {
            path.get().remove(path.get().size() - 1);
        }
    }

    @Override
    public void register(List<String> propertyPath, Matcher matcher) {
        propertyToMatcher.put(propertyPath, matcher);        
    }

    @Override
    public Optional<Matcher> getCustomMatcher() {
        return Optional.ofNullable(propertyToMatcher.get(path.get()));
    }

}
