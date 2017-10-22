package nl.alexeyu.structmatcher.property;

import java.util.List;

abstract class AbstractPath {

    final List<String> list;
    
    public AbstractPath(List<String> list) {
        this.list = list;
    }

    public final boolean isEmpty() {
        return list.isEmpty();
    }
    
    protected final void checkNotEmpty() {
        if (isEmpty()) {
            throw new IllegalStateException("Cannot perform operation on an empty path");
        }
    }

    public final String head() {
        checkNotEmpty();
        return list.get(0);
    }
    
    @Override
    public int hashCode() {
        return list.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && obj.getClass() == this.getClass()) {
            AbstractPath other = (AbstractPath) obj;
            return this.list.equals(other.list);
        }
        return false;
    }

    @Override
    public String toString() {
        return list.toString();
    }

}
