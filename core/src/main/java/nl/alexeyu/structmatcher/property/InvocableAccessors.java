package nl.alexeyu.structmatcher.property;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Finds a declaration of an accessor that the library can call.
 * <p>
 * A public accessor sits out of reach when its declaring class is not public: reflection refuses
 * the call with <code>IllegalAccessException</code>. A package-private class overriding a public
 * supertype's accessor, which AutoValue and Immutables generate, has a public declaration further
 * up, and calling that one reaches the override through virtual dispatch. A package-private base
 * whose getters a public subclass inherits arrives as the compiler's synthetic bridge, which
 * {@link ClassProperty} keeps when it can call nothing else.
 */
final class InvocableAccessors {

    private static final String OUR_PACKAGE = InvocableAccessors.class.getPackageName();

    private InvocableAccessors() {
    }

    /**
     * Returns the accessor itself when the library can call it, else the nearest public supertype
     * declaring the same signature, and nothing when no supertype declares it.
     * <p>
     * A bridge prefers the supertype too, even though the class declaring it is public. The bridge
     * belongs to one implementation, while the declaration it stands for is shared by all of them,
     * and the two structures being matched need not be the same implementation.
     */
    static Optional<Method> resolve(Method accessor) {
        if (isReachable(accessor.getDeclaringClass()) && !accessor.isBridge()) {
            return Optional.of(accessor);
        }
        return declaredOnSupertype(accessor).or(() -> ownDeclaration(accessor));
    }

    private static Optional<Method> declaredOnSupertype(Method accessor) {
        return supertypesOf(accessor.getDeclaringClass())
                .map(supertype -> sameAccessorOn(supertype, accessor)).flatMap(Optional::stream)
                .findFirst();
    }

    /** A bridge is the last resort: with a package-private base there is nothing else to call. */
    private static Optional<Method> ownDeclaration(Method accessor) {
        return isReachable(accessor.getDeclaringClass()) ? Optional.of(accessor) : Optional.empty();
    }

    private static Optional<Method> sameAccessorOn(Class<?> supertype, Method accessor) {
        if (!isReachable(supertype)) {
            return Optional.empty();
        }
        try {
            var candidate = supertype.getMethod(accessor.getName(), accessor.getParameterTypes());
            return isReachable(candidate.getDeclaringClass()) ? Optional.of(candidate)
                    : Optional.empty();
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        }
    }

    /** Superclasses and interfaces, nearest first, each visited once. */
    private static Stream<Class<?>> supertypesOf(Class<?> type) {
        var queue = new ArrayDeque<Class<?>>();
        var visited = new LinkedHashSet<Class<?>>();
        enqueueSupertypes(type, queue);
        while (!queue.isEmpty()) {
            var supertype = queue.poll();
            if (visited.add(supertype)) {
                enqueueSupertypes(supertype, queue);
            }
        }
        return visited.stream();
    }

    private static void enqueueSupertypes(Class<?> type, Deque<Class<?>> queue) {
        if (type.getSuperclass() != null) {
            queue.add(type.getSuperclass());
        }
        queue.addAll(Arrays.asList(type.getInterfaces()));
    }

    /**
     * Whether the type is reachable from this package, the one making the reflective call: the
     * type is public, or it is ours. javac compiles a private nested type to a package-private
     * class, so the package check covers those.
     */
    private static boolean isReachable(Class<?> type) {
        return Modifier.isPublic(type.getModifiers())
                || type.getPackageName().equals(OUR_PACKAGE);
    }

}
