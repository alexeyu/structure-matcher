package nl.alexeyu.structmatcher.property;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * The path a custom matcher was registered for, tested against the {@link PropertyPath} the
 * comparison has reached. A segment may be the wildcard <code>*</code>, any run of properties.
 */
public final class PropertyPathPattern extends AbstractPath {

    /**
     * Orders the patterns that match one path, most specific first: fewest wildcards, then most
     * literal segments, then the longest run of literals before the first wildcard. The pattern
     * text settles the rest, so no rule wins by hash order.
     */
    public static final Comparator<PropertyPathPattern> MOST_SPECIFIC_FIRST = Comparator
            .comparingInt(PropertyPathPattern::wildcardCount)
            .thenComparing(Comparator.comparingInt(PropertyPathPattern::literalCount).reversed())
            .thenComparing(
                    Comparator.comparingInt(PropertyPathPattern::literalPrefixLength).reversed())
            .thenComparing(PropertyPathPattern::toString);

    PropertyPathPattern(String... elements) {
        this(Arrays.asList(elements));
    }

    public PropertyPathPattern(List<String> list) {
        super(list);
    }

    public boolean startsWithWildcard() {
        return !isEmpty() && isWildcard(head());
    }

    private static boolean isWildcard(String s) {
        return "*".equals(s);
    }

    public boolean isPositive() {
        return list.stream().allMatch(PropertyPathPattern::isWildcard);
    }

    private int wildcardCount() {
        return (int) list.stream().filter(PropertyPathPattern::isWildcard).count();
    }

    private int literalCount() {
        return list.size() - wildcardCount();
    }

    private int literalPrefixLength() {
        var wildcard = list.indexOf("*");
        return wildcard < 0 ? list.size() : wildcard;
    }

    public boolean headsMatch(PropertyPath path) {
        return head().equals(path.head());
    }

    public PropertyPathPattern tail() {
        checkNotEmpty();
        return new PropertyPathPattern(list.subList(1, list.size()));
    }

}
