package nl.alexeyu.structmatcher.matcher;

import java.util.function.Function;
import java.util.regex.Pattern;

import nl.alexeyu.structmatcher.Property;
import nl.alexeyu.structmatcher.feedback.Feedback;

public class Matchers {
    
    private static final Context context = new ThreadLocalContext();
    
    public static <V> void registerCustomMatcher(Matcher<V> matcher, String... propertyPath) {
        context.register(matcher, propertyPath);
    }
    
    public static <V> Matcher<V> contextAware(Matcher<V> defaultMatcher) {
        return new ContextAwareMatcher<>(context, defaultMatcher);
    }

    public static <V> Matcher<V> nullAware(Matcher<V> nextMatcher) {
        return new NullAwareMatcher<>(nextMatcher);
    }

    public static <V> Matcher<V> anyValue() {
        return nullAware((p, e, a) -> Feedback.empty(p));
    }

    public static <V> Matcher<V> constant(V alwaysExpected) {
        return (prop, exp, act) -> valuesEqual().match(prop, alwaysExpected, act);
    }

    public static <V> Matcher<V> valuesEqual() {
        return nullAware(new ValuesEqualMatcher<>());
    }

    public static ListMatcher listsEqual() {
        return new ListMatcher();
    }

    public static <V> Matcher<V> structuresEqual() {
        return nullAware(new StructureMatcher<>());
    }
    
    @SafeVarargs
    public static <V> Matcher<V> and(Matcher<V>... matchers) {
        return new AndMatcher<>(matchers);
    }

    public static <V> Matcher<V> normalizing(Function<V, V> normalizer, Matcher<V> delegate) {
        return (prop, exp, act) -> delegate.match(prop, exp, normalizer.apply(act));
    }

    public static <V> Matcher<V>  normalizingBase(Function<V, V> normalizer, Matcher<V>  delegate) {
        return (prop, exp, act) -> delegate.match(prop, 
                normalizer.apply(exp), 
                act);
    }

    public static <V> Matcher<V> normalizingBoth(Function<V, V> normalizer, Matcher<V> delegate) {
        return (prop, exp, act) -> delegate.match(prop, 
                normalizer.apply(exp), 
                normalizer.apply(act));
    }

    @SuppressWarnings("rawtypes")
    public static Matcher forProperty(Property property) {
        if (property.isList()) {
            return listsEqual();
        }
        if (property.isSimple()) {
            return valuesEqual();
        }
        return structuresEqual();

    }
    
    public static Matcher<String> regex(String expr) {
        return new MustConformMatcher<>(
                str -> Pattern.compile(expr).matcher(str).matches(),
                "The regular expression: " + expr);
    }

    public static Matcher<String> nonEmptyString() {
        return new MustConformMatcher<>(str -> !str.toString().isEmpty(), "Non-empty string");
    }

    public static <V> Matcher<V> nonNull() {
        return new MustConformMatcher<>(v -> v != null, "Any value");
    }

    static <V> Matcher<V> getNullAwareMatcher(V obj) {
        return nullAware(forObject(obj));
    }

    static <V> Matcher<V> forObject(V obj) {
        if (obj == null) {
             // Should not happen due to the null-aware matcher logic
            return (p, e, a) -> { throw new NullPointerException("Cannot match null value.");};
        }
        if (Property.isSimple(obj.getClass())) {
            return valuesEqual();
        }
        return structuresEqual();
    }

}
