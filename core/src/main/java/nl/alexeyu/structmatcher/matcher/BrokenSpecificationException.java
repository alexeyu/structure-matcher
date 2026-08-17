package nl.alexeyu.structmatcher.matcher;

/**
 * Thrown when the <em>base</em> value breaks a strict matcher's condition, e.g. a null under
 * {@link Matchers#nonNull()}. The reference side breaking its own rule blames the spec, not the
 * data, so it is an exception rather than feedback.
 */
public class BrokenSpecificationException extends RuntimeException {

    public BrokenSpecificationException(String property, Object value, String specification) {
        super(String.format("The original value of %s is %s. It is against the specification: %s",
                property, value, specification));
    }

}
