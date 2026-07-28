package nl.alexeyu.structmatcher.json;

/**
 * One broken expectation as persisted in a {@link FeedbackArchive}: the canonical path at which a
 * comparison broke, plus the expectation and the offending value. An archive lists these
 * explicitly, which keeps the wire shape flat and round-trippable, where {@link Json#mapper()}
 * renders the same feedback as a nested, property-keyed tree for a human to read.
 *
 * <p>
 * {@code expectation} and {@code value} persist as whatever JSON their runtime values map to, and
 * come back in their JSON-native form (string, number, boolean, null, list or object) rather than
 * the original Java types. The archive round-trips the JSON, not the types.
 *
 * @param path
 *            the canonical, registration-style path to the broken leaf, e.g.
 *            {@code Books[0].Authors[0].FirstName} or {@code Server.Ip}.
 * @param expectation
 *            what the matcher expected: a value, or a spec such as {@code "Non-null"}.
 * @param value
 *            the value that broke it, possibly {@code null}.
 */
public record ArchivedLeaf(String path, Object expectation, Object value) {
}
