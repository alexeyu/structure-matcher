package nl.alexeyu.structmatcher.matcher;

import nl.alexeyu.structmatcher.Property;

public class Matchers {
    
    public static PartialMatcher nullAwareMatcher() {
        return new NullAwareMatcher();
    }

    public static Matcher propertyMatcher() {
        return new SimplePropertyMatcher();
    }

    public static Matcher listMatcher() {
        return new ListMatcher();
    }

    public static Matcher structureMatcher() {
        return new StructureMatcher();
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
