package nl.alexeyu.structmatcher.matcher;

/**
 * Test model carrying both a real {@code String getValue()} and the bridge {@code Object
 * getValue()} the compiler adds for the covariant override.
 */
public final class StringValueBox implements ValueBox<String> {

    private final String value;

    public StringValueBox(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

}
