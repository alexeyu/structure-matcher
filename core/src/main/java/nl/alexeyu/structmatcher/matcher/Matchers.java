package nl.alexeyu.structmatcher.matcher;

import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.property.ClassProperty;
import nl.alexeyu.structmatcher.property.Property;

/**
 * Factory which produces common matchers.
 */
public final class Matchers {

    private Matchers() {
    }

    /**
     * Wraps a matcher so that two nulls match and a single null does not. When both values are
     * present, {@code nextMatcher} decides.
     */
    public static <V> Matcher<V> nullAware(Matcher<V> nextMatcher) {
        return new NullAwareMatcher<>(nextMatcher);
    }

    /**
     * Accepts any pair of values, as long as both are <code>null</code> or both are present. Use it
     * to drop a property out of the comparison.
     */
    public static <V> Matcher<V> anyValue() {
        return nullAware((p, e, a) -> Feedback.empty(p));
    }

    /**
     * Compares the actual value with <code>alwaysExpected</code>, ignoring the base value its
     * <code>match</code> method receives.
     * <p/>
     * <i>Example:</i>
     *
     * <pre>
     * Matchers.constant(3.14).match("pi", null, 3.14); // Produces an empty feedback
     * Matchers.constant(3.14).match("pi", 3, 3); // Produces a non-empty feedback
     * </pre>
     */
    public static <V> Matcher<V> constant(V alwaysExpected) {
        return (prop, exp, act) -> valuesEqual().match(prop, alwaysExpected, act);
    }

    /**
     * Matches two values that are both <code>null</code> or equal to each other. This is the
     * default for a simple property.
     */
    public static <V> Matcher<V> valuesEqual() {
        return nullAware(new ValuesEqualMatcher<>());
    }

    /**
     * Matches two lists element by element, in order, and reports each mismatch under
     * <code>property[index]</code>. Elements go through the default logic
     * (<code>valuesEqual</code> for simple values, <code>structuresEqual</code> for the rest), so a
     * matcher registered for a path inside an element still applies. Lists of different size never
     * match. Neither list may be null.
     */
    public static <V> ListMatcher<V> listsEqual() {
        return new ListMatcher<>();
    }

    /**
     * Matches two lists as multisets: it sorts both with the comparator first, so
     * <code>[1, 2, 3]</code> matches <code>[2, 3, 1]</code>. After the sort the elements still
     * have to match pairwise, so order is the only difference this tolerates.
     */
    public static <V> IgnoreOrderListMatcher<V> listsHaveEqualElements(Comparator<V> comparator) {
        return new IgnoreOrderListMatcher<>(comparator);
    }

    /**
     * Matches two maps key by key, reporting missing and extra keys along with value mismatches.
     * Values go through the default logic (<code>valuesEqual</code> for simple values,
     * <code>structuresEqual</code> for complex ones). Neither map may be null.
     */
    public static <K, V> MapMatcher<K, V> mapsEqual() {
        return new MapMatcher<>();
    }

    /**
     * Matches two sets by membership, reporting the elements only one side holds. It compares
     * elements by their own <code>equals</code>/<code>hashCode</code>, so value types (records,
     * strings, enums) make the natural elements; for a field-aware, order-insensitive comparison
     * of a <em>list</em>, use {@link #listsHaveEqualElements}. Neither set may be null.
     */
    public static <V> SetMatcher<V> setsEqual() {
        return new SetMatcher<>();
    }

    /**
     * Matches two arrays of the same length index by index, boxing primitives on the way in.
     * Elements go through the default logic (<code>valuesEqual</code> for simple values,
     * <code>structuresEqual</code> for complex ones). Neither array may be null.
     */
    public static ArrayMatcher arraysEqual() {
        return new ArrayMatcher();
    }

    /**
     * Matches two {@link java.util.Optional} properties by unwrapping both and comparing the
     * contents with the default logic, treating an empty optional as <code>null</code>. Two empty
     * optionals match; an empty one against a present one does not.
     */
    public static OptionalMatcher optional() {
        return new OptionalMatcher();
    }

    /**
     * Matches two structures property by property, recursing into nested ones. Two nulls match, and
     * each property gets the default matcher for its kind (<code>valuesEqual</code> for a simple
     * property, <code>listsEqual</code> for a list, <code>structuresEqual</code> for the rest)
     * unless a matcher registered on {@link ObjectMatcher} claims its path.
     */
    public static <V> Matcher<V> structuresEqual() {
        return nullAware(new StructureMatcher<>());
    }

    /**
     * Applies the normalizer to the actual value, then delegates. <i>Example:</i>
     *
     * <pre>
     * Matchers.<String>normalizing(name -> name.trim(), Matchers.valuesEqual())
     *         .match("name", "Alex", " Alex "));
     * // Produces an empty feedback.
     * </pre>
     */
    public static <V> Matcher<V> normalizing(UnaryOperator<V> normalizer, Matcher<V> delegate) {
        return delegate.normalizing(normalizer);
    }

