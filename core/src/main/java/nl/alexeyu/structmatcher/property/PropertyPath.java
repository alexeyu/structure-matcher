package nl.alexeyu.structmatcher.property;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the comparison currently is: the properties it has descended into, root first. The
 * matching stack pushes a property before matching it and pops it after.
 */
public final class PropertyPath extends AbstractPath {

    public PropertyPath() {
        super(new ArrayList<>());
    }

    public PropertyPath(List<String> list) {
        super(new ArrayList<>(list));
    }

    public void push(String element) {
        list.add(element);
    }

    public void pop() {
        if (!isEmpty()) {
            list.remove(list.size() - 1);
        }
    }

    public PropertyPath tail() {
        checkNotEmpty();
        return new PropertyPath(list.subList(1, list.size()));
    }

}
