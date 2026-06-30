package nl.alexeyu.structmatcher.json;

/**
 * One broken expectation as persisted in a {@link FeedbackArchive}: the canonical path at which a
 * comparison broke, plus the expectation and the offending value. This is the stable, flat wire
 * shape — unlike the nested, property-keyed tree of {@link Json#mapper()} (a human-readable
 * rendering), an archive is an explicit list of these, so it is unambiguous and round-trippable.
 *
 * <p>
 * {@code expectation} and {@code value} are persisted as whatever JSON their runtime values map to;
 * on read-back they come back as their JSON-native form (string / number / boolean / null / list /
 * object), not necessarily the original Java types. The archive round-trips the JSON, not the types.
 *
 * @param path
 *            the canonical, registration-style path to the broken leaf, e.g.
 *            {@code Books[0].Authors[0].FirstName} or {@code Server.Ip}.
 * @param expectation
 *            description of the expectation that was not met (an expected value, or a spec such as
 *            {@code "Non-null"}).
 * @param value
 *            the actual value that failed the expectation (may be {@code null}).
 */
public record ArchivedLeaf(String path, Object expectation, Object value) {
}
