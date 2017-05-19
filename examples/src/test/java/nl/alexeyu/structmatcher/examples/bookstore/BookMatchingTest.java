package nl.alexeyu.structmatcher.examples.bookstore;

import static org.apache.commons.lang3.StringUtils.stripAccents;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import nl.alexeyu.structmatcher.ObjectMatcher;
import nl.alexeyu.structmatcher.examples.bookstore.model.Author;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.matcher.Matchers;

public class BookMatchingTest {
    
    private final Author francoiseSagan = new Author("Françoise", "Sagan");
    private final Author francoiseSaganNormalized = new Author("Francoise", "Sagan");
    
    @Test
    public void notNormalizedStringsDoNotMatchByDefault() {
        FeedbackNode feedback = ObjectMatcher.forObject("author")
                .match(francoiseSaganNormalized, francoiseSagan);
        assertFalse(feedback.isEmpty());
    }

    @Test
    public void notNormalizedStringsDoMatchWithNormalizationAwareMatcher() {
        FeedbackNode feedback = ObjectMatcher.forObject("author")
                .with(Matchers.normalizing(name -> stripAccents(name.toString()), 
                        Matchers.valuesEqual()), "FirstName")
                .match(francoiseSaganNormalized, francoiseSagan);
        assertTrue(feedback.isEmpty());
    }

}
