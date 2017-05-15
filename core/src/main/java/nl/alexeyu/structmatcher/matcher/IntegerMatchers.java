package nl.alexeyu.structmatcher.matcher;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class IntegerMatchers {
    
    private static final ToInteger TO_INT = new ToInteger(); 
    
    private IntegerMatchers() {
    }

    public static Matcher any() {
        return new PredicateMatcher(v -> TO_INT.apply(v).isPresent(), "Must be an integer");
    }

    public static Matcher positive() {
        return new PredicateMatcher(
                v -> TO_INT.apply(v).orElse(Integer.MIN_VALUE) > 0, "Must be a positive integer");
    }

    public static Matcher inRange(int minExclusive, int maxExclusive) {
        return new PredicateMatcher(
                v -> TO_INT.andThen(new Within(minExclusive, maxExclusive)).apply(v),
                String.format("Must be bigger than %s but smaller than %s", minExclusive, maxExclusive));
    }
    
    public static Matcher oneOf(Integer... possibleValues) {
        return new PredicateMatcher(
                v -> TO_INT.andThen(new OneOf(possibleValues)).apply(v),
                String.format("Must be one of the following values: %s", Arrays.asList(possibleValues)));
    }

    private static class Within implements Function<Optional<Integer>, Boolean> {
        
        private final int minExclusive;

        private final int maxExclusive;

        public Within(int minExclusive, int maxExclusive) {
            this.minExclusive = minExclusive;
            this.maxExclusive = maxExclusive;
        }

        @Override
        public Boolean apply(Optional<Integer> t) {
            return t.isPresent() && t.get() > minExclusive && t.get() < maxExclusive;
        }
        
    }

    private static class OneOf implements Function<Optional<Integer>, Boolean> {
        
        private final Set<Integer> possibleValues;

        public OneOf(Integer... possibleValues) {
            this.possibleValues = Arrays.stream(possibleValues).collect(Collectors.toSet());
        }

        @Override
        public Boolean apply(Optional<Integer> t) {
            return t.isPresent() && possibleValues.contains(t.get());
        }
        
    }
    
    private static class ToInteger implements Function<Object, Optional<Integer>> {

        @Override
        public Optional<Integer> apply(Object t) {
            try {
                return Optional.of(
                        Integer.valueOf(
                                String.valueOf(t)));
            } catch (NumberFormatException ex) {
                return Optional.empty();
            }
        }
        
    }

    
}