package nl.alexeyu.structmatcher.matcher;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import nl.alexeyu.structmatcher.property.PropertyPathPattern;
import nl.alexeyu.structmatcher.property.PropertyPath;

@RunWith(Theories.class)
public class WildcardPathCheckerTest {
    
    private WildcardPathChecker checker = new WildcardPathChecker();

    @DataPoints("positive")
    public static PropertyPathPattern[] positiveExamples() {
        return new PropertyPathPattern[] {
            pattern("*"), pattern("a", "*"), pattern("a", "*", "d"),
            pattern("a", "b", "c", "d"), pattern("*", "c", "d"), 
            pattern("a", "*", "c", "*"), pattern("*", "*", "d"),
            pattern("*", "*", "*", "*", "*"),
        };
    }
    
    @Theory
    public void resolves(@FromDataPoints("positive") PropertyPathPattern pattern) {
        assertTrue(checker.test(pattern, new PropertyPath(asList("a", "b", "c", "d"))));
    }
    
    @DataPoints("negative")
    public static PropertyPathPattern[] negativeExamples() {
        return new PropertyPathPattern[] {
            pattern(""), pattern("a"), pattern("a", "b", "c"),
            pattern("a", "b", "d", "c"), pattern("*", "c"), 
            pattern("b", "*"), pattern("a", "b", "c", "d", "e")
        };
    }
    
    @Theory
    public void doesNotResolve(@FromDataPoints("negative") PropertyPathPattern pattern) {
        assertFalse(checker.test(pattern, new PropertyPath(asList("a", "b", "c", "d"))));
    }

    private static PropertyPathPattern pattern(String... elements) {
        return new PropertyPathPattern(asList(elements));
    }

}
