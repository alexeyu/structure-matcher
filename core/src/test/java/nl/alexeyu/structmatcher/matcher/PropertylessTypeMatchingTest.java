package nl.alexeyu.structmatcher.matcher;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nl.alexeyu.structmatcher.feedback.Feedback;

/**
 * Types with no discoverable properties. Empty feedback is the one outcome to rule out, since it
 * would report two unrelated values as matching. The type's own {@code equals} decides instead,
 * and a type without one gets its comparison rejected.
 */
public class PropertylessTypeMatchingTest {

    private final Matcher<Object> ignore = Matchers.anyValue();

    @Test
    public void propertylessTypeWithEqualsMatchesWhenEqual() {
        var feedback = ObjectMatcher.forClass(FluentValue.class).match(new FluentValue("a"),
                new FluentValue("a"));
        assertTrue(feedback.isEmpty());
    }

    @Test
    public void propertylessTypeWithEqualsIsNotSilentlyAccepted() {
        var expected = new FluentValue("a");
        var actual = new FluentValue("b");
        var feedback = ObjectMatcher.forClass(FluentValue.class).match(expected, actual);
        assertFalse(feedback.isEmpty());
        assertEquals(Feedback.nonEqual(FluentValue.class.getName(), expected, actual), feedback);
    }

    @Test
    public void nestedPropertylessTypeWithEqualsIsComparedByEquals() {
        var expected = new PropertylessHolder(new FluentValue("a"), new OpaqueValue("x"));
        var actual = new PropertylessHolder(new FluentValue("b"), new OpaqueValue("x"));
        var feedback = ObjectMatcher.forClass(PropertylessHolder.class)
                .withMatcher(ignore, "Opaque").match(expected, actual);
        assertFalse(feedback.isEmpty());
        assertEquals(
                Feedback.composite(PropertylessHolder.class.getName(),
                        asList(Feedback.nonEqual("Fluent", expected.fluent(), actual.fluent()))),
                feedback);
    }

    @Test
    public void propertylessTypeWithoutEqualsIsRejected() {
        var exception = assertThrows(NoComparablePropertiesException.class,
                () -> ObjectMatcher.forClass(OpaqueValue.class).match(new OpaqueValue("x"),
                        new OpaqueValue("x")));
        assertTrue(exception.getMessage().contains(OpaqueValue.class.getName()),
                "the message should name the offending type, was: " + exception.getMessage());
    }

    @Test
    public void nestedPropertylessTypeWithoutEqualsNamesTheProperty() {
        var exception = assertThrows(NoComparablePropertiesException.class,
                () -> ObjectMatcher.forClass(PropertylessHolder.class).match(
                        new PropertylessHolder(new FluentValue("a"), new OpaqueValue("x")),
                        new PropertylessHolder(new FluentValue("a"), new OpaqueValue("x"))));
        assertTrue(exception.getMessage().contains("'Opaque'"),
                "the message should name the property, was: " + exception.getMessage());
    }

    @Test
    public void customMatcherResolvesARejectedProperty() {
        var feedback = ObjectMatcher.forClass(PropertylessHolder.class)
                .withMatcher(ignore, "Opaque")
                .match(new PropertylessHolder(new FluentValue("a"), new OpaqueValue("x")),
                        new PropertylessHolder(new FluentValue("a"), new OpaqueValue("y")));
        assertTrue(feedback.isEmpty());
    }

}
