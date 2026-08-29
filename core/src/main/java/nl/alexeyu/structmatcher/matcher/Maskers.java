package nl.alexeyu.structmatcher.matcher;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Factory of common maskers. Each works off the value's <code>toString()</code>, so it fits a
 * field of any type.
 *
 * @see Matcher#masking(Masker)
 */
public final class Maskers {

    /** What every masker here writes in place of what it withholds. */
    public static final String MASK = "***";

    private static final int HASH_LENGTH = 16;

    private Maskers() {
    }

    /** Replaces the whole value with {@link #MASK}, keeping nothing of it. */
    public static Masker redacted() {
        return fixed(MASK);
    }

    /** Replaces the whole value with a string of your own. */
    public static Masker fixed(String replacement) {
        return value -> replacement;
    }

    /**
     * Replaces the value with a truncated SHA-256 of its text, prefixed <code>sha256:</code>. The
     * digest is stable across runs and JVMs, so equal values stay equal and different ones differ:
     * a batch report still counts how often a field diverged, without holding what it held.
     */
    public static Masker hash() {
        return value -> "sha256:" + digest(String.valueOf(value));
    }

    /**
     * Keeps the first <code>count</code> characters and masks the rest, e.g. an order id as
     * <code>NL-***</code>. A value no longer than that is masked whole, since keeping all of it
     * would withhold nothing.
     */
    public static Masker keepingFirst(int count) {
        checkCount(count);
        return value -> {
            var text = String.valueOf(value);
            return text.length() > count ? text.substring(0, count) + MASK : MASK;
        };
    }

    /**
     * Keeps the last <code>count</code> characters and masks the rest, e.g. a card number as
     * <code>***1234</code>. A value no longer than that is masked whole, since keeping all of it
     * would withhold nothing.
     */
    public static Masker keepingLast(int count) {
        checkCount(count);
        return value -> {
            var text = String.valueOf(value);
            return text.length() > count ? MASK + text.substring(text.length() - count) : MASK;
        };
    }

    private static void checkCount(int count) {
        if (count < 1) {
            throw new IllegalArgumentException("Keep at least one character, got " + count);
        }
    }

    private static String digest(String text) {
        try {
            var sha256 = MessageDigest.getInstance("SHA-256");
            var digest = sha256.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, HASH_LENGTH);
        } catch (NoSuchAlgorithmException e) {
            // Unreachable: every Java platform ships SHA-256.
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

}