    /**
     * Applies the normalizer to the base value, then delegates. <i>Example:</i>
     *
     * <pre>
     * Matchers.<String>normalizingBase(name -> name.substring(0,1) + ".",
     *   Matchers.valuesEqual()).match("initial", "Alex", "A."));
     * // Produces an empty feedback.
     * </pre>
     */
    public static <V> Matcher<V> normalizingBase(UnaryOperator<V> normalizer, Matcher<V> delegate) {
        return delegate.normalizingBase(normalizer);
    }

    /**
     * Applies the normalizer to both values, then delegates. <i>Example:</i>
     *
     * <pre>
     * Matchers.<String>normalizingBoth(name -> name.trim(),
     *   Matchers.valuesEqual()).match("name", "   Alex", "Alex   "));
     * // Produces an empty feedback.
     * </pre>
     */
    public static <V> Matcher<V> normalizingBoth(UnaryOperator<V> normalizer, Matcher<V> delegate) {
        return delegate.normalizingBoth(normalizer);
    }

    /**
     * Picks the default matcher for a property by its kind: <code>listsEqual()</code> for a list,
     * <code>mapsEqual()</code> for a map, <code>setsEqual()</code> for a set,
     * <code>arraysEqual()</code> for an array, <code>optional()</code> for an
     * {@link java.util.Optional}, <code>valuesEqual()</code> for a simple value, and
     * <code>structuresEqual()</code> for anything else.
     * <p>
     * The pick comes from the base structure's declaration, so the matcher meets only an actual
     * value of that shape; anything else is a mismatch of the property. A matcher registered for
     * the path replaces this one and takes both values as they come.
     *
     * @see ClassProperty
     * @see Property#fitsDeclaredShape
     */
    public static Matcher<Object> forProperty(Property property) {
        var matcher = asObjectMatcher(defaultMatcherFor(property));
        return (name, expected, actual) -> expected != null && !property.fitsDeclaredShape(actual)
                ? Feedback.differentTypes(name, expected.getClass(), actual.getClass())
                : matcher.match(name, expected, actual);
    }

    private static Matcher<?> defaultMatcherFor(Property property) {
        if (property.isList()) {
            return listsEqual();
        }
        if (property.isMap()) {
            return mapsEqual();
        }
        if (property.isSet()) {
            return setsEqual();
        }
        if (property.isArray()) {
            return arraysEqual();
        }
        if (property.isOptional()) {
            return optional();
        }
        if (property.isSimple()) {
            return valuesEqual();
        }
        return structuresEqual();
    }

    /**
     * Adapts a matcher of any value type to {@code Matcher<Object>}, the single erased type the
     * matching stack stores and invokes. The cast holds because the stack feeds a matcher only the
     * values of the property it was selected or registered for, whose runtime type is the type the
     * matcher expects.
     */
    @SuppressWarnings("unchecked")
    static Matcher<Object> asObjectMatcher(Matcher<?> matcher) {
        return (Matcher<Object>) matcher;
    }

    /**
     * Requires both values to be present, with the strictness {@link #mustConform} defines: a
     * <code>null</code> actual value produces non-empty feedback, while a <code>null</code> base
     * value throws <code>BrokenSpecificationException</code>.
     */
    public static <V> Matcher<V> nonNull() {
        return mustConform(v -> v != null, "A non-null value");
    }

    /**
     * Requires both values to satisfy the predicate. An actual value that fails it produces
     * non-empty feedback; a base value that fails it throws
     * <code>BrokenSpecificationException</code>, since the reference side breaking the rule points
     * at the spec, not at the data. <i>Example:</i>
     *
     * <pre>
     * Matchers.<String>mustConform(m -> m.toUpperCase().equals(m),
     *         "A short month name must be in upper-case.").match("month", "JAN", "Jan");
     * // the feedback - month: Jan !~ A short month name must be in upper-case.
     * </pre>
     *
     * @param specification
     *            what the predicate stands for, quoted in the feedback or the exception.
     */
    public static <V> Matcher<V> mustConform(Predicate<V> predicate, String specification) {
        return new MustConformMatcher<>(predicate, specification);
    }

    public static <T, V> IndirectMatcher<T, V> indirectMatcher(String description,
            Matcher<V> valueMatcher, Function<T, V> expectedValueFetcher,
            Function<T, V> actualValueFetcher) {
        return new IndirectMatcher<>(description, valueMatcher, expectedValueFetcher,
                actualValueFetcher);
    }

    static ContextAwareMatcher contextAware(Property property,
            Supplier<Matcher<Object>> defaultMatcherSupplier) {
        return new ContextAwareMatcher(property, MatchingStackHolder.get(), defaultMatcherSupplier);
    }

    static <V> Matcher<V> getNullAwareMatcher(V obj) {
        return nullAware(forObject(obj));
    }

    static <V> Matcher<V> forObject(V obj) {
        if (obj == null) {
            // Unreachable: the null-aware wrapper answers before a null gets here.
            return (p, e, a) -> {
                throw new NullPointerException("Cannot match null value.");
            };
        }
        if (ClassProperty.isSimple(obj.getClass())) {
            return valuesEqual();
        }
        return structuresEqual();
    }

}
