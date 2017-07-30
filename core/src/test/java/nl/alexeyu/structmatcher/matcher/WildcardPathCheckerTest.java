package nl.alexeyu.structmatcher.matcher;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

@RunWith(Theories.class)
public class WildcardPathCheckerTest {
    
    private WildcardPathChecker checker = new WildcardPathChecker();

    @DataPoints("positive")
    @SuppressWarnings("rawtypes")
    public static List[] positiveExamples() {
        return new List[] {
            asList("*"), asList("a", "*"), asList("a", "*", "d"),
            asList("a", "b", "c", "d"), asList("*", "c", "d"), 
            asList("a", "*", "c", "*"), asList("*", "*", "d"),
            asList("*", "*", "*", "*", "*"),
        };
    }
    
    @Theory
    public void resolves(@FromDataPoints("positive") List<String> path) {
        assertTrue(checker.test(path, asList("a", "b", "c", "d")));
    }
    
    @DataPoints("negative")
    @SuppressWarnings("rawtypes")
    public static List[] negativeExamples() {
        return new List[] {
            asList(""), asList("a"), asList("a", "b", "c"),
            asList("a", "b", "d", "c"), asList("*", "c"), 
            asList("b", "*"), asList("a", "b", "c", "d", "e")
        };
    }
    
    @Theory
    public void doesNotResolve(@FromDataPoints("negative") List<String> path) {
        assertFalse(checker.test(path, asList("a", "b", "c", "d")));
    }

}
