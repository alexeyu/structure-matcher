package nl.alexeyu.structmatcher.matcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.property.Property;
import nl.alexeyu.structmatcher.property.PropertyPathPattern;
import nl.alexeyu.structmatcher.property.ClassProperty;
import nl.alexeyu.structmatcher.property.SimpleProperty;

/**
 * Matches two objects of a given class. This is the entry point to the library.
 * Matching consists from four steps:
 * <ol>
 * <li>Create a matcher.
 * <li>Optionally register custom matchers for sub-properties of the structure.
 * <li>Perform matching.
 * <li>Analyze the result.
 * </ol>
 * This class has convenient methods to provide steps 1-3. In the simplest case
 * (no custom matchers), the code could look like:
 *
 * <pre>
 * Date date1 = Date.from(Instant.parse("2017-05-22T23:00:00.01Z"));
 * Date date2 = Date.from(Instant.parse("2017-05-22T23:00:00Z"));
 * FeedbackNode feedback = ObjectMatcher.forClass(Date.class).match(date1, date2);
 * // The feedback is: java.util.Date: [Time: 1495494000000 !~ 1495494000010].
 * </pre>
 * <p>
 * For instance, if milliseconds should be ignored, a special matcher can be
 * registered. (In this case it "normalizes" both values by dividing them by 1000
 * before the comparison).
 *
 * <pre>
 * FeedbackNode feedback = ObjectMatcher.forClass(Date.class)
 *         .withMatcher(Matchers.normalizingBoth(millis -> millis / 1000, Matchers.<Long>valuesEqual()), "Time")
 *         .match(date1, date2);
 * // The feedback is: java.util.Date: [].
 * </pre>
 */
public class ObjectMatcher<T> {

    private final Class<T> clazz;

    private final Map<PropertyPathPattern, Matcher<?>> propertyToMatcher = new HashMap<>();

    private ObjectMatcher(Class<T> clazz) {
        this.clazz = clazz;
    }

    /**
     * Creates a matcher which would validate the instances of a given class.
     *
     * @param clazz the class of entities to be matched.
     * @return a matcher ready to be set up and executed.
     */
    public static <T> ObjectMatcher<T> forClass(Class<T> clazz) {
        return new ObjectMatcher<>(clazz);
    }

    /**
     * Registers a custom matcher for a property with a given path.
     *
     * @param matcher      to be registered and invoked when two values of a specified
     *                     property will be matched.
     * @param propertyPath a path to a property, imcluding the property itself. Every
     *                     string value corresponds to a property name in a given
     *                     structure. Properties of a class are defined by getter methods
     *                     without parameters. For example,
     *                     <code>java.util.Calendar</code> would have 10 properties (8
     *                     get and 2 is methods), while the return type of
     *                     <code>Calendar::getTimeZone</code> has 4 properties. If it is
     *                     necessary to define a custom matcher for
     *                     <code>TimeZone::getRawOffset</code>, the code would look like:
     *
     *                     <pre>
     *                                ObjectMapper.forClass(Calendar.class)
     *                                      .withMatcher(<a custom matcher>, "TimeZone", "RawOffset");
     *                                </pre>
     * @return the same instance of the <code>ObjectMatcher</code>, ready to be
     * set up further and/or executed.
     * @see {@link ClassProperty}
     */
    public ObjectMatcher<T> withMatcher(Matcher<?> matcher, String... propertyPath) {
        List<String> fullPath = new ArrayList<>(Arrays.asList(propertyPath));
        fullPath.add(0, clazz.getName());
        propertyToMatcher.put(new PropertyPathPattern(fullPath), matcher);
        return this;
    }

    /**
     * A shortcut for the <code>withMatcher</code> method. The path to a
     * property is specified by one string where property names are separated by
     * period. So, an example from the former method would look like:
     *
     * <pre>
     * ObjectMapper.forClass(Calendar.class)
     *     .with(<a custom matcher>, "TimeZone.RawOffset");
     * </pre>
     *
     * @param matcher      to be registered and invoked when two values of a specified
     *                     property will be matched.
     * @param propertyPath the path to a property as a dot-separated string.
     * @return the same instance of the <code>ObjectMatcher</code>, ready to be
     * set up further and/or executed.
     */
    public ObjectMatcher<T> with(Matcher<?> matcher, String propertyPath) {
        return withMatcher(matcher, propertyPath.split("\\."));
    }

    /**
     * Matches two objects and returns a feedback. If the feedback is empty, the
     * objects are considered matching. Otherwise the feedback contains details
     * of which concrete properties do not match and why.
     *
     * @param expected a "base" object the other object should be matched against.
     * @param actual   an object to be matched against the base one.
     * @return empty feedback if the objects are considered matching, a
     * non-empty feedback tree otherwise.
     */
    public FeedbackNode match(T expected, T actual) {
        try {
            MatchingStackHolder.set(new DefaultMatchingStack(expected, actual, propertyToMatcher));
            Property property = new SimpleProperty(clazz.getName());
            return Matchers.contextAware(property, () -> Matchers.structuresEqual()).match(expected, actual);
        } finally {
            MatchingStackHolder.clear();
        }
    }

}
