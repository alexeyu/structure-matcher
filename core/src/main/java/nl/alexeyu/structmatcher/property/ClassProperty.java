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
     * The declaration of {@link #method} that the library can call, which differs from
     * {@code method} when the model declares its accessor on a class that is not public. Only
     * {@link #getValue} reads it: name and type come from the declaration the model made, so a
     * covariant override keeps its own return type. Resolved on first read rather than in the
     * constructor, since an unreadable property still has a name to register a matcher under.
     */
    private Method invocable;

    /**
     * Whether {@link #method} is a record component accessor (e.g. {@code name()}) rather than a
     * bean getter. Carrying no prefix, it names its property by capitalization only.
     */
    private final boolean recordComponent;

    private ClassProperty(Method method, boolean recordComponent) {
        this.method = method;
        this.recordComponent = recordComponent;
    }

    /**
     * Streams the properties of a class: a {@code record} yields its components in declaration
     * order, any other class its public no-arg <code>get</code>/<code>is</code> methods, without
     * <code>getClass()</code> and sorted by name. The sorting matters because
     * <code>Class.getMethods()</code> has no specified order, which would otherwise reach the
     * feedback tree, the JSON rendering and the stored archives.
     * <p>
     * Bridge and synthetic accessors drop out, unless a bridge is the only one of its name: the
     * model then declares the getter out of our reach, and dropping the property would read as a
     * match.
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
     * from the declaring class. Use this for an accessor known directly (e.g. resolved from a
     * method reference), so it is named by the same rules as one {@link #forClass} discovers.
     */
    public static ClassProperty forMethod(Method method) {
        return new ClassProperty(method, method.getDeclaringClass().isRecord());
    }

    /**
     * Returns the name of a property. A bean getter loses its prefix, so <code>getFoo</code> and
     * <code>isFoo</code> both yield 'Foo'; a record component only gains a capital, so
     * <code>foo()</code> yields 'Foo' too. With camel-case names a bean and a record therefore
     * produce the same path.
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
     * @throws IllegalStateException
     *             if the accessor call fails.
     * @throws InaccessibleAccessorException
     *             if no declaration of the accessor is one the library can call.
     */
    @Override
    public Object getValue(Object obj) {
        try {
            return accessorFor(obj).invoke(obj);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new IllegalStateException("Could not invoke " + method.getName() + " for " + obj,
                    e);
        }
    }

    /**
     * The declaration to call on this object. Properties are discovered off the base structure, so
     * the actual structure can be another implementation of the same type, one the base's own
     * declaration cannot be invoked on. Such an object is read through its own accessor of the
     * same name.
     */
    private Method accessorFor(Object obj) {
        var accessor = invocable();
        if (obj == null || accessor.getDeclaringClass().isInstance(obj)) {
            return accessor;
        }
        return invocableOn(obj.getClass());
    }

    private Method invocable() {
        if (invocable == null) {
            invocable = invocable(method);
        }
        return invocable;
    }

    /**
     * Whether this property can be read off an object. Properties are discovered from the base
     * structure, so an actual structure of an unrelated type may declare no such accessor, and
     * the structure matcher checks this before reading. The question is only whether the accessor
     * exists: one that exists but cannot be called is a broken spec, and {@link #getValue} throws.
     */
    public boolean isReadableFrom(Object obj) {
        return obj == null || method.getDeclaringClass().isInstance(obj)
                || accessorOf(obj.getClass()).isPresent();
    }

    private Method invocableOn(Class<?> type) {
        return invocable(accessorOf(type).orElseThrow(() -> new IllegalStateException(
                type.getName() + " declares no " + method.getName() + "()")));
    }

    private Optional<Method> accessorOf(Class<?> type) {
        try {
            return Optional.of(type.getMethod(method.getName()));
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        }
    }

    private static Method invocable(Method accessor) {
        return InvocableAccessors.resolve(accessor)
                .orElseThrow(() -> new InaccessibleAccessorException(accessor));
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

    /** Whether the declared type implements <code>java.util.Map</code>. @see #isList() */
    @Override
    public boolean isMap() {
        return Map.class.isAssignableFrom(method.getReturnType());
    }

    /** Whether the declared type implements <code>java.util.Set</code>. @see #isList() */
    @Override
    public boolean isSet() {
        return Set.class.isAssignableFrom(method.getReturnType());
    }

    /** Whether the property is an array, of objects or of primitives. */
    @Override
    public boolean isArray() {
        return method.getReturnType().isArray();
    }

    /** Whether the property is an {@link Optional}, which is final, so the type must be exact. */
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
