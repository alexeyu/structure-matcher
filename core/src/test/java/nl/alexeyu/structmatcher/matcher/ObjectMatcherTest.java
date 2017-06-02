package nl.alexeyu.structmatcher.matcher;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.matcher.Matcher;
import nl.alexeyu.structmatcher.matcher.Matchers;
import nl.alexeyu.structmatcher.matcher.ObjectMatcher;

public class ObjectMatcherTest {

    private final Structure expected = new Structure(Color.BLACK, asList("a"), new Substructure(true));

    private final Structure actual = new Structure(Color.BLACK, asList("b"), new Substructure(false));
    
    private final Matcher<Object> ignore = Matchers.anyValue();
    
    @Test
    public void ignoreAllPropertiesOnTheHighestLevel() {
        FeedbackNode feedback = ObjectMatcher.forClass(Structure.class)
                .withMatcher(ignore, "Color")
                .withMatcher(ignore, "Strings")
                .withMatcher(ignore, "Sub")
                .match(expected, actual);
        assertTrue(feedback.isEmpty());
    }

    @Test
    public void ignoreAllPropertiesOnTheLowestLevel() {
        FeedbackNode feedback = ObjectMatcher.forClass(Structure.class)
                .withMatcher(ignore, "Color")
                .withMatcher(ignore, "Strings")
                .withMatcher(ignore, "Sub", "Bool")
                .match(expected, actual);
        assertTrue(feedback.isEmpty());
    }

}
