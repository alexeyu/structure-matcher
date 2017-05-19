package nl.alexeyu.structmatcher;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Convenient wrapper of POJO properties (which are represented by getter methods). 
 */
public class Property {
    
    private static List<String> HIDDEN_GETTERS = Arrays.asList("getClass", "getDeclaringClass");

    private final Method method;

    private Property(Method method) {
        this.method = method;
    }
    
    public static Stream<Property> forClass(Class<?> cl) {
        return Arrays.stream(cl.getMethods())
            .map(Property::of)
            .filter(Optional::isPresent)
            .map(Optional::get);
    }

    static Optional<Property> of(Method method) {
        return isValid(method)
            ? Optional.of(new Property(method))
            : Optional.empty();
    }

    public String getName() {
        if (isGetMethod(method)) {
            return method.getName().substring(3);
        }
        if (isIsMethod(method)) {
            return method.getName().substring(2);
        }
        return method.getName();
    }

    public Object getValue(Object obj) {
        try {
            return method.invoke(obj);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new IllegalStateException("Could not invoke " + method.getName() + " for " + obj, e);
        }
    }
    
    public boolean isSimple() {
        return isSimple(method.getReturnType());
    }
    
    public boolean isList() {
        return method.getReturnType().isAssignableFrom(List.class);
    }
    
    public static boolean isSimple(Class<?> cl) {
        return String.class.isAssignableFrom(cl)
                || Number.class.isAssignableFrom(cl)
                || Boolean.class.isAssignableFrom(cl)
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