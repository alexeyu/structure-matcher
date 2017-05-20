package nl.alexeyu.structmatcher.matcher;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import nl.alexeyu.structmatcher.Context;
import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

@RunWith(MockitoJUnitRunner.class)
public class ContextAwareMatcherTest {

    @Mock
    private Matcher<String> defaultMatcher;

    @Mock
    private Matcher<String> customMatcher;

    @Mock
    private Context context;

    private ContextAwareMatcher<String> contextAwareMatcher;
    
    @Before
    public void setUp() {
        contextAwareMatcher = new ContextAwareMatcher<>(context, defaultMatcher);
    }
    
    @Test
    public void callsDefaultMatcherIfCustomOneIsNotRegistered() {
        FeedbackNode expectedFeedback = Feedback.empty("test");
        when(context.getCustomMatcher()).thenReturn(Optional.empty());
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
        when(context.<String>getCustomMatcher()).thenReturn(Optional.of(customMatcher));
        when(customMatcher.match("specific", "a", "b")).thenReturn(expectedFeedback);
        FeedbackNode feedback = contextAwareMatcher.match("specific", "a", "b");
        verify(context).push("specific");
        verify(customMatcher).match("specific", "a", "b");
        verify(context).pop();
        assertSame(expectedFeedback, feedback);
    }

}
