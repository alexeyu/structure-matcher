package nl.alexeyu.structmatcher;

import java.util.List;
import java.util.Optional;

import nl.alexeyu.structmatcher.matcher.Matcher;

public interface Context {

    void push(String property);

    void pop();
    
    <V> void register(List<String> propertyPath, Matcher<V> matcher);

    <V> Optional<Matcher<V>> getCustomMatcher();
    
}