package nl.alexeyu.structmatcher.property;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * One property of a structure: a name, a value read off a given object, and the shape that picks
 * its default matcher. {@link ClassProperty} reads a getter or a record component.
 */
public interface Property {

    String getName();

    Object getValue(Object obj);

    boolean isList();

    boolean isMap();

    boolean isSet();

    boolean isArray();

    boolean isOptional();

    boolean isSimple();

    /**
     * Whether a value fits the shape this property's declared type promises. The default matcher
     * comes from the base declaration and the value from the actual structure, so a list matcher
     * can meet a string; the matcher factory asks this before it delegates. Only a list, a map, a
     * set, an array and an {@link Optional} have a matcher the wrong type breaks.
     */
    default boolean fitsDeclaredShape(Object value) {
        if (value == null) {
            return true;
        }
        if (isList()) {
            return value instanceof List;
        }
        if (isMap()) {
            return value instanceof Map;
        }
        if (isSet()) {
            return value instanceof Set;
        }
        if (isArray()) {
            return value.getClass().isArray();
        }
        if (isOptional()) {
            return value instanceof Optional;
        }
        return true;
    }
}
