package nl.alexeyu.structmatcher.matcher;

/** A generic accessor interface; {@link StringValueBox} implements it covariantly. */
public interface ValueBox<T> {

    T getValue();

}
