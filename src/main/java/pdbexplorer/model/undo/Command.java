package pdbexplorer.model.undo;

/**
 * This interface describes an undo- and redo-able command.
 * Implemented and provided by Daniel Huson 6.2023
 */
public interface Command {
    void undo();

    void redo();

    String name();

    boolean canUndo();

    boolean canRedo();
}
