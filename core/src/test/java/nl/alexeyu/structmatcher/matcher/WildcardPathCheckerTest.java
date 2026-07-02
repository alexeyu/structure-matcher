package nl.alexeyu.structmatcher.matcher;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import nl.alexeyu.structmatcher.property.PropertyPath;
import nl.alexeyu.structmatcher.property.PropertyPathPattern;

public class WildcardPathCheckerTest {

    private WildcardPathChecker checker = new WildcardPathChecker();

    static PropertyPathPattern[] positiveExamples() {
        return new PropertyPathPattern[]{pattern("*"), pattern("a", "*"), pattern("a", "*", "d"),
                pattern("a", "b", "c", "d"), pattern("*", "c", "d"), pattern("a", "*", "c", "*"),
                pattern("*", "*", "d"), pattern("*", "*", "*", "*", "*"),};
    }

    @ParameterizedTest
    @MethodSource("positiveExamples")
    public void resolves(PropertyPathPattern pattern) {
        assertTrue(checker.test(pattern, new PropertyPath(asList("a", "b", "c", "d"))));
    }

    static PropertyPathPattern[] negativeExamples() {
        return new PropertyPathPattern[]{pattern(""), pattern("a"), pattern("a", "b", "c"),
                pattern("a", "b", "d", "c"), pattern("*", "c"), pattern("b", "*"),
                pattern("a", "b", "c", "d", "e")};
    }

    @ParameterizedTest
    @MethodSource("negativeExamples")
    public void doesNotResolve(PropertyPathPattern pattern) {
        assertFalse(checker.test(pattern, new PropertyPath(asList("a", "b", "c", "d"))));
    }

    private static PropertyPathPattern pattern(String... elements) {
        return new PropertyPathPattern(asList(elements));
    }

}
