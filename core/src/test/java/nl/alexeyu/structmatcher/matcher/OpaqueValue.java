package nl.alexeyu.structmatcher.matcher;

/** Test model with neither discoverable properties nor an {@code equals} of its own. */
public final class OpaqueValue {

    private final String text;

    public OpaqueValue(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }

    @Override
    public String toString() {
        return "OpaqueValue[" + text + "]";
    }

}
