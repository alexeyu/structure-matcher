package nl.alexeyu.structmatcher.matcher;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
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
    
    public static Matcher nullAware(Supplier<Matcher> nextMatcher) {
        return new NullAwareMatcher(nextMatcher);
    }

    public static Matcher anyValue() {
        return new ContextAwareMatcher(context, 
                (prop, exp, act) -> nullAware(() -> ((p, e, a) -> Feedback.empty(p))).match(prop, exp, act));
    }

    public static Matcher constant(Object alwaysExpected) {
        return new ContextAwareMatcher(context, 
                (prop, exp, act) -> nullAware(() -> new SimplePropertyMatcher()).match(prop, alwaysExpected, act));
    }

    public static Matcher propertyEquals() {
        return new ContextAwareMatcher(context, 
                (prop, exp, act) -> nullAware(() -> new SimplePropertyMatcher()).match(prop, exp, act));
    }

    public static Matcher listsEqual() {
        return new ContextAwareMatcher(context, new ListMatcher());
    }

    public static Matcher structuresEqual() {
        return new ContextAwareMatcher(context, 
                (prop, exp, act) -> nullAware(() -> new StructureMatcher()).match(prop, exp, act));
    }

    public static Matcher and(Matcher... matchers) {
        return new ContextAwareMatcher(context, new AndMatcher(matchers));
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

    public static Matcher getMatcher(Property property) {
        if (property.isList()) {
            return listsEqual();
        }
        if (property.isSimple()) {
            return propertyEquals();
        }
        return structuresEqual();

    }
    
    public static Matcher regex(String expr) {
        return new MustConformMatcher(
                v -> Pattern.compile(expr).matcher(String.valueOf(v)).matches(),
                "The regular expression: " + expr);
    }

    public static Matcher nonEmptyString() {
        return new MustConformMatcher(v -> !v.toString().isEmpty(),
                "Non-empty string");
    }

    public static Matcher nonNull() {
        return new MustConformMatcher(v -> v != null, "Any value");
    }

    public static Matcher getNullAwareMatcher(Object obj) {
        return nullAware(() -> getMatcher(obj.getClass()));
    }

    public static Matcher getMatcher(Class<?> cl) {
        if (Property.isSimple(cl)) {
            return propertyEquals();
        }
        return structuresEqual();
    }

}
