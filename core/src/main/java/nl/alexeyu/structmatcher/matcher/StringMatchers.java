package nl.alexeyu.structmatcher.matcher;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Factory of string-specific matchers. Both are strict, in the sense {@link MustConformMatcher}
 * defines: an actual value that fails the test produces non-empty feedback, while a base value that
 * fails it throws <code>BrokenSpecificationException</code>.
 */
public class StringMatchers {

    private StringMatchers() {
    }

    /**
     * Accepts a string that is neither null nor empty. Chained ahead of another matcher with
     * {@link Matcher#and}, it short-circuits, so the next matcher never sees an empty string.
     */
    public static Matcher<String> nonEmpty() {
        return Matchers.mustConform(str -> str != null && !str.isEmpty(), "A non-empty string");
    }

    /**
     * Accepts a string the regular expression matches in full. Null matches nothing, so a null
     * actual value yields feedback and a null base value throws, the split
     * {@link MustConformMatcher} applies to any condition.
     *
     * @param expr
     *            the regular expression both values must match. This factory compiles it once, so
     *            a malformed expression fails here instead of at the first comparison.
     * @throws java.util.regex.PatternSyntaxException
     *             if the expression does not compile.
     */
    public static Matcher<String> regex(String expr) {
        var pattern = Pattern.compile(Objects.requireNonNull(expr, "Regular expression is null"));
        return new MustConformMatcher<>(str -> str != null && pattern.matcher(str).matches(),
                "The regular expression: " + expr);
    }

}
