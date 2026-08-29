package nl.alexeyu.structmatcher.matcher;

/**
 * Replaces a value that took part in a comparison but must not be printed. {@link Maskers} holds
 * the ready ones.
 * <p>
 * A masker never sees <code>null</code>: reporting an absent value as masked would hide that it
 * was missing.
 */
@FunctionalInterface
public interface Masker {

    /**
     * The stand-in that lands in the feedback in place of the value. Deriving it from the value,
     * as {@link Maskers#hash} does, keeps equal values equal in the report; a constant keeps
     * nothing.
     *
     * @param value
     *            the value to withhold, never <code>null</code>.
     */
    Object mask(Object value);

}
