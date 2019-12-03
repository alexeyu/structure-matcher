package nl.alexeyu.structmatcher.matcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Supplier;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;
import nl.alexeyu.structmatcher.property.PropertyPathPattern;
import nl.alexeyu.structmatcher.property.SimpleProperty;

@RunWith(MockitoJUnitRunner.class)
public class ContextAwareMatcherTest {

    @Mock
    private Matcher<Object> defaultMatcher;

    @Mock
    private Matcher<Object> customMatcher;

    private ContextAwareMatcher<Object> contextAwareMatcher;

    private Supplier<Matcher<Object>> defaultMatcherSupplier = () -> defaultMatcher;

    @Test
    public void callsDefaultMatcherIfCustomOneIsNotRegistered() {
        MatchingStack context = DefaultMatchingStack.BARE;
        contextAwareMatcher = new ContextAwareMatcher<Object>(
                new SimpleProperty("test"), context, defaultMatcherSupplier);
        FeedbackNode expectedFeedback = Feedback.empty("test");
        when(defaultMatcher.match("test", "a", "b")).thenReturn(expectedFeedback);
        FeedbackNode feedback = contextAwareMatcher.match( "a", "b");
        verify(defaultMatcher).match("test", "a", "b");
        assertSame(expectedFeedback, feedback);
    }

    @Test
    public void callsCustomMatcherIfRegistered() {
        MatchingStack context = new DefaultMatchingStack(null, null,
                Collections.singletonMap(new PropertyPathPattern(Arrays.asList("specific")), customMatcher));
        contextAwareMatcher = new ContextAwareMatcher<Object>(
                new SimpleProperty("specific"), context, defaultMatcherSupplier);
        FeedbackNode expectedFeedback = Feedback.empty("test");
        when(customMatcher.match("specific", "a", "b")).thenReturn(expectedFeedback);
        FeedbackNode feedback = contextAwareMatcher.match("a", "b");
        verify(customMatcher).match("specific", "a", "b");
        assertSame(expectedFeedback, feedback);
    }

    @Test
    public void callsCustomIndirectMatcherIfRegistered() {
        IndirectMatcher<Structure, Color> colorMatcher = new IndirectMatcher<>(
                "check color",
                Matchers.valuesEqual(), Structure::getColor, Structure::getColor);
        Structure expected = new Structure(Color.BLACK, Arrays.asList(), new Substructure(false));
        Structure actual = new Structure(Color.WHITE, Arrays.asList(), new Substructure(false));
        MatchingStack context = new DefaultMatchingStack(expected, actual,
                Collections.singletonMap(new PropertyPathPattern(Arrays.asList("indirect")), colorMatcher));
        contextAwareMatcher = new ContextAwareMatcher<Object>(
                new SimpleProperty("indirect"), context, defaultMatcherSupplier);
        FeedbackNode expectedFeedback = Feedback.nonEqual("check color", Color.BLACK, Color.WHITE);
        FeedbackNode feedback = contextAwareMatcher.match("to-be-ignored-1", "to-be-ignored-2");
        assertEquals(expectedFeedback, feedback);
    }

}