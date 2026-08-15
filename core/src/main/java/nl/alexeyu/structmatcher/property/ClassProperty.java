package nl.alexeyu.structmatcher.property;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Convenient wrapper of POJO properties. For a classic bean these are its getter methods; for a
 * {@code record} they are its components (whose accessors carry no {@code get}/{@code is} prefix).
 */
public final class ClassProperty implements Property {

    private static final List<String> HIDDEN_GETTERS = Arrays.asList("getClass");

    /** The accessor name breaks the tie when a bean has both {@code getFoo} and {@code isFoo}. */
    private static final Comparator<ClassProperty> BY_NAME =
            Comparator.comparing(ClassProperty::getName).thenComparing(p -> p.method.getName());

    private final Method method;

    /**
     * The declaration of {@link #method} that the library can call. It differs from
     * {@code method} when the model declares its accessor on a class that is not public. Only
     * {@link #getValue} reads it: the property takes its name and type from the declaration the
     * model made, so a covariant override keeps its own return type.
     */
    private final Method invocable;

    /**
     * Whether {@link #method} is a record component accessor (e.g. {@code name()}) rather than a
     * {@code get}/{@code is}-prefixed bean getter. Record accessors carry no prefix, so their
     * property name is derived by capitalization only.
     */
    private final boolean recordComponent;

    private ClassProperty(Method method, boolean recordComponent) {
        this.method = method;
        this.invocable = InvocableAccessors.resolve(method)
                .orElseThrow(() -> new InaccessibleAccessorException(method));
        this.recordComponent = recordComponent;
    }

    /**
     * Streams the properties of a class. A {@code record} yields its components, in declaration
     * order. Any other class yields its public getters, by name: a no-arg method whose name starts
     * with 'get' or 'is'. This skips <code>getClass()</code>, along with bridge and synthetic
     * methods.
     * <p>
     * A bridge survives when it is the only accessor of its name. That happens when the model
     * declares the getter on a class outside our reach, leaving the compiler's bridge as the one
     * way in. Drop it and the property goes with it, and a property that never runs contributes
     * no feedback, which reads as a match.
     * <p>
     * <code>Class.getMethods()</code> has no specified order, and that order reaches the JSON
     * rendering and the stored archives. Sorting keeps a batch stored on one JDK diffable against
     * a batch stored on another.
     */
    public static Stream<ClassProperty> forClass(Class<?> cl) {
        if (cl.isRecord()) {
            return Arrays.stream(cl.getRecordComponents()).map(RecordComponent::getAccessor)
                    .map(accessor -> new ClassProperty(accessor, true));
        }
        var accessors = Arrays.stream(cl.getMethods()).filter(ClassProperty::isAccessor).toList();
        var declaredNames = accessors.stream().filter(ClassProperty::isDeclared)
                .map(Method::getName).collect(Collectors.toSet());
        return accessors.stream()
                .filter(method -> isDeclared(method) || !declaredNames.contains(method.getName()))
                .map(method -> new ClassProperty(method, false)).sorted(BY_NAME);
    }

    public static Optional<ClassProperty> of(Method method) {
        return isAccessor(method) && isDeclared(method)
                ? Optional.of(new ClassProperty(method, false))
                : Optional.empty();
    }

    /**
     * Wraps an accessor {@link Method} as a property, inferring the {@link #recordComponent} flag
     * from the declaring class. Use this when the accessor is known directly (e.g. resolved from a
     * method reference) rather than discovered by scanning a class, so that {@link #getName()}
     * applies the same naming rules as {@link #forClass}.
     */
    public static ClassProperty forMethod(Method method) {
        return new ClassProperty(method, method.getDeclaringClass().isRecord());
    }

    /**
     * Returns the name of a property. For a bean getter the prefix is stripped: <code>getFoo</code>
     * and <code>isFoo</code> both yield 'Foo'. For a record component the accessor name is used
     * as-is: <code>foo()</code> yields 'Foo'.<br/>
     * <b>Note:</b> with camel-case method names, a property name starts with a capital either way,
     * so a bean and a record produce the same path.
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
     * Reads this property off an object by calling its accessor.
     *
     * @param obj
     *            the object to read the property from.
     * @throws IllegalStateException
     *             if the accessor call fails.
     */
    @Override
    public Object getValue(Object obj) {
        try {
            return invocable.invoke(obj);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new IllegalStateException("Could not invoke " + method.getName() + " for " + obj,
                    e);
        }
    }

    /**
     * Whether the property is simple, which covers these types:
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
     * Whether the declared type implements <code>java.util.List</code>. Mind the direction: a
     * concrete <code>ArrayList</code> counts, a supertype (<code>Collection</code>,
     * <code>Object</code>) does not.
     */
    @Override
    public boolean isList() {
        return List.class.isAssignableFrom(method.getReturnType());
    }

    /**
     * Whether the declared type implements <code>java.util.Map</code>. @see #isList()
     */
    @Override
    public boolean isMap() {
        return Map.class.isAssignableFrom(method.getReturnType());
    }

    /**
     * Whether the declared type implements <code>java.util.Set</code>. @see #isList()
     */
    @Override
    public boolean isSet() {
        return Set.class.isAssignableFrom(method.getReturnType());
    }

    /**
     * Whether the property is an array, of objects or of primitives.
     */
    @Override
    public boolean isArray() {
        return method.getReturnType().isArray();
    }

    /**
     * Whether the property is an {@link Optional}. <code>Optional</code> is final, so this checks
     * the exact type rather than assignability.
     */
    @Override
    public boolean isOptional() {
        return method.getReturnType().equals(Optional.class);
    }

    public static boolean isSimple(Class<?> cl) {
        return String.class.isAssignableFrom(cl) || Number.class.isAssignableFrom(cl)
                || Boolean.class.isAssignableFrom(cl) || Character.class.isAssignableFrom(cl)
                || cl.isEnum() || cl.isPrimitive();
    }

    private static boolean isAccessor(Method method) {
        return nameMatches(method) && parametersMatch(method) && isNotDenylisted(method);
    }

    /**
     * Whether the model declared the method itself. The bridge of a covariant accessor declares
     * the erased return type, so keeping it alongside the real one would duplicate the property
     * and type it as <code>Object</code>.
     */
    private static boolean isDeclared(Method method) {
        return !method.isBridge() && !method.isSynthetic();
    }

    private static boolean nameMatches(Method method) {
        return isGetMethod(method) || isIsMethod(method);
    }

    private static boolean parametersMatch(Method method) {
        return method.getParameterCount() == 0;
    }

    private static boolean isNotDenylisted(Method method) {
        return !HIDDEN_GETTERS.contains(method.getName());
    }

    private static boolean isGetMethod(Method method) {
        return method.getName().startsWith("get");
    }

    private static boolean isIsMethod(Method method) {
        return method.getName().startsWith("is");
    }

}
