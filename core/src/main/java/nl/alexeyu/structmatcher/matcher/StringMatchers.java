package nl.alexeyu.structmatcher.matcher;

import java.util.regex.Pattern;

/**
 * Factory of string-specific matchers. 
 */
public class StringMatchers {

    /**
     * Returns a strict matcher which ensures the values are not null or empty
     * strings. A base value must be a non-empty string, otherwise
     * <code>BrokenSpecificationException</code> will be thrown. The matcher
     * will return an empty feedback if an actual value is a non-empty string a
     * non-empty feedback otherwise.
     * 
     * @return a matcher with the behavior specified above.
     */
    public static Matcher<String> nonEmpty() {
        return Matchers.mustConform(str -> str != null && !str.isEmpty(), "A non-empty string");
    }

    /**
     * Returns a strict regular expression-based matcher. A base value must match a
     * given regular expression, otherwise
     * <code>BrokenSpecificationException</code> will be thrown. The matcher
     * will return an empty feedback if an actual value matches the regular
     * expression and a non-empty feedback otherwise.
     * 
     * @param expr a regular expression to match values against.
     * @return a matcher with the behavior specified above.
     */
    public static Matcher<String> regex(String expr) {
        return new MustConformMatcher<>(
                str -> Pattern.compile(expr).matcher(str).matches(),
                "The regular expression: " + expr);
    }

}
