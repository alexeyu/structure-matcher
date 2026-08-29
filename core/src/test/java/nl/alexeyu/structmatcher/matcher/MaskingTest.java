package nl.alexeyu.structmatcher.matcher;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import nl.alexeyu.structmatcher.feedback.Feedback;

/**
 * A masked field is compared as strictly as an unmasked one and reported without its values.
 */
public class MaskingTest {

    private static final String SECRET = "alice@example.com";

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({"'***', alice@example.com", "'***', a"})
    public void redactedKeepsNothing(String expected, String value) {
        assertEquals(expected, Maskers.redacted().mask(value));
    }

    @Test
    public void hashIsStableAndDistinguishesValues() {
        var masker = Maskers.hash();
        assertEquals(masker.mask(SECRET), masker.mask(SECRET));
        assertEquals("sha256:ff8d9819fc0e12bf", masker.mask(SECRET));
        assertFalse(masker.mask(SECRET).equals(masker.mask("bob@example.com")));
    }

    @ParameterizedTest(name = "keeping {1} of {0} -> {2} / {3}")
    @CsvSource({"NL-4711, 3, 'NL-***', '***711'", "ab, 2, '***', '***'"})
    public void keepingEndsMasksTheRestAndAShortValueWhole(String value, int count,
            String first, String last) {
        assertEquals(first, Maskers.keepingFirst(count).mask(value));
        assertEquals(last, Maskers.keepingLast(count).mask(value));
    }

