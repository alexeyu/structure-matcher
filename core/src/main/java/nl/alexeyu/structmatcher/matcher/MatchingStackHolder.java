package nl.alexeyu.structmatcher.matcher;

final class MatchingStackHolder {

    private static final ThreadLocal<MatchingStack> context = new ThreadLocal<>();
    
    static {
        clear();
    }

    private MatchingStackHolder() {
    }
    
    static void set(MatchingStack stack) {
        context.set(stack);
    }

    static void clear() {
        context.set(DefaultMatchingStack.BARE);
    }
    
    static MatchingStack get() {
        return context.get();
    }

}
