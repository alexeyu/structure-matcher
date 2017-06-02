package nl.alexeyu.structmatcher.examples.bookstore;

import static org.apache.commons.lang3.StringUtils.stripAccents;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import nl.alexeyu.structmatcher.examples.bookstore.model.Author;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.matcher.Matchers;
import nl.alexeyu.structmatcher.matcher.ObjectMatcher;

public class BookMatchingTest {
    
    private final Author francoiseSagan = new Author("Françoise", "Sagan");
    private final Author francoiseSaganNormalized = new Author("Francoise", "Sagan");
    
    @Test
    public void notNormalizedStringsDoNotMatchByDefault() {
        FeedbackNode feedback = ObjectMatcher.forClass(Author.class)
                .match(francoiseSaganNormalized, francoiseSagan);
        assertFalse(feedback.isEmpty());
    }

    @Test
    public void notNormalizedStringsDoMatchWithNormalizationAwareMatcher() {
        FeedbackNode feedback = ObjectMatcher.forClass(Author.class)
                .with(Matchers.<String>normalizing(name -> stripAccents(name), 
                        Matchers.valuesEqual()), "FirstName")
                .match(francoiseSaganNormalized, francoiseSagan);
        assertTrue(feedback.isEmpty());
    }

}
