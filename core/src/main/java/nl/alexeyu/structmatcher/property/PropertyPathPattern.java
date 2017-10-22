package nl.alexeyu.structmatcher.property;

import java.util.Arrays;
import java.util.List;

public final class PropertyPathPattern extends AbstractPath {

    PropertyPathPattern(String... elements) {
        this(Arrays.asList(elements));
    }

    public PropertyPathPattern(List<String> list) {
        super(list);
    }

    public boolean startsWithWildcard() {
        return !isEmpty() && isWildcard(head());
    }

    private boolean isWildcard(String s) {
        return "*".equals(s);
    }

    public boolean isPositive() {
        return list.stream().allMatch(this::isWildcard);
    }

    public boolean headsMatch(PropertyPath path) {
        return head().equals(path.head());
    }

    public PropertyPathPattern tail() {
        checkNotEmpty();
        return new PropertyPathPattern(list.subList(1, list.size()));
    }
    
}
