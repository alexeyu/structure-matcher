package nl.alexeyu.structmatcher.matcher;

/**
 * Holds the stack of the comparison running on this thread. Wrappers like {@code and} and the
 * {@code normalizing} family cannot pass a context through the {@link Matcher} interface, so keep
 * one {@code match()} on one thread: on another it finds a bare stack and skips custom matchers.
 */
final class MatchingStackHolder {

    private static final ThreadLocal<MatchingStack<Object>> context =
            ThreadLocal.withInitial(() -> DefaultMatchingStack.BARE);

    private MatchingStackHolder() {
    }

    static void set(MatchingStack<Object> stack) {
        context.set(stack);
    }

    /**
     * Puts back the stack a comparison found on entry, so a custom matcher can run a nested
     * comparison without stranding the outer one. Drops the thread's entry when the outermost
     * comparison ends, so pooled threads keep none.
     */
    static void restore(MatchingStack<Object> previous) {
        if (previous == DefaultMatchingStack.BARE) {
            context.remove();
        } else {
            context.set(previous);
        }
    }

    static MatchingStack<Object> get() {
        return context.get();
    }

}
