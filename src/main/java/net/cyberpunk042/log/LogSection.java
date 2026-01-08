package net.cyberpunk042.log;

/**
 * Interface for custom log sections in formatted output.
 */
@FunctionalInterface
public interface LogSection {
    String render();
}
