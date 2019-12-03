package nl.alexeyu.structmatcher.matcher;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import nl.alexeyu.structmatcher.property.PropertyPathPattern;
import nl.alexeyu.structmatcher.property.PropertyPath;

final class DefaultMatchingStack<T> implements MatchingStack<T> {

    public static final MatchingStack<Object> BARE = new DefaultMatchingStack<>(
            new Object(), new Object(), Collections.emptyMap());

    private final PropertyPath path = new PropertyPath();

    private final CustomMatcherResolver customMatcherResolver;

    private final T expected;

    private final T actual;

    public DefaultMatchingStack(T expected, T actual, Map<PropertyPathPattern, Matcher<Object>> propertyToMatcher) {
        this.expected = expected;
        this.actual = actual;
        this.customMatcherResolver = new WildcardMatcherResolver(propertyToMatcher);
    }

    @Override
    public Matcher<Object> push(String property, Supplier<Matcher<Object>> fallbackSupplier) {
        path.push(property);
        Optional<Matcher<Object>> maybeMatcher = customMatcherResolver.forPath(path);
        return maybeMatcher.orElseGet(fallbackSupplier);
    }

    @Override
    public void pop() {
        path.pop();
    }

    @Override
    public T getBaseStructure() {
        return expected;
    }

    @Override
    public T getActualStructure() {
        return actual;
    }
}