    @Test
    public void keepingNothingIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Maskers.keepingFirst(0));
        assertThrows(IllegalArgumentException.class, () -> Maskers.keepingLast(-1));
    }

    @Test
    public void aMaskedFieldStillHasToMatch() {
        var matcher = Matchers.<String>valuesEqual().masking(Maskers.redacted());
        assertTrue(matcher.match("Email", SECRET, SECRET).isEmpty());
        assertFalse(matcher.match("Email", SECRET, "bob@example.com").isEmpty());
    }

    @Test
    public void bothValuesOfAMismatchAreWithheld() {
        var feedback = Matchers.<String>valuesEqual().masking(Maskers.redacted())
                .match("Email", SECRET, "bob@example.com");
        assertEquals(Feedback.nonEqual("Email", "***", "***"), feedback);
    }

    /** The expectation states a condition, not a value, so masking it would say nothing. */
    @Test
    public void aSpecificationIsNotMasked() {
        var feedback = StringMatchers.nonEmpty().masking(Maskers.redacted())
                .match("Email", SECRET, "");
        assertEquals(Feedback.doesNotConform("Email", "***", "A non-empty string"), feedback);
    }

    /** Neither slot of a size mismatch holds data: both are the matcher's own words. */
    @Test
    public void aSizeMismatchIsNotMasked() {
        var feedback = Matchers.<String>listsEqual().masking(Maskers.redacted()).match("Tags",
                List.of("vip"), List.of());
        assertEquals(Feedback.differentCollectionSizes("Tags", 1, 0), feedback);
    }

    /** An absent value is not data: reporting it masked would hide that it was missing. */
    @Test
    public void anAbsentValueStaysAbsent() {
        var feedback = Matchers.<String>valuesEqual().masking(Maskers.redacted()).match("Email",
                SECRET, null);
        assertEquals(Feedback.gotNull("Email", "***"), feedback);
    }

    /** Masking a structure reaches every leaf below it. */
    @Test
    public void aMaskedStructureRedactsItsSubtree() {
        var feedback = ObjectMatcher.forClass(Order.class)
                .with(Matchers.structuresEqual().masking(Maskers.redacted()), "Customer")
                .match(new Order("A-1", new Customer("Alice", SECRET)),
                        new Order("A-1", new Customer("Bob", "bob@example.com")));
        assertEquals(Feedback.composite(Order.class.getName(),
                asList(Feedback.composite("Customer",
                        asList(Feedback.nonEqual("Name", "***", "***"),
                                Feedback.nonEqual("Email", "***", "***"))))),
                feedback);
    }

    /** A map key names the entry that broke, and can be an identifier itself. */
    @Test
    public void aMapKeyIsMaskedAndAListIndexIsNot() {
        var feedback = ObjectMatcher.forClass(Contacts.class)
                .with(Matchers.mapsEqual().masking(Maskers.redacted()), "ByEmail")
                .with(Matchers.listsEqual().masking(Maskers.redacted()), "Tags")
                .match(new Contacts(Map.of(SECRET, "home"), List.of("vip")),
                        new Contacts(Map.of(SECRET, "work"), List.of("staff")));
        assertEquals(Feedback.composite(Contacts.class.getName(),
                asList(Feedback.composite("ByEmail",
                        asList(Feedback.nonEqual("ByEmail[***]", "***", "***"))),
                        Feedback.composite("Tags",
                                asList(Feedback.nonEqual("Tags[0]", "***", "***"))))),
                feedback);
    }

    /** The registration is a path like any other, so one rule reaches every email in the model. */
    @Test
    public void aWildcardPathMasksEveryMatchingField() {
        var feedback = ObjectMatcher.forClass(Order.class)
                .with(Matchers.valuesEqual().masking(Maskers.hash()), "*.Email")
                .match(new Order("A-1", new Customer("Alice", SECRET)),
                        new Order("A-2", new Customer("Alice", "bob@example.com")));
        assertEquals(Feedback.composite(Order.class.getName(),
                asList(Feedback.nonEqual("Id", "A-1", "A-2"),
                        Feedback.composite("Customer",
                                asList(Feedback.nonEqual("Email", "sha256:ff8d9819fc0e12bf",
                                        "sha256:5ff860bf1190596c"))))),
                feedback);
    }

    /**
     * The mismatch is reported at {@code Customer}, above the masked path, so the matcher
     * registered for the email never runs and the leaf holds the whole record. The ancestor
     * withholds it, or masking is one null away from printing everything.
     */
    @Test
    public void aNullAncestorOfAMaskedFieldIsWithheld() {
        var shipment = new Shipment("DHL", "TRACK-1");
        var feedback = ObjectMatcher.forClass(Delivery.class)
                .with(Matchers.valuesEqual().masking(Maskers.redacted()), "Customer.Email")
                .match(new Delivery(new Customer("Alice", SECRET), shipment),
                        new Delivery(null, null));
        assertEquals(Feedback.composite(Delivery.class.getName(),
                asList(Feedback.gotNull("Customer", "***"),
                        // Nothing is masked below Shipment, so it is reported in full.
                        Feedback.gotNull("Shipment", shipment))),
                feedback);
    }

    /** The same hole one level down: an element the list matcher never descended into. */
    @Test
    public void aCollectionElementReportedWholeIsWithheld() {
        var feedback = ObjectMatcher.forClass(Basket.class)
                .with(Matchers.valuesEqual().masking(Maskers.redacted()), "Customers.Email")
                .match(new Basket(asList(new Customer("Alice", SECRET))),
                        new Basket(Collections.singletonList(null)));
        assertEquals(Feedback.composite(Basket.class.getName(),
                asList(Feedback.composite("Customers",
                        asList(Feedback.gotNull("Customers[0]", "***"))))),
                feedback);
    }

    /** A map entry the target lacks is reported whole too; its key still says which one. */
    @Test
    public void aMissingMapEntryIsWithheldAndKeepsItsKey() {
        var feedback = ObjectMatcher.forClass(Registry.class)
                .with(Matchers.valuesEqual().masking(Maskers.redacted()), "ByRegion.Email")
                .match(new Registry(Map.of("eu", new Customer("Alice", SECRET))),
                        new Registry(Map.of()))
                ;
        assertEquals(Feedback.composite(Registry.class.getName(),
                asList(Feedback.composite("ByRegion",
                        asList(Feedback.gotNull("ByRegion[eu]", "***"))))),
                feedback);
    }

    /** The ancestor withholds the objects it reports whole; its simple fields keep theirs. */
    @Test
    public void anAncestorStillReportsItsSimpleFields() {
        var feedback = ObjectMatcher.forClass(Order.class)
                .with(Matchers.valuesEqual().masking(Maskers.redacted()), "Customer.Email")
                .match(new Order("A-1", new Customer("Alice", SECRET)),
                        new Order("A-2", new Customer("Alicia", "bob@example.com")));
        assertEquals(Feedback.composite(Order.class.getName(),
                asList(Feedback.nonEqual("Id", "A-1", "A-2"),
                        Feedback.composite("Customer",
                                asList(Feedback.nonEqual("Name", "Alice", "Alicia"),
                                        Feedback.nonEqual("Email", "***", "***"))))),
                feedback);
    }

    /**
     * An indirect matcher is picked out of the stack by its type and fed the whole structures, so
     * a masking wrapper around one used to hand the fetchers a single property value.
     */
    @Test
    public void anIndirectMatcherMasksTheValuesItDerives() {
        var indirect = Matchers.<Order, String>indirectMatcher("Email", Matchers.valuesEqual(),
                order -> order.customer().email(), Order::id);
        var spec = ObjectMatcher.forClass(Order.class).with(indirect.masking(Maskers.redacted()),
                "Customer.Email");

        assertTrue(spec.masksValues(), "masking inside an indirect matcher still counts");
        assertEquals(Feedback.composite(Order.class.getName(),
                asList(Feedback.nonEqual("Id", "A-1", "A-2"),
                        Feedback.composite("Customer",
                                asList(Feedback.nonEqual("Email", "***", "***"))))),
                spec.match(new Order("A-1", new Customer("Alice", SECRET)),
                        new Order("A-2", new Customer("Alice", SECRET))));
    }

    public record Customer(String name, String email) {
    }

    public record Shipment(String carrier, String trackingCode) {
    }

    public record Delivery(Customer customer, Shipment shipment) {
    }

    public record Basket(List<Customer> customers) {
    }

    public record Registry(Map<String, Customer> byRegion) {
    }

    public record Order(String id, Customer customer) {
    }

    public record Contacts(Map<String, String> byEmail, List<String> tags) {
    }

}
