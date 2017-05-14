package nl.alexeyu.structmatcher.matcher;

import java.util.List;

import nl.alexeyu.structmatcher.Context;
import nl.alexeyu.structmatcher.Property;
import nl.alexeyu.structmatcher.ThreadLocalContext;

public class Matchers {
    
    private static final Context context = new ThreadLocalContext();
    
    public static void registerCustomMatcher(List<String> propertyPath, Matcher matcher) {
        context.register(propertyPath, matcher);
    }
    
    public static PartialMatcher nullAware() {
        return new NullAwareMatcher();
    }

    public static Matcher propertyEquals() {
        return new ContextAwareMatcher(context, new SimplePropertyMatcher());
    }

    public static Matcher listsEqual() {
        return new ContextAwareMatcher(context, new ListMatcher());
    }

    public static Matcher structuresEqual() {
        return new ContextAwareMatcher(context, new StructureMatcher());
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

    public static Matcher getMatcher(Class<?> cl) {
        if (Property.isSimple(cl)) {
            return propertyEquals();
        }
        return structuresEqual();
    }

}
