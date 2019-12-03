package nl.alexeyu.structmatcher.matcher;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;

import nl.alexeyu.structmatcher.property.PropertyPathPattern;

public class DefaultMatchingStackTest {

    private MatchingStack<Object> context;
    
    private Matcher<?> customMatcher = Matchers.valuesEqual();

    private Matcher<Object> fallbackMatcher = Matchers.anyValue();
    
    @Before
    public void setUp() {
        context = new DefaultMatchingStack(new Object(), new Object(),
                singletonMap(new PropertyPathPattern(asList("a", "b")), customMatcher));
    }

    @Test
    public void matchersGetRegisteredAndCanBeReached() {
        assertSame(fallbackMatcher, context.push("a", () -> fallbackMatcher));
        assertSame(customMatcher, context.push("b", () -> fallbackMatcher));
    }

    @Test
    public void extraPopIsHarmless() {
        context.push("a", () -> null);
        context.pop();
        context.pop();
    }

}
