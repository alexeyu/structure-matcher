package nl.alexeyu.structmatcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import nl.alexeyu.structmatcher.matcher.ExpectAnyValueMatcher;
import nl.alexeyu.structmatcher.matcher.Matcher;

public class ContextTest {

    private Context context;
    
    private Matcher customMatcher = new ExpectAnyValueMatcher();
    
    @Before
    public void setUp() {
        context = new ThreadLocalContext();
    }

    @Test
    public void matchersGetRegisteredAndCanBeReached() {
        context.register(Arrays.asList("a", "b"), customMatcher);
        assertFalse(context.getCustomMatcher().isPresent());
        context.push("a");
        assertFalse(context.getCustomMatcher().isPresent());
        context.push("b");
        assertSame(context.getCustomMatcher().get(), customMatcher);
        context.pop();
        assertFalse(context.getCustomMatcher().isPresent());
    }

    @Test
    public void extraPopIsHarmless() {
        context.push("a");
        context.pop();
        context.pop();
    }

}
