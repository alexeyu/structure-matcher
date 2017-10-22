package nl.alexeyu.structmatcher.matcher;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;

import nl.alexeyu.structmatcher.property.PropertyPathPattern;

public class DefaultMatchingStackTest {

    private MatchingStack context;
    
    private Matcher<?> customMatcher = Matchers.anyValue();
    
    @Before
    public void setUp() {
        context = new DefaultMatchingStack(
                singletonMap(new PropertyPathPattern(asList("a", "b")), customMatcher));
    }

    @Test
    public void matchersGetRegisteredAndCanBeReached() {
        assertFalse(context.push("a").isPresent());
        assertSame(context.push("b").get(), customMatcher);
    }

    @Test
    public void extraPopIsHarmless() {
        context.push("a");
        context.pop();
        context.pop();
    }

}
