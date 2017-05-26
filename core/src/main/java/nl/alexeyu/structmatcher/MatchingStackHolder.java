package nl.alexeyu.structmatcher;

public class MatchingStackHolder {

    private static final ThreadLocal<MatchingStack> context = new ThreadLocal<>();
    
    static {
        clear();
    }

    public static void set(MatchingStack stack) {
        context.set(stack);
    }

    public static void clear() {
        context.set(DefaultMatchingStack.BARE);
    }
    
    public static MatchingStack get() {
        return context.get();
    }

}
