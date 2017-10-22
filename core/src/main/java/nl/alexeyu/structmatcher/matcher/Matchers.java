package nl.alexeyu.structmatcher.matcher;

import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Predicate;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.property.Property;

/**
 * Factory which produces common matchers.  
 */
public final class Matchers {
    
    private Matchers() {
    }
    
    /**
     * Returns a matcher that considers two values matching if both of them are
     * <code>null</code>. If both of them are not <code>null</code>, calls a
     * matcher passed as an argument to make the final decision.
     * 
     * @param nextMatcher
     *            matcher to be called if both arguments passed for matching are
     *            not <code>null</code>.
     * @return a matcher with the behavior specified above.
     * @see {@link NullAwareMatcher}
     */
    public static <V> Matcher<V> nullAware(Matcher<V> nextMatcher) {
        return new NullAwareMatcher<>(nextMatcher);
    }

    /**
     * Returns a matcher which considers two values matching when both of them
     * are <code>null</code> or both of them are not <code>null</code>.
     * 
     * @return a matcher with the behavior specified above.
     * @see {@link NullAwareMatcher}
     */
    public static <V> Matcher<V> anyValue() {
        return nullAware((p, e, a) -> Feedback.empty(p));
    }

    /**
     * Returns a matcher which expect any given value to be equal to an argument
     * of this method. The matcher ignores the first argument of its
     * <code>match</code> method, replacing it with the
     * <code>alwaysExpected</code> parameter.<p/>
     * <i>Example:</i>
     * <pre>
     * Matchers.constant(3.14).match("pi", null, 3.14);  // Produces an empty feedback
     * Matchers.constant(3.14).match("pi", 3, 3);  // Produces a non-empty feedback
     * </pre>
     * 
     * @param alwaysExpected a value every value passed to the matcher will compared with.
     * @return a matcher with the behavior specified above.
     */
    public static <V> Matcher<V> constant(V alwaysExpected) {
        return (prop, exp, act) -> valuesEqual().match(prop, alwaysExpected, act);
    }

    /**
     * Produces a matcher which considers two values matching either if they
     * both <code>null</code> or equal to each other.
     * 
     * @return a matcher with the behavior specified above.
     * @see {@link NullAwareMatcher}
     * @see {@link Object.equals}
     */
    public static <V> Matcher<V> valuesEqual() {
        return nullAware(new ValuesEqualMatcher<>());
    }

    /**
     * Produces a matcher which considers two lists matching if all their
     * elements match to each other. By default, the elements will be processed
     * with the <code>valuesMatcher</code>, which means they would be considered
     * matching if both are null or equal. However it is possible to redefine
     * this matcher with a custom one. Such custom matcher would be applied to
     * every pair of elements of the lists. Indeed, lists of different size are
     * considered non-matching (this logic cannot be re-defined). Does not allow
     * arguments to be null.
     * 
     * @return a matcher with the behavior specified above.
     * @see {@link ObjectMatcher}
     */
    public static <V> ListMatcher<V> listsEqual() {
        return new ListMatcher<>();
    }

    /**
     * Produces a matcher which considers two lists matching if all their
     * elements match to each other. Unlike the previous matcher, this one
     * ignores the order of elements. For instance, lists <code>[1, 2, 3]</code>
     * and <code>[2, 3, 1]</code> will be considered matching.
     * 
     * @param an element comparator which will be used by the matcher. 
     * @return a matcher with the behavior specified above.
     * @see {@link ObjectMatcher}
     */
    public static <V> IgnoreOrderListMatcher<V> listsHaveEqualElements(Comparator<V> comparator) {
        return new IgnoreOrderListMatcher<>(comparator);
    }

    /**
     * Returns a matcher which matches two data structures. It considers
     * structures matching either if they both are <code>null</code> or if each
     * of their properties matches to a respective property of another
     * structure. By default, the properties will be processed with matchers
     * which look for their equality (<code>valuesEqual</code> for simple
     * properties, <code>listsEqual</code> for lists and
     * <code>structuresEqual</code> for all the other properties). However it
     * is possible to redefine a matcher for any property with a custom one.
     * 
     * @return a matcher with the behavior specified above.
     * @see {@link ObjectMatcher}
     */
    public static <V> Matcher<V> structuresEqual() {
        return nullAware(new StructureMatcher<>());
    }
    
    /**
     * Returns a matcher which executes all the matchers passed as parameters
     * and returns an empty feedback only if all these matchers yielded an empty
     * feedback. Otherwise returns the first non-empty feedback produced by
     * them. It is a convenient method to test a few assumptions about a value
     * at once. <i>Example:</i>
     * 
     * <pre>
     * FeedbackNode feedback = Matchers.and(
     *          Matchers.nonNull(),
     *          Matchers.nonEmptyString()
     *     ).match("Title", expected, actual);
     * </pre>
     * 
     * 
     * @param matchers
     *            matchers to be executed on a given pair of values.
     * @return a matcher with the behavior specified above.
     */
    @SafeVarargs
    public static <V> Matcher<V> and(Matcher<V>... matchers) {
        return new AndMatcher<>(matchers);
    }

