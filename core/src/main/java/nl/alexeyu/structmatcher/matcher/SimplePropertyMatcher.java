package nl.alexeyu.structmatcher.matcher;

import java.util.function.BiPredicate;
import java.util.function.Function;

import nl.alexeyu.structmatcher.feedback.Feedback;
import nl.alexeyu.structmatcher.feedback.FeedbackNode;

public class SimplePropertyMatcher implements Matcher {
    
    private static final BiPredicate<Object, Object> EQ = (e, a) -> e.equals(a); 
    
    private final PartialMatcher nullAwareMatcher = new NullAwareMatcher();
    
    private final BiPredicate<Object, Object> predicate;
    
    private final Function<Object, Object> mapper;
    
    public SimplePropertyMatcher() {
        this(EQ);
    }

    public SimplePropertyMatcher(BiPredicate<Object, Object> predicate) {
        this(predicate, Function.identity());
    }

    public SimplePropertyMatcher(Function<Object, Object> mapper) {
        this(EQ, mapper);
    }

    public SimplePropertyMatcher(BiPredicate<Object, Object> predicate, Function<Object, Object> mapper) {
        this.predicate = predicate;
        this.mapper = mapper;
    }

    @Override
    public FeedbackNode match(String property, Object expected, Object actual) {
        return nullAwareMatcher
                .maybeMatch(property, expected, actual)
                .orElseGet(() -> doMatch(property, expected, actual)); 
    }
    
    private FeedbackNode doMatch(String property, Object expected, Object actual) {
        return predicate.test(mapper.apply(expected), mapper.apply(actual))
                ? Feedback.empty(property)
                : Feedback.nonEqual(property, expected, actual);
    }

}
