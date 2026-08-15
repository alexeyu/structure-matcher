package nl.alexeyu.structmatcher.property;

import java.lang.reflect.Method;

/**
 * Thrown when the library can call no declaration of a property's accessor: the accessor is
 * public, the classes declaring it are not, and reflection refuses. Like the other spec failures
 * this blames the model rather than the data. Make the type public, or a supertype that declares
 * the accessor, or register a custom matcher for the property.
 */
public class InaccessibleAccessorException extends RuntimeException {

    public InaccessibleAccessorException(Method accessor) {
        super(String.format(
                "Cannot call %s() on %s: the class is not public and no public supertype declares "
                        + "the accessor, so the library cannot read the property. Make the type "
                        + "(or a supertype declaring the accessor) public, or register a custom "
                        + "matcher for this property.",
                accessor.getName(), accessor.getDeclaringClass().getName()));
    }

}
