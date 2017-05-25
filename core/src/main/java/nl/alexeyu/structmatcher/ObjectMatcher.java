package nl.alexeyu.structmatcher;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.matcher.Matcher;
import nl.alexeyu.structmatcher.matcher.Matchers;

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
 * 
 * For instance, if milliseconds should be ignored, a respective matcher can be
 * registered:
 * 
 * <pre>
 * Matcher<Long> ignoreMilliseconds = (p, e, a) -> Math.abs(e - a) < 1000 ? Feedback.empty(p) : Feedback.useDefault();
 * FeedbackNode feedback = ObjectMatcher.forClass(Date.class).withMatcher(ignoreMilliseconds, "Time").match(date1,
 *         date2);
 * // The feedback is: java.util.Date: [].
 * </pre>
 */
public class ObjectMatcher<T> {

    private final Class<T> clazz;

    private ObjectMatcher(Class<T> clazz) {
        this.clazz = clazz;
    }

    public static <V> ObjectMatcher<V> forClass(Class<V> clazz) {
        return new ObjectMatcher<>(clazz);
    }

    public ObjectMatcher<T> withMatcher(Matcher<?> matcher, String... propertyPath) {
        String[] fullPath = new String[propertyPath.length + 1];
        System.arraycopy(propertyPath, 0, fullPath, 1, propertyPath.length);
        fullPath[0] = clazz.getName();
        Matchers.registerCustomMatcher(matcher, fullPath);
        return this;
    }

    public ObjectMatcher<T> with(Matcher<?> matcher, String propertyPath) {
        return withMatcher(matcher, propertyPath.split("\\."));
    }

    public FeedbackNode match(T expected, T actual) {
        return Matchers.contextAware(Matchers.structuresEqual()).match(clazz.getName(), expected, actual);
    }

}
