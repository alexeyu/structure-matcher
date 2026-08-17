package nl.alexeyu.structmatcher.property;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Derives the property name a {@link PropertyRef} points at, so a method reference yields the same
 * name a string path would. It recovers the accessor behind the lambda through its synthetic
 * {@code writeReplace} method, which returns a {@link SerializedLambda}, then applies the
 * {@link ClassProperty} naming rules: a getter loses its {@code get}/{@code is} prefix, a record
 * component keeps its name and gains a capital.
 */
public final class PropertyRefs {

    private PropertyRefs() {
    }

    /**
     * Returns the capitalized property name a reference points at, e.g. {@code Server::getIp} and
     * {@code Server::ip} (a record accessor) both yield {@code "Ip"}.
     *
     * @throws IllegalArgumentException
     *             if the reference is not a direct accessor reference (e.g. an inline lambda) or
     *             the accessor cannot be resolved.
     */
    public static <T, R> String nameOf(PropertyRef<T, R> ref) {
        var lambda = serializedLambda(ref);
        var accessor = accessor(ownerClass(lambda, ref), lambda.getImplMethodName());
        return ClassProperty.forMethod(accessor).getName();
    }

    private static SerializedLambda serializedLambda(PropertyRef<?, ?> ref) {
        try {
            var writeReplace = ref.getClass().getDeclaredMethod("writeReplace");
            writeReplace.setAccessible(true);
            return (SerializedLambda) writeReplace.invoke(ref);
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new IllegalArgumentException(
                    "Expected a method reference to a property accessor, got " + ref, e);
        }
    }

    private static Class<?> ownerClass(SerializedLambda lambda, PropertyRef<?, ?> ref) {
        var className = lambda.getImplClass().replace('/', '.');
        ClassNotFoundException failure = null;
        for (var loader : loaders(ref)) {
            try {
                return Class.forName(className, false, loader);
            } catch (ClassNotFoundException e) {
                failure = e;
            }
        }
        throw new IllegalArgumentException("Could not load " + className, failure);
    }

    /**
     * The lambda's own loader first: it defined the reference, so it sees the accessor by
     * construction. The context classloader stays as a fallback for the setup where it sees more;
     * either may be null, which asks the bootstrap loader.
     */
    private static List<ClassLoader> loaders(PropertyRef<?, ?> ref) {
        var loaders = new ArrayList<ClassLoader>(2);
        loaders.add(ref.getClass().getClassLoader());
        var contextLoader = Thread.currentThread().getContextClassLoader();
        if (!loaders.contains(contextLoader)) {
            loaders.add(contextLoader);
        }
        return loaders;
    }

    private static Method accessor(Class<?> owner, String name) {
        try {
            return owner.getMethod(name);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    name + " is not a no-argument accessor of " + owner.getName(), e);
        }
    }

}
