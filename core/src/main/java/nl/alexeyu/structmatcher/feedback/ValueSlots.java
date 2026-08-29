package nl.alexeyu.structmatcher.feedback;

/**
 * Which slots of an {@link ExpectationBroken} leaf hold data read from the compared objects, as
 * opposed to words the matcher wrote itself: a specification, a size, a type name.
 * <p>
 * Masking redacts the data and leaves the words alone; redacting both would leave
 * <code>*** !~ ***</code>.
 */
public enum ValueSlots {

    /** Neither slot: both sides are the matcher's own words. */
    NONE,

    /** The value slot only; the expectation states a condition the value failed. */
    VALUE,

    /** Both slots: the expectation is the base value the actual one was held against. */
    BOTH

}
