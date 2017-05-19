package nl.alexeyu.structmatcher.matcher;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

import nl.alexeyu.structmatcher.Context;
import nl.alexeyu.structmatcher.Property;
import nl.alexeyu.structmatcher.ThreadLocalContext;
import nl.alexeyu.structmatcher.feedback.Feedback;

public class Matchers {
    
    private static final Context context = new ThreadLocalContext();
    
    public static void registerCustomMatcher(List<String> propertyPath, Matcher matcher) {
        context.register(propertyPath, matcher);
    }
    
    public static Matcher contextAware(Matcher defaultMatcher) {
        return new ContextAwareMatcher(context, defaultMatcher);
    }

    public static Matcher nullAware(Matcher nextMatcher) {
        return new NullAwareMatcher(nextMatcher);
    }

    public static Matcher anyValue() {
        return nullAware((p, e, a) -> Feedback.empty(p));
    }

    public static Matcher constant(Object alwaysExpected) {
        return (prop, exp, act) -> valuesEqual().match(prop, alwaysExpected, act);
    }

    public static Matcher valuesEqual() {
        return nullAware(new ValuesEqualMatcher());
    }

    public static Matcher listsEqual() {
        return new ListMatcher();
    }

    public static Matcher structuresEqual() {
        return nullAware(new StructureMatcher());
    }
    
    public static Matcher and(Matcher... matchers) {
        return new AndMatcher(matchers);
    }

    public static Matcher normalizing(Function<Object, Object> normalizer, Matcher delegate) {
        return (prop, exp, act) -> delegate.match(prop, exp, normalizer.apply(act));
    }

    public static Matcher normalizingBase(Function<Object, Object> normalizer, Matcher delegate) {
        return (prop, exp, act) -> delegate.match(prop, 
                normalizer.apply(exp), 
                act);
    }

    public static Matcher normalizingBoth(Function<Object, Object> normalizer, Matcher delegate) {
        return (prop, exp, act) -> delegate.match(prop, 
                normalizer.apply(exp), 
                normalizer.apply(act));
    }

    public static Matcher forProperty(Property property) {
        if (property.isList()) {
            return listsEqual();
        }
        if (property.isSimple()) {
            return valuesEqual();
        }
        return structuresEqual();

    }
    
    public static Matcher regex(String expr) {
        return new MustConformMatcher(
                v -> Pattern.compile(expr).matcher(String.valueOf(v)).matches(),
                "The regular expression: " + expr);
    }

    public static Matcher nonEmptyString() {
        return new MustConformMatcher(v -> !v.toString().isEmpty(), "Non-empty string");
    }

    public static Matcher nonNull() {
        return new MustConformMatcher(v -> v != null, "Any value");
    }

    static Matcher getNullAwareMatcher(Object obj) {
        return nullAware(forObject(obj));
    }

    static Matcher forObject(Object obj) {
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
