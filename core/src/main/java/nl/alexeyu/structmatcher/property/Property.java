package nl.alexeyu.structmatcher.property;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
     * Whether a value has the shape this property's declared type promises. The base structure's
     * declaration picks the default matcher, while the value comes off the actual structure, which
     * may keep the accessor name and change the type: a list matcher handed a string throws
     * {@code ClassCastException}. The matcher factory asks this before it delegates.
     * <p>
     * Only a list, a map, a set, an array and an {@link Optional} have a matcher that the wrong
     * type breaks. A simple value compares by <code>equals</code>, and a structure reports the
     * clash itself, so both take any type.
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
