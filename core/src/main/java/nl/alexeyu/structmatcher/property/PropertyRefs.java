package nl.alexeyu.structmatcher.property;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

/**
 * Derives the property name a {@link PropertyRef} points at, so a method reference yields the same
 * name a string path would. The implementing accessor is recovered from the lambda's synthetic
 * {@code writeReplace} method (which returns a {@link SerializedLambda}), then the existing
 * {@link ClassProperty} naming rules are applied — getters get their {@code get}/{@code is} prefix
 * stripped, record components are capitalized as-is.
 */
public final class PropertyRefs {

    private PropertyRefs() {
    }

    /**
     * Returns the capitalized property name a reference points at, e.g. {@code Server::getIp} and
     * {@code Server::ip} (a record accessor) both yield {@code "Ip"}.
     *
     * @throws IllegalArgumentException
     *             if the reference is not a direct accessor reference (e.g. an inline lambda) or the
     *             accessor cannot be resolved.
     */
    public static <T, R> String nameOf(PropertyRef<T, R> ref) {
        var lambda = serializedLambda(ref);
        var accessor = accessor(ownerClass(lambda), lambda.getImplMethodName());
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

    private static Class<?> ownerClass(SerializedLambda lambda) {
        var className = lambda.getImplClass().replace('/', '.');
        try {
            return Class.forName(className, false,
                    Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not load " + className, e);
        }
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
