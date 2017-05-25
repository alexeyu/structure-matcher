package nl.alexeyu.structmatcher.matcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

@RunWith(MockitoJUnitRunner.class)
public class ContextAwareMatcherTest {

    @Mock
    private Matcher<Object> defaultMatcher;

    @Mock
    private Matcher<Object> customMatcher;

    @Mock
    private Context context;

    private ContextAwareMatcher<Object> contextAwareMatcher;
    
    @Before
    public void setUp() {
        contextAwareMatcher = new ContextAwareMatcher<>(context, defaultMatcher);
    }
    
    @Test
    public void callsDefaultMatcherIfCustomOneIsNotRegistered() {
        FeedbackNode expectedFeedback = Feedback.empty("test");
        when(context.push("test")).thenReturn(Optional.empty());
        when(defaultMatcher.match("test", "a", "b")).thenReturn(expectedFeedback);
        FeedbackNode feedback = contextAwareMatcher.match("test", "a", "b");
        verify(context).push("test");
        verify(defaultMatcher).match("test", "a", "b");
        verify(context).pop();
        assertSame(expectedFeedback, feedback);
    }

    @Test
    public void callsCustomMatcherIfRegistered() {
        FeedbackNode expectedFeedback = Feedback.empty("test");
        when(context.push("specific")).thenReturn(Optional.of(customMatcher));
        when(customMatcher.match("specific", "a", "b")).thenReturn(expectedFeedback);
        FeedbackNode feedback = contextAwareMatcher.match("specific", "a", "b");
        verify(context).push("specific");
        verify(customMatcher).match("specific", "a", "b");
        verify(context).pop();
        assertSame(expectedFeedback, feedback);
    }

    @Test
    public void callsCustomAndThenDefaultMatcherIfNecessary() {
        when(context.push("specific")).thenReturn(Optional.of(customMatcher));
        when(customMatcher.match("specific", "a", "b")).thenReturn(Feedback.useDefault());
        when(defaultMatcher.match("specific", "a", "b")).thenReturn(Feedback.nonEqual("specific", "a", "b"));
        FeedbackNode feedback = contextAwareMatcher.match("specific", "a", "b");
        verify(context).push("specific");
        verify(customMatcher).match("specific", "a", "b");
        verify(defaultMatcher).match("specific", "a", "b");
        verify(context).pop();
        assertEquals(Feedback.nonEqual("specific", "a", "b"), feedback);
    }

}
