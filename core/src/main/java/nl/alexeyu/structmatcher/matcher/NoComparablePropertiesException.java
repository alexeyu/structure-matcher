package nl.alexeyu.structmatcher.matcher;

/**
 * Thrown when a structure exposes no properties (no <code>get</code>/<code>is</code> getters, no
 * record components) and no {@code equals} of its own: nothing is left to compare it by, and empty
 * feedback would mean "these match". Like {@link BrokenSpecificationException} it blames the spec,
 * not the data - register a custom matcher for the property, or give the type an {@code equals}.
 */
public class NoComparablePropertiesException extends RuntimeException {

    public NoComparablePropertiesException(String property, Class<?> type) {
        super(String.format(
                "Cannot compare two instances of %s (property '%s'): the type exposes no "
                        + "properties (no get/is getters, no record components) and does not "
                        + "override equals, so there is nothing to compare. Register a custom "
                        + "matcher for this property, or give the type an equals.",
                type.getName(), property));
    }

}
