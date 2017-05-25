package nl.alexeyu.structmatcher.matcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;

public class ThreadLocalContextTest {

    private Context context;
    
    private Matcher<?> customMatcher = Matchers.anyValue();
    
    @Before
    public void setUp() {
        context = new ThreadLocalContext();
    }

    @Test
    public void matchersGetRegisteredAndCanBeReached() {
        context.register(customMatcher, "a", "b");
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
