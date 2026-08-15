package nl.alexeyu.structmatcher.matcher;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import nl.alexeyu.structmatcher.property.PropertyPath;
import nl.alexeyu.structmatcher.property.PropertyPathPattern;

/**
 * Several registered patterns can match one property. The resolver hands back the most specific,
 * and a named segment counts for more than a wildcard.
 */
public class WildcardMatcherResolverTest {

    private final Map<PropertyPathPattern, Matcher<Object>> registrations = new HashMap<>();

    @Test
    public void anExactPathBeatsAWildcard() {
        var exact = register("Root", "A", "Url");
        register("Root", "*", "Url");
        assertSame(exact, resolve("Root", "A", "Url"));
    }

    @Test
    public void moreNamedSegmentsWin() {
        var anchored = register("Root", "*", "Url");
        register("*", "Url");
        assertSame(anchored, resolve("Root", "A", "Url"));
    }

    /** Both hold one wildcard and two names, so the one anchored deeper on the left wins. */
    @Test
    public void aLongerRunOfNamesBeforeTheWildcardWins() {
        var deeper = register("Root", "A", "*");
        register("Root", "*", "Url");
        assertSame(deeper, resolve("Root", "A", "Url"));
    }

    @Test
    public void aPathNoPatternMatchesResolvesToNothing() {
        register("Root", "A", "Url");
        assertTrue(new WildcardMatcherResolver(registrations)
                .forPath(new PropertyPath(asList("Root", "B", "Name"))).isEmpty());
    }

    private Matcher<Object> register(String... pattern) {
        Matcher<Object> matcher = Matchers.anyValue();
        registrations.put(new PropertyPathPattern(asList(pattern)), matcher);
        return matcher;
    }

    private Matcher<Object> resolve(String... path) {
        return new WildcardMatcherResolver(registrations).forPath(new PropertyPath(asList(path)))
                .orElseThrow();
    }

}
