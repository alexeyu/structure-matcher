package nl.alexeyu.structmatcher.matcher;

import java.lang.reflect.Array;
import java.util.List;
import java.util.stream.IntStream;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Matches two arrays element by element by adapting them to lists and delegating to
 * {@link ListMatcher}. Reading through {@link Array} boxes primitives, so an array of them behaves
 * like an array of objects. Neither array may be <code>null</code>, though their elements may.
 */
public final class ArrayMatcher implements Matcher<Object> {

    private final ListMatcher<Object> listMatcher = new ListMatcher<>();

    @Override
    public FeedbackNode match(String property, Object expected, Object actual) {
        return listMatcher.match(property, toList(expected), toList(actual));
    }

    private static List<Object> toList(Object array) {
        return IntStream.range(0, Array.getLength(array))
                .mapToObj(i -> Array.get(array, i))
                .toList();
    }

}
