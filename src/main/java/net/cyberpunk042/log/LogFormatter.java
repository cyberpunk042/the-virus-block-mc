package net.cyberpunk042.log;

/**
 * Functional interface for custom type formatting in logs.
 */
@FunctionalInterface
public interface LogFormatter<T> {
    String format(T value);
}