    /**
     * Returns a matcher which apply a given function to the second (so-called
     * actual) value and then delegates matching to a given matcher.
     * <i>Example:</i>
     * 
     * <pre>
     * Matchers.<String>normalizing(name -> name.trim(), Matchers.valuesEqual()).match("name", "Alex", " Alex "));
     * // Produces an empty feedback.
     * </pre>
     * 
     * @param normalizer
     *            a function to be applied to the actual value before matching
     *            it to the base one.
     * @param delegate
     *            a matcher to be applied after the normalization.
     * @return a matcher with the behavior specified above.
     */
    public static <V> Matcher<V> normalizing(Function<V, V> normalizer, Matcher<V> delegate) {
        return (prop, exp, act) -> delegate.match(prop, exp, normalizer.apply(act));
    }

    /**
     * Returns a matcher which applies a given function to the first (so-called
     * base) value and then delegates matching to a given matcher.
     * <i>Example:</i>
     * 
     * <pre>
     * Matchers.<String>normalizingBase(name -> name.substring(0,1) + ".", 
     *   Matchers.valuesEqual()).match("initial", "Alex", "A."));
     * // Produces an empty feedback.
     * </pre>
     * 
     * @param normalizer
     *            a function to be applied to the base value before matching the
     *            actual one to it.
     * @param delegate
     *            a matcher to be applied after the normalization.
     * @return a matcher with the behavior specified above.
     */
    public static <V> Matcher<V> normalizingBase(Function<V, V> normalizer, Matcher<V>  delegate) {
        return (prop, exp, act) -> delegate.match(prop, 
                normalizer.apply(exp), 
                act);
    }

    /**
     * Returns a matcher which applies a given function to the both values and
     * then delegates matching to a given matcher. <i>Example:</i>
     * 
     * <pre>
     * Matchers.<String>normalizingBoth(name -> name.trim(), 
     *   Matchers.valuesEqual()).match("name", "   Alex", "Alex   "));
     * // Produces an empty feedback.
     * </pre>
     * 
     * @param normalizer
     *            a function to be applied to both values before matching them.
     * @param delegate
     *            a matcher to be applied after the normalization.
     * @return a matcher with the behavior specified above.
     */
    public static <V> Matcher<V> normalizingBoth(Function<V, V> normalizer, Matcher<V> delegate) {
        return (prop, exp, act) -> delegate.match(prop, 
                normalizer.apply(exp), 
                normalizer.apply(act));
    }

    /**
     * Returns a default matcher for a given property.
     * 
     * @param property to receive a matcher for.
     * @return <code>listEqual()</code> matcher for a list property,
     *         <code>valuesEqual()</code> matcher for a simple property,
     *         <code>structureMatcher()</code> for any other property.
     * @see {@link Property}
     */
    @SuppressWarnings("rawtypes")
    public static Matcher forProperty(Property property) {
        if (property.isList()) {
            return listsEqual();
        }
        if (property.isSimple()) {
            return valuesEqual();
        }
        return structuresEqual();

    }
    
    /**
     * Returns a strict matcher which ensures the values are not null. If a base
     * value appears to be <code>null</code>,
     * <code>BrokenSpecificationException</code> will be thrown. The matcher
     * will return an empty feedback if an actual value is not <code>null</code>
     * and non-empty feedback otherwise.
     * 
     * @return a matcher with the behavior specified above.
     */
    public static <V> Matcher<V> nonNull() {
        return mustConform(v -> v != null, "A non-null value");
    }

    /**
     * Returns a strict matcher which ensure the values conform a given
     * predicate. If the predicate returns <code>false</code> for the base
     * value, <code>BrokenSpecificationException</code> will be thrown. The
     * matcher will return an empty feedback if the predicate returns
     * <code>true</code> for the actual value and non-empty feedback otherwise.
     * <i>Example:</i>
     * 
     * <pre>
     * Matchers.<String>mustConform(m -> m.toUpperCase().equals(m), 
     *     "A short month name must be in upper-case.")
     *         .match("month", "JAN", "Jan");
     * // the feedback - month: Jan !~ A short month name must be in upper-case.
     * </pre>
     * 
     * @param predicate
     *            predicate to be applied to both values.
     * @param specification
     *            a message for an exception or a non-empty feedback this method
     *            will result in if a base/actual value does not conform the
     *            given predicate.
     * @return a matcher with the behavior specified above.
     */
    public static <V> Matcher<V> mustConform(Predicate<V> predicate, String specification) {
        return new MustConformMatcher<>(predicate, specification);
    }
    
    static <V> Matcher<V> contextAware(Matcher<V> defaultMatcher) {
        return new ContextAwareMatcher<>(MatchingStackHolder.get(), defaultMatcher);
    }
    
    static <V> Matcher<V> getNullAwareMatcher(V obj) {
        return nullAware(forObject(obj));
    }

    static <V> Matcher<V> forObject(V obj) {
        if (obj == null) {
             // Should not happen due to the null-aware matcher logic
            return (p, e, a) -> { throw new NullPointerException("Cannot match null value.");};
        }
        if (Property.isSimple(obj.getClass())) {
            return valuesEqual();
        }
        return structuresEqual();
    }

}
