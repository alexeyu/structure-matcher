package nl.alexeyu.structmatcher.matcher;

import java.util.List;
import java.util.Optional;

public interface CustomMatchers {
    
    void register(List<String> propertyPath, Matcher matcher);

    Optional<Matcher> get(List<String> propertyPath);

}
