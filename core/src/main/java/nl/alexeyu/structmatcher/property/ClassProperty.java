package nl.alexeyu.structmatcher.property;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Convenient wrapper of POJO properties. For a classic bean these are its getter
 * methods; for a {@code record} they are its components (whose accessors carry no
 * {@code get}/{@code is} prefix).
 */
public final class ClassProperty implements Property {

    private static final List<String> HIDDEN_GETTERS = Arrays.asList("getClass");

    private final Method method;

    /**
     * Whether {@link #method} is a record component accessor (e.g. {@code name()})
     * rather than a {@code get}/{@code is}-prefixed bean getter. Record accessors
     * carry no prefix, so their property name is derived by capitalization only.
     */
    private final boolean recordComponent;

    private ClassProperty(Method method, boolean recordComponent) {
        this.method = method;
        this.recordComponent = recordComponent;
    }

    /**
     * Provides stream of properties of a given class. For a {@code record} the
     * properties are its components, in declaration order. For any other class they
     * are derived from its public getter methods: a method is considered a getter if
     * its name starts with 'get' or 'is' and it takes no parameters. The method
     * <code>getClass()</code> is ignored.
     */
    public static Stream<ClassProperty> forClass(Class<?> cl) {
        if (cl.isRecord()) {
            return Arrays.stream(cl.getRecordComponents())
                    .map(RecordComponent::getAccessor)
                    .map(accessor -> new ClassProperty(accessor, true));
        }
        return Arrays.stream(cl.getMethods())
                .map(ClassProperty::of)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    public static Optional<ClassProperty> of(Method method) {
        return isValid(method)
                ? Optional.of(new ClassProperty(method, false))
                : Optional.empty();
    }

    /**
     * Returns the name of a property. For a bean getter the prefix is stripped:
     * <code>getFoo</code> and <code>isFoo</code> both yield 'Foo'. For a record
     * component the accessor name is used as-is: <code>foo()</code> yields 'Foo'.<br/>
     * <b>Note:</b> provided the method names are in the camel case, property
     * name will always start with a capital letter, regardless of the source.
     */
    @Override
    public String getName() {
        if (recordComponent) {
            return capitalize(method.getName());
        }
        if (isGetMethod(method)) {
            return method.getName().substring(3);
        }
        if (isIsMethod(method)) {
            return method.getName().substring(2);
        }
        return method.getName();
    }

    private static String capitalize(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * Gets the value of this property for a given object by calling the
     * propertie's get method.
     *
     * @param obj an object to get the property value from.
     * @return a value of a property.
     * @throws IllegalStateException if there was an exception on the method call.
     */
    @Override
    public Object getValue(Object obj) {
        try {
            return method.invoke(obj);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new IllegalStateException("Could not invoke " + method.getName() + " for " + obj, e);
        }
    }

    /**
     * Tells if a property is simple. Properties of the following types are
     * considered simple:
     * <ul>
     * <li>all primitive types
     * <li>enumerations
     * <li><code>Character</code>
     * <li><code>Number</code>and its descendants
     * <li><code>Boolean</code>
     * <li><code>String</code>
     * </ul>
     */
    @Override
    public boolean isSimple() {
        return isSimple(method.getReturnType());
    }

    /**
     * Tells if a property a list (implements the
     * <code>java.util.list.List</code> interface).
     */
    @Override
    public boolean isList() {
        return method.getReturnType().isAssignableFrom(List.class);
    }

    /**
     * Tells if a property is a map (implements the
     * <code>java.util.Map</code> interface).
     */
    @Override
    public boolean isMap() {
        return method.getReturnType().isAssignableFrom(Map.class);
    }

    /**
     * Tells if a property is a set (implements the
     * <code>java.util.Set</code> interface).
     */
    @Override
    public boolean isSet() {
        return method.getReturnType().isAssignableFrom(Set.class);
    }

    /**
     * Tells if a property is an array (of objects or of primitives).
     */
    @Override
    public boolean isArray() {
        return method.getReturnType().isArray();
    }

    /**
     * Tells if a property is an {@link Optional}. <code>Optional</code> is final, so
     * this is an exact type check rather than an "is-assignable" one.
     */
    @Override
    public boolean isOptional() {
        return method.getReturnType().equals(Optional.class);
    }

    public static boolean isSimple(Class<?> cl) {
        return String.class.isAssignableFrom(cl)
                || Number.class.isAssignableFrom(cl)
                || Boolean.class.isAssignableFrom(cl)
                || Character.class.isAssignableFrom(cl)
                || cl.isEnum()
                || cl.isPrimitive();
    }

    private static boolean isValid(Method method) {
        return nameMatches(method) && parametersMatch(method) && isNotBlacklisted(method);
    }

    private static boolean nameMatches(Method method) {
        return isGetMethod(method) || isIsMethod(method);
    }

    private static boolean parametersMatch(Method method) {
        return method.getParameterCount() == 0;
    }

    private static boolean isNotBlacklisted(Method method) {
        return !HIDDEN_GETTERS.contains(method.getName());
    }

    private static boolean isGetMethod(Method method) {
        return method.getName().startsWith("get");
    }

    private static boolean isIsMethod(Method method) {
        return method.getName().startsWith("is");
    }

}