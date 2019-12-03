package nl.alexeyu.structmatcher.property;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Convenient wrapper of POJO properties (which are represented by getter methods).
 */
public final class ClassProperty implements Property {

    private static final List<String> HIDDEN_GETTERS = Arrays.asList("getClass");

    private final Method method;

    private ClassProperty(Method method) {
        this.method = method;
    }

    /**
     * Provides stream of properties of a given class. Properties are derived by
     * looking at public getter methods of the class. A method considered as a
     * getter if its name starts from 'get' or 'is' and it does not take any
     * parameters. The method <code>getClass()</code> is ignored.
     */
    public static Stream<ClassProperty> forClass(Class<?> cl) {
        return Arrays.stream(cl.getMethods())
                .map(ClassProperty::of)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    public static Optional<ClassProperty> of(Method method) {
        return isValid(method)
                ? Optional.of(new ClassProperty(method))
                : Optional.empty();
    }

    /**
     * Returns the name of a property derive from the name of its getter method.
     * If the method name is <code>getFoo</code>, the property name would be
     * 'Foo'. If the method name is <code>isFoo</code>, the property name would
     * be 'Foo' as well.<br/>
     * <b>Note:</b> provided the method names are in the camel case, property
     * name will always start with a capital letter.
     */
    @Override
    public String getName() {
        if (isGetMethod(method)) {
            return method.getName().substring(3);
        }
        if (isIsMethod(method)) {
            return method.getName().substring(2);
        }
        return method.getName();
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