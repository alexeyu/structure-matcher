package nl.alexeyu.structmatcher.matcher;

final class MatchingStackHolder {

    private static final ThreadLocal<MatchingStack<Object>> context = new ThreadLocal<>();

    static {
        clear();
    }

    private MatchingStackHolder() {
    }

    static void set(MatchingStack<Object> stack) {
        context.set(stack);
    }

    static void clear() {
        context.set(DefaultMatchingStack.BARE);
    }

    static MatchingStack<Object> get() {
        return context.get();
    }

}
