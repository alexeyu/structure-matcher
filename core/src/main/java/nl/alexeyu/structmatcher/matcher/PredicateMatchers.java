package nl.alexeyu.structmatcher.matcher;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

public class PredicateMatchers {
    
    private PredicateMatchers() {
    }

    public static Matcher integer() {
        return new PredicateMatcher(v -> new ToInteger().apply(v).isPresent(), "Must be an integer");
    }

    public static Matcher positiveInteger() {
        return new PredicateMatcher(
                v -> new ToInteger().apply(v).orElse(Integer.MIN_VALUE) > 0, "Must be a positive integer");
    }

    public static Matcher integerInRange(int minExclusive, int maxExclusive) {
        return new PredicateMatcher(
                v -> new ToInteger().andThen(new Within(minExclusive, maxExclusive)).apply(v),
                String.format("Must be bigger than %s but smaller than %s", minExclusive, maxExclusive));
    }
    
    public static Matcher regex(String expr) {
        return new PredicateMatcher(
                v -> Pattern.compile(expr).matcher(String.valueOf(v)).matches(),
                "Must match the regular expression: " + expr);
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
