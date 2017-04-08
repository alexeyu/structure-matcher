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
    
    public static PartialMatcher nullAwareMatcher() {
        return new NullAwareMatcher();
    }

    public static Matcher propertyMatcher() {
        return new ContextAwareMatcher(context, new SimplePropertyMatcher());
    }

    public static Matcher listMatcher() {
        return new ContextAwareMatcher(context, new ListMatcher());
    }

    public static Matcher structureMatcher() {
        return new ContextAwareMatcher(context, new StructureMatcher());
    }
    
    public static Matcher getMatcher(Property property) {
        if (property.isList()) {
            return listMatcher();
        }
        if (property.isSimple()) {
            return propertyMatcher();
        }
        return structureMatcher();

    }

    public static Matcher getMatcher(Class<?> cl) {
        if (Property.isSimple(cl)) {
            return propertyMatcher();
        }
        return structureMatcher();
    }

}
