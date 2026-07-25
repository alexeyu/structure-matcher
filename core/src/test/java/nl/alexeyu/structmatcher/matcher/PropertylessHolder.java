package nl.alexeyu.structmatcher.matcher;

/** Test model exercising the property-less values as nested properties, not only as a root. */
public record PropertylessHolder(FluentValue fluent, OpaqueValue opaque) {
}
