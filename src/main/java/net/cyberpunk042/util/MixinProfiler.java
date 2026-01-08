package net.cyberpunk042.util;

/**
 * Helper for profiling mixins with minimal boilerplate.
 * 
 * Usage in mixin:
 * {@code
 * var ctx = MixinProfiler.enter("MyMixin.methodName");
 * try {
 *     // your code
 * } finally {
 *     ctx.exit();
 * }
 * }
 * 
 * Or for quick early-exit patterns:
 * {@code
 * var ctx = MixinProfiler.enter("MyMixin.methodName");
 * if (earlyExitCondition) {
 *     return ctx.exitWith(defaultValue);
 * }
 * // rest of code
 * return ctx.exitWith(result);
 * }
 */
public final class MixinProfiler {
    
    private static volatile boolean enabled = true;
    
    private MixinProfiler() {}
    
    /**
     * Start profiling a mixin method.
     * @param label Unique identifier like "ZombieMixin.tick"
     * @return A context that must be exited
     */
    public static Context enter(String label) {
        if (!enabled) return NOOP;
        SuperProfiler.start("Mixin:" + label);
        return new ActiveContext(label);
    }
    
    /**
     * Quick count without timing - for very hot paths
     */
    public static void count(String label) {
        if (!enabled) return;
        SuperProfiler.start("Mixin:" + label);
        SuperProfiler.end("Mixin:" + label);
    }
    
    public static void setEnabled(boolean value) {
        enabled = value;
    }
    
    // ========================================================================
    // Context interface for clean exit patterns
    // ========================================================================
    
    public interface Context {
        void exit();
        <T> T exitWith(T value);
        void exitVoid();
    }
    
    private static final Context NOOP = new Context() {
        @Override public void exit() {}
        @Override public <T> T exitWith(T value) { return value; }
        @Override public void exitVoid() {}
    };
    
    private static final class ActiveContext implements Context {
        private final String label;
        
        ActiveContext(String label) {
            this.label = label;
        }
        
        @Override
        public void exit() {
            SuperProfiler.end("Mixin:" + label);
        }
        
        @Override
        public <T> T exitWith(T value) {
            SuperProfiler.end("Mixin:" + label);
            return value;
        }
        
        @Override
        public void exitVoid() {
            SuperProfiler.end("Mixin:" + label);
        }
    }
}
