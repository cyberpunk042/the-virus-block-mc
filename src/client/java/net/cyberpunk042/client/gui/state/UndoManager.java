package net.cyberpunk042.client.gui.state;

import net.cyberpunk042.field.FieldDefinition;
import net.cyberpunk042.log.Logging;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Simple undo/redo manager for field definitions.
 * Stores immutable definition snapshots.
 */
public class UndoManager {
    
    private final int maxSize;
    private final Deque<FieldDefinition> undoStack = new ArrayDeque<>();
    private final Deque<FieldDefinition> redoStack = new ArrayDeque<>();
    
    public UndoManager(int maxSize) {
        this.maxSize = maxSize;
    }
    
    /**
     * Push current state before making a change.
     */
    public void push(FieldDefinition state) {
        undoStack.push(state);
        redoStack.clear(); // New change invalidates redo
        
        // Trim to max size
        while (undoStack.size() > maxSize) {
            undoStack.removeLast();
        }
        
        Logging.GUI.topic("undo").trace("Pushed state, stack size: {}", undoStack.size());
    }
    
    /**
     * Undo last change.
     * @param current The current state to move to redo stack
     * @return The previous state, or current if nothing to undo
     */
    public FieldDefinition undo(FieldDefinition current) {
        if (undoStack.isEmpty()) {
            return current;
        }
        
        redoStack.push(current);
        FieldDefinition previous = undoStack.pop();
        Logging.GUI.topic("undo").debug("Undo performed, {} remaining", undoStack.size());
        return previous;
    }
    
    /**
     * Redo last undone change.
     * @param current The current state to move to undo stack
     * @return The next state, or current if nothing to redo
     */
    public FieldDefinition redo(FieldDefinition current) {
        if (redoStack.isEmpty()) {
            return current;
        }
        
        undoStack.push(current);
        FieldDefinition next = redoStack.pop();
        Logging.GUI.topic("undo").debug("Redo performed");
        return next;
    }
    
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }
    
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
    
    public void clear() {
        undoStack.clear();
        redoStack.clear();
        Logging.GUI.topic("undo").trace("Stacks cleared");
    }
    
    public int undoSize() {
        return undoStack.size();
    }
    
    public int redoSize() {
        return redoStack.size();
    }
}
