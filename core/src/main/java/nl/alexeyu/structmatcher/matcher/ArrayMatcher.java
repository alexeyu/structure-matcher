package nl.alexeyu.structmatcher.matcher;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Matches two arrays element by element by adapting them to lists and delegating to
 * {@link ListMatcher}. It reads the elements through {@link Array}, so an array of primitives
 * behaves like an array of objects, boxing on the way. Elements go through the usual logic, simple
 * ones by equality and complex ones structurally, and each mismatch lands under
 * <code>property[index]</code>. Neither array may be <code>null</code>, though their elements may.
 */
public final class ArrayMatcher implements Matcher<Object> {

    private final ListMatcher<Object> listMatcher = new ListMatcher<>();

    @Override
    public FeedbackNode match(String property, Object expected, Object actual) {
        return listMatcher.match(property, toList(expected), toList(actual));
    }

    private List<Object> toList(Object array) {
        var length = Array.getLength(array);
        var list = new ArrayList<Object>(length);
        for (int i = 0; i < length; i++) {
            list.add(Array.get(array, i));
        }
        return list;
    }

}
