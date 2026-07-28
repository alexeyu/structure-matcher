package nl.alexeyu.structmatcher.matcher;

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
     * Accepts a string the regular expression matches in full.
     *
     * @param expr
     *            the regular expression both values are held against.
     */
    public static Matcher<String> regex(String expr) {
        return new MustConformMatcher<>(str -> Pattern.compile(expr).matcher(str).matches(),
                "The regular expression: " + expr);
    }

}
