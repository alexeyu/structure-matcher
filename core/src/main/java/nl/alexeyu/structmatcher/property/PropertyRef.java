package nl.alexeyu.structmatcher.property;

import java.io.Serializable;
import java.util.function.Function;

/**
 * A refactor-safe, type-checked alternative to a stringly-typed property name: a serializable
 * accessor reference such as {@code Server::getIp} or, for a record, {@code Server::ip}. Being
 * {@link Serializable} is what lets the implementing method be recovered (via the synthetic
 * {@code writeReplace}/{@link java.lang.invoke.SerializedLambda}), so the property name can be
 * derived exactly as {@link ClassProperty} derives it from a discovered accessor.
 *
 * @param <T>
 *            the type that declares the property.
 * @param <R>
 *            the property's type.
 * @see PropertyRefs#nameOf(PropertyRef)
 */
@FunctionalInterface
public interface PropertyRef<T, R> extends Function<T, R>, Serializable {
}
