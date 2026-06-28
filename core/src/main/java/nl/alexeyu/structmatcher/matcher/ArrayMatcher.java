package nl.alexeyu.structmatcher.matcher;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import nl.alexeyu.structmatcher.feedback.FeedbackNode;

/**
 * Matches two arrays element by element by adapting them to lists and delegating to
 * {@link ListMatcher}. Reflection ({@link Array}) is used to read the elements, so arrays of
 * objects and of primitives are handled the same way (primitive elements are auto-boxed). Elements
 * are matched the way they are everywhere else: simple values by equality, complex values
 * structurally; each is reported under <code>property[index]</code>. The arrays themselves must not
 * be <code>null</code> (their elements may be).
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
