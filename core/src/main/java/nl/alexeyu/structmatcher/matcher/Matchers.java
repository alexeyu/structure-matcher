package nl.alexeyu.structmatcher.matcher;

import java.util.List;
import java.util.function.Supplier;

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

    public static Matcher expectAnyValue() {
        return new ContextAwareMatcher(context, 
                (prop, exp, act) -> nullAware(() -> ((p, e, a) -> Feedback.empty(p))).match(prop, exp, act));
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

    public static Matcher getMatcher(Property property) {
        if (property.isList()) {
            return listsEqual();
        }
        if (property.isSimple()) {
            return propertyEquals();
        }
        return structuresEqual();

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
