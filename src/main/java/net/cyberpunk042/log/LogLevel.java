package net.cyberpunk042.log;

/**
 * Log levels for channel and topic filtering.
 * Ordered from most restrictive (OFF) to most verbose (TRACE).
 */
public enum LogLevel {
    OFF(0),
    ERROR(1),
    WARN(2),
    INFO(3),
    DEBUG(4),
    TRACE(5);
    
    private final int priority;
    
    LogLevel(int priority) {
        this.priority = priority;
    }
    
    public int priority() { return priority; }
    
    public boolean includes(LogLevel other) {
        return this.priority >= other.priority;
    }
    
    public static LogLevel parse(String value) {
        return parse(value, INFO);
    }
    
    public static LogLevel parse(String value, LogLevel defaultLevel) {
        if (value == null || value.isBlank()) return defaultLevel;
        String upper = value.trim().toUpperCase();
        for (LogLevel level : values()) {
            if (level.name().equals(upper)) return level;
        }
        return defaultLevel;
    }
}
