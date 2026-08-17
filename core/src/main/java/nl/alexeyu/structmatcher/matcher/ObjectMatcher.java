package nl.alexeyu.structmatcher.matcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.property.ClassProperty;
import nl.alexeyu.structmatcher.property.PropertyPathPattern;
import nl.alexeyu.structmatcher.property.PropertyRef;
import nl.alexeyu.structmatcher.property.PropertyRefs;
import nl.alexeyu.structmatcher.property.SimpleProperty;

/**
 * Matches two objects of a given class, and serves as the entry point to the library. You create a
 * matcher, register custom matchers for the sub-properties where the comparison should be loose,
 * run it, then read the {@link FeedbackNode} it returns. Without custom matchers, the code looks
 * like:
 *
 * <pre>
 * Date date1 = Date.from(Instant.parse("2017-05-22T23:00:00.01Z"));
 * Date date2 = Date.from(Instant.parse("2017-05-22T23:00:00Z"));
 * FeedbackNode feedback = ObjectMatcher.forClass(Date.class).match(date1, date2);
 * // The feedback is: java.util.Date: [Time: 1495494000000 !~ 1495494000010].
 * </pre>
 * <p>
 * To ignore the milliseconds, register a matcher that normalizes both values by dividing them by
 * 1000 before the comparison:
 *
 * <pre>
 * FeedbackNode feedback = ObjectMatcher.forClass(Date.class)
 *         .withMatcher(
 *                 Matchers.<Long>valuesEqual().normalizingBoth(millis -> millis / 1000),
 *                 "Time")
 *         .match(date1, date2);
 * // The feedback is: java.util.Date: [].
 * </pre>
 */
public class ObjectMatcher<T> {

    private final Class<T> clazz;

    private final Map<PropertyPathPattern, Matcher<Object>> propertyToMatcher = new HashMap<>();

    private ObjectMatcher(Class<T> clazz) {
        this.clazz = clazz;
    }

    /**
     * Creates a matcher for instances of the given class, ready to be set up and run.
     */
    public static <T> ObjectMatcher<T> forClass(Class<T> clazz) {
        return new ObjectMatcher<>(clazz);
    }

    /**
     * Registers a custom matcher for the property at the given path, and returns this matcher for
     * further set-up.
     *
     * @param matcher
     *            invoked instead of the default when the two values of that property meet.
     * @param propertyPath
     *            the path to the property, the property itself included. Each segment names a
     *            property of the structure one level up, and a no-arg getter defines a property. A
     *            matcher for <code>TimeZone::getRawOffset</code> registers like this:
     *
     *            <pre>
     * ObjectMapper.forClass(Calendar.class)
     *         .withMatcher(<a custom matcher>, "TimeZone", "RawOffset");
     *            </pre>
     *
     *            A segment may be the wildcard <code>*</code>, standing for any run of properties,
     *            so <code>"*", "Url"</code> reaches every 'Url' in the model. Register the exact
     *            path to take one of them back: when two patterns match a property, the more
     *            specific one wins.
     *
     * @see ClassProperty
     * @see PropertyPathPattern#MOST_SPECIFIC_FIRST
     */
    public ObjectMatcher<T> withMatcher(Matcher<?> matcher, String... propertyPath) {
        var fullPath = new ArrayList<String>(Arrays.asList(propertyPath));
        fullPath.add(0, clazz.getName());
        propertyToMatcher.put(new PropertyPathPattern(fullPath), Matchers.asObjectMatcher(matcher));
        return this;
    }

    /**
     * A shortcut for <code>withMatcher</code> that takes the property names in one dot-separated
     * string, so the example above shortens to:
     *
     * <pre>
     * ObjectMapper.forClass(Calendar.class)
     *     .with(<a custom matcher>, "TimeZone.RawOffset");
     * </pre>
     *
     * @param matcher
     *            invoked instead of the default when the two values of that property meet.
     * @param propertyPath
     *            the path to the property, dot-separated.
     */
    public ObjectMatcher<T> with(Matcher<?> matcher, String propertyPath) {
        return withMatcher(matcher, propertyPath.split("\\."));
    }

    /**
     * Type-safe counterpart of {@link #withMatcher}: you spell the path as a chain of accessor
     * references ({@code Server::getIp}, or {@code Server::ip} for a record) rather than strings,
     * so the compiler catches a rename and the IDE completes each step. Each reference's return
     * type must be the receiver type of the next, e.g.
     *
     * <pre>
     * ObjectMatcher.forClass(BookSearchResult.class)
     *     .with(urlMatcher, BookSearchResult::getMetadata,
     *             SearchMetadata::getServer, Server::getIp);
     * </pre>
     *
     * The resulting path is identical to the equivalent string one, since both capitalize the
     * property names the same way, so the two registration styles are interchangeable and take
     * part in the same precedence with wildcard paths registered elsewhere.
     */
    public <A> ObjectMatcher<T> with(Matcher<?> matcher, PropertyRef<? super T, A> p1) {
        return withRefs(matcher, p1);
    }

    /** Two-step typed path. @see #with(Matcher, PropertyRef) */
    public <A, B> ObjectMatcher<T> with(Matcher<?> matcher, PropertyRef<? super T, A> p1,
            PropertyRef<? super A, B> p2) {
        return withRefs(matcher, p1, p2);
    }

    /** Three-step typed path. @see #with(Matcher, PropertyRef) */
    public <A, B, C> ObjectMatcher<T> with(Matcher<?> matcher, PropertyRef<? super T, A> p1,
            PropertyRef<? super A, B> p2, PropertyRef<? super B, C> p3) {
        return withRefs(matcher, p1, p2, p3);
    }

    /** Four-step typed path. @see #with(Matcher, PropertyRef) */
    public <A, B, C, D> ObjectMatcher<T> with(Matcher<?> matcher, PropertyRef<? super T, A> p1,
            PropertyRef<? super A, B> p2, PropertyRef<? super B, C> p3,
            PropertyRef<? super C, D> p4) {
        return withRefs(matcher, p1, p2, p3, p4);
    }

    @SafeVarargs
    private ObjectMatcher<T> withRefs(Matcher<?> matcher, PropertyRef<?, ?>... refs) {
        var path = Arrays.stream(refs).map(PropertyRefs::nameOf).toArray(String[]::new);
        return withMatcher(matcher, path);
    }

    /**
     * Matches two objects and returns the feedback. Empty feedback means they match; otherwise the
     * tree names each property that diverged and why.
     *
     * @param expected
     *            the "base" object, the reference the other one is held against.
     * @param actual
     *            the object under comparison.
     */
    public FeedbackNode match(T expected, T actual) {
        var previous = MatchingStackHolder.get();
        try {
            MatchingStack<Object> stack =
                    new DefaultMatchingStack<>(expected, actual, propertyToMatcher);
            MatchingStackHolder.set(stack);
            var property = new SimpleProperty(clazz.getName());
            return Matchers.contextAware(property, () -> Matchers.structuresEqual()).match(expected,
                    actual);
        } finally {
            MatchingStackHolder.restore(previous);
        }
    }

}
