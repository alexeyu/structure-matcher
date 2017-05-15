package nl.alexeyu.structmatcher.matcher;

import java.util.regex.Pattern;

public class PredicateMatchers {
    
    private PredicateMatchers() {
    }

    public static Matcher regex(String expr) {
        return new PredicateMatcher(
                v -> Pattern.compile(expr).matcher(String.valueOf(v)).matches(),
                "The regular expression: " + expr);
    }

    public static Matcher nonEmptyString() {
        return new PredicateMatcher(v -> !v.toString().isEmpty(),
                "Non-empty string");
    }

    public static Matcher nonNull() {
        return new PredicateMatcher(v -> v != null, "Any value");
    }

}
