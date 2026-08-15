package nl.alexeyu.structmatcher.matcher;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.property.InaccessibleAccessorException;

/**
 * Models whose accessors the library cannot call where the model declared them. These classes sit
 * in {@code structmatcher.matcher} and {@code ClassProperty} calls from
 * {@code structmatcher.property}, so package-private here puts a model out of reach the way a
 * user's own model would be.
 */
public class NonPublicAccessorMatchingTest {

    /**
     * A public model inheriting a getter from a package-private base. The compiler puts the getter
     * on the subclass as a synthetic bridge, the one declaration anything can call, so filtering
     * bridges dropped the property and two orders with different customers compared as equal.
     */
    @Test
    public void aGetterInheritedFromANonPublicBaseIsCompared() {
        var feedback = ObjectMatcher.forClass(Order.class).match(new Order("alice", 10),
                new Order("bob", 10));
        assertFalse(feedback.isEmpty(), "a differing customer has to be reported");
        assertEquals(Feedback.composite(Order.class.getName(),
                asList(Feedback.nonEqual("Customer", "alice", "bob"))), feedback);
    }

    @Test
    public void aGetterInheritedFromANonPublicBaseStillMatchesWhenEqual() {
        assertTrue(ObjectMatcher.forClass(Order.class)
                .match(new Order("alice", 10), new Order("alice", 10)).isEmpty());
    }

    /**
     * The AutoValue and Immutables shape: a public type the user registers, a package-private
     * generated subclass at runtime. {@code StructureMatcher} introspects the runtime class, so
     * the accessor arrives declared on the subclass, and we call it through the public supertype.
     */
    @Test
    public void anAccessorOfANonPublicImplementationIsCalledThroughItsPublicSupertype() {
        Animal expected = new GeneratedAnimal("cat");
        Animal actual = new GeneratedAnimal("dog");
        var feedback = ObjectMatcher.forClass(Animal.class).match(expected, actual);
        assertEquals(Feedback.composite(Animal.class.getName(),
                asList(Feedback.nonEqual("Name", "cat", "dog"))), feedback);
    }

    /** The same route for a package-private record behind a public interface. */
    @Test
    public void aNonPublicRecordIsComparedThroughTheInterfaceItImplements() {
        var feedback = ObjectMatcher.forClass(Shape.class).match(new Circle(1.0), new Circle(2.0));
        assertEquals(Feedback.composite(Shape.class.getName(),
                asList(Feedback.nonEqual("Radius", 1.0, 2.0))), feedback);
    }

    /**
     * With no public declaration anywhere, the library cannot read the value. It names the
     * accessor and the remedy instead of letting a bare {@code IllegalAccessException} out.
     */
    @Test
    public void aModelWithNoPublicDeclarationIsRejectedByName() {
        var thrown = assertThrows(InaccessibleAccessorException.class, () -> ObjectMatcher
                .forClass(Hidden.class).match(new Hidden("a"), new Hidden("b")));
        assertTrue(thrown.getMessage().contains(Hidden.class.getName()), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("name()"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("custom matcher"), thrown.getMessage());
    }

    /** A base holding shared fields, kept out of the public API. An ordinary layering choice. */
    abstract static class AbstractOrder {

        private final String customer;

        AbstractOrder(String customer) {
            this.customer = customer;
        }

        public String getCustomer() {
            return customer;
        }

    }

    public static final class Order extends AbstractOrder {

        private final int amount;

        public Order(String customer, int amount) {
            super(customer);
            this.amount = amount;
        }

        public int getAmount() {
            return amount;
        }

    }

    public abstract static class Animal {

        public abstract String getName();

    }

    /** Stands in for the package-private subclass AutoValue generates. */
    static final class GeneratedAnimal extends Animal {

        private final String name;

        GeneratedAnimal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

    }

    public interface Shape {

        double radius();

    }

    record Circle(double radius) implements Shape {
    }

    /** No public supertype declares the accessor, so nothing can reach it. */
    record Hidden(String name) {
    }

}
