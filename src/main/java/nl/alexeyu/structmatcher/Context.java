package nl.alexeyu.structmatcher;

import java.util.List;
import java.util.Optional;

import nl.alexeyu.structmatcher.matcher.Matcher;

public interface Context {

    void push(String property);

    void pop();
    
    void register(List<String> propertyPath, Matcher matcher);

    Optional<Matcher> getCustomMatcher();

}