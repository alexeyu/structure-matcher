package nl.alexeyu.structmatcher.matcher;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

import nl.alexeyu.structmatcher.property.PropertyPath;
import nl.alexeyu.structmatcher.property.PropertyPathPattern;

final class DefaultMatchingStack<T> implements MatchingStack<T> {

    public static final MatchingStack<Object> BARE = new DefaultMatchingStack<>(new Object(),
            new Object(), Collections.emptyMap());

    private final PropertyPath path = new PropertyPath();

    private final CustomMatcherResolver customMatcherResolver;

    private final T expected;

    private final T actual;

    public DefaultMatchingStack(T expected, T actual,
            Map<PropertyPathPattern, Matcher<Object>> propertyToMatcher) {
        this.expected = expected;
        this.actual = actual;
        this.customMatcherResolver = new WildcardMatcherResolver(propertyToMatcher);
    }

    @Override
    public Matcher<Object> push(String property, Supplier<Matcher<Object>> fallbackSupplier) {
        path.push(property);
        var maybeMatcher = customMatcherResolver.forPath(path);
        if (maybeMatcher.isPresent()) {
            return maybeMatcher.get();
        }
        return withMaskingOfWholeValues(fallbackSupplier.get());
    }

    /**
     * A default matcher that stops at this node - one side null, the two clashing - reports the
     * whole value in one leaf, and the rule masking a field below it never runs. Where something
     * below is masked, the ancestor withholds it too.
     */
    private Matcher<Object> withMaskingOfWholeValues(Matcher<Object> matcher) {
        return customMatcherResolver.maskerBelow(path)
                .map(masker -> Masking.maskingWholeValues(matcher, masker)).orElse(matcher);
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
