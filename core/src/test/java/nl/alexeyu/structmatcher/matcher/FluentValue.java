package nl.alexeyu.structmatcher.matcher;

import java.util.Objects;

/**
 * Test model with no discoverable properties (its accessor carries no {@code get}/{@code is}
 * prefix) but a meaningful {@code equals}.
 */
public final class FluentValue {

    private final String text;

    public FluentValue(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof FluentValue other && Objects.equals(this.text, other.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text);
    }

    @Override
    public String toString() {
        return "FluentValue[" + text + "]";
    }

}
