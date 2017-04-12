package nl.alexeyu.structmatcher;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.matcher.Color;
import nl.alexeyu.structmatcher.matcher.IgnoreMatcher;
import nl.alexeyu.structmatcher.matcher.Structure;
import nl.alexeyu.structmatcher.matcher.Substructure;

public class ObjectMatcherTest {

    private Structure expected = new Structure(Color.BLACK, asList("a"), new Substructure(true));

    private Structure actual = new Structure(Color.BLACK, asList("b"), new Substructure(false));
    
    @Test
    public void ignoreAllPropertiesOnTheHighestLevel() {
        FeedbackNode feedback = ObjectMatcher.forObject("test")
                .withMatcher(new IgnoreMatcher(), "Color")
                .withMatcher(new IgnoreMatcher(), "Strings")
                .withMatcher(new IgnoreMatcher(), "Sub")
                .match(expected, actual);
        assertTrue(feedback.isEmpty());
    }

    @Test
    public void ignoreAllPropertiesOnTheLowestLevel() {
        FeedbackNode feedback = ObjectMatcher.forObject("test")
                .withMatcher(new IgnoreMatcher(), "Color")
                .withMatcher(new IgnoreMatcher(), "Strings")
                .withMatcher(new IgnoreMatcher(), "Sub", "Bool")
                .match(expected, actual);
        assertTrue(feedback.isEmpty());
    }

}
