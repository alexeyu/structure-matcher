package nl.alexeyu.structmatcher;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.matcher.Color;
import nl.alexeyu.structmatcher.matcher.Matcher;
import nl.alexeyu.structmatcher.matcher.Matchers;
import nl.alexeyu.structmatcher.matcher.Structure;
import nl.alexeyu.structmatcher.matcher.Substructure;

public class ObjectMatcherTest {

    private final Structure expected = new Structure(Color.BLACK, asList("a"), new Substructure(true));

    private final Structure actual = new Structure(Color.BLACK, asList("b"), new Substructure(false));
    
    private final Matcher ignore = Matchers.anyValue();
    
    @Test
    public void ignoreAllPropertiesOnTheHighestLevel() {
        FeedbackNode feedback = ObjectMatcher.forObject("test")
                .withMatcher(ignore, "Color")
                .withMatcher(ignore, "Strings")
                .withMatcher(ignore, "Sub")
                .match(expected, actual);
        assertTrue(feedback.isEmpty());
    }

    @Test
    public void ignoreAllPropertiesOnTheLowestLevel() {
        FeedbackNode feedback = ObjectMatcher.forObject("test")
                .withMatcher(ignore, "Color")
                .withMatcher(ignore, "Strings")
                .withMatcher(ignore, "Sub", "Bool")
                .match(expected, actual);
        assertTrue(feedback.isEmpty());
    }

}
