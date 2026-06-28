package nl.alexeyu.structmatcher.property;

public interface Property {

    String getName();

    Object getValue(Object obj);

    boolean isList();

    boolean isMap();

    boolean isSet();

    boolean isSimple();
}
