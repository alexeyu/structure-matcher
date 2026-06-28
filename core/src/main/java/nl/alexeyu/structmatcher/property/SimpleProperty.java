package nl.alexeyu.structmatcher.property;

public final class SimpleProperty implements Property {

    private final String name;

    public SimpleProperty(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getValue(Object obj) {
        return obj;
    }

    @Override
    public boolean isList() {
        return false;
    }

    @Override
    public boolean isMap() {
        return false;
    }

    @Override
    public boolean isSet() {
        return false;
    }

    @Override
    public boolean isArray() {
        return false;
    }

    @Override
    public boolean isOptional() {
        return false;
    }

    @Override
    public boolean isSimple() {
        return true;
    }
}
