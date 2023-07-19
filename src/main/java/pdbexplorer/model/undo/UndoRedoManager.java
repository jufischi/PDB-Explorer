package pdbexplorer.model.undo;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * This class manages the undo and redo stacks.
 * Implemented and provided by Daniel Huson 6.2023
 */
public class UndoRedoManager {
    private final ObservableList<Command> undoStack = FXCollections.observableArrayList();
    private final ObservableList<Command> redoStack = FXCollections.observableArrayList();

    private final StringProperty undoLabel = new SimpleStringProperty("Undo");
    private final StringProperty redoLabel = new SimpleStringProperty("Redo");
    private final BooleanProperty canUndo = new SimpleBooleanProperty(false);
    private final BooleanProperty canRedo = new SimpleBooleanProperty(false);

    private final BooleanProperty inUndoRedo = new SimpleBooleanProperty(false);
    // this is used to prevent adding an undoable event when undoing or redoing an event
    // when undoing or redoing changes a property that is being observed so as to add to the undo stack

    /**
     * Constructor for the UndoRedoManager.
     */
    public UndoRedoManager() {
        undoStack.addListener((InvalidationListener) e ->
                undoLabel.set("Undo " + (undoStack.size() == 0 ? "" : undoStack.get(undoStack.size() - 1).name())));
        redoStack.addListener((InvalidationListener) e ->
                redoLabel.set("Redo " + (redoStack.size() == 0 ? "" : redoStack.get(redoStack.size() - 1).name())));
        canUndo.bind(Bindings.size(undoStack).isNotEqualTo(0));
        canRedo.bind(Bindings.size(redoStack).isNotEqualTo(0));
    }

    /**
     * Undoes a command and handles the undo and redo stacks.
     */
    public void undo() {
        inUndoRedo.set(true);
        try {
            if (isCanUndo()) {
                var command = undoStack.remove(undoStack.size() - 1);
                command.undo();
                if (command.canRedo())
                    redoStack.add(command);
            }
        } finally {
            inUndoRedo.set(false);
        }
    }

    /**
     * Redoes a command and handles the undo and redo stacks.
     */
    public void redo() {
        inUndoRedo.set(true);
        try {
            if (isCanRedo()) {
                var command = redoStack.remove(redoStack.size() - 1);
                command.redo();
                if (command.canUndo())
                    undoStack.add(command);
            }
        } finally {
            inUndoRedo.set(false);
        }
    }

    /**
     * Adds the given command to the undo stack.
     * @param command (Command): the command to add to the undo stack
     */
    public void add(Command command) {
        if (!isInUndoRedo()) {
            if (command.canUndo())
                undoStack.add(command);
            else
                undoStack.clear();
        }
    }

    /**
     * Adds the given command to the undo stack and directly performs the redo action.
     * @param command (Command): the command to add to the undo stack
     */
    public void addAndExecute(Command command) {
        if (!isInUndoRedo()) {
            add(command);
            command.redo();
        }
    }

    /**
     * Clears everything from the undo and redo stacks.
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    /**
     * Returns the specified label of the undo command.
     * @return (String): label of undoable command
     */
    public String getUndoLabel() {
        return undoLabel.get();
    }

    /**
     * Returns the specified label of the undo command as the property.
     * @return ReadOnlyStringProperty: label of undoable command
     */
    public ReadOnlyStringProperty undoLabelProperty() {
        return undoLabel;
    }

    /**
     * Returns the specified label of the redo command.
     * @return (String): label of redo-able command
     */
    public String getRedoLabel() {
        return redoLabel.get();
    }

    /**
     * Returns the specified label of the redo command as the property.
     * @return ReadOnlyStringProperty: label of redo-able command
     */
    public ReadOnlyStringProperty redoLabelProperty() {
        return redoLabel;
    }

    /**
     * Returns whether undo is possible, i.e., whether there are Commands in the undo stack.
     * @return boolean: whether undo is possible
     */
    public boolean isCanUndo() {
        return canUndo.get();
    }

    /**
     * Returns whether undo is possible, i.e., whether there are Commands in the undo stack, as a Boolean Property.
     * @return ReadOnlyBooleanProperty: whether undo is possible
     */
    public ReadOnlyBooleanProperty canUndoProperty() {
        return canUndo;
    }

    /**
     * Returns whether redo is possible, i.e., whether there are Commands in the redo stack.
     * @return boolean: whether redo is possible
     */
    public boolean isCanRedo() {
        return canRedo.get();
    }

    /**
     * Returns whether redo is possible, i.e., whether there are Commands in the redo stack, as a Boolean Property.
     * @return ReadOnlyBooleanProperty: whether redo is possible
     */
    public ReadOnlyBooleanProperty canRedoProperty() {
        return canRedo;
    }

    /**
     * To prevent adding an undoable event when undoing or redoing an event.
     * @return boolean: whether currently in undo/redo
     */
    public boolean isInUndoRedo() {
        return inUndoRedo.get();
    }

    /**
     * To prevent adding an undoable event when undoing or redoing an event. Returns the property.
     * @return ReadOnlyBooleanProperty: whether currently in undo/redo
     */
    public ReadOnlyBooleanProperty inUndoRedoProperty() {
        return inUndoRedo;
    }
}
