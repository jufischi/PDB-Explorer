package pdbexplorer.model.undo;

/**
 * This class implements a simple command that is undo- and redo-able.
 * Implemented and provided by Daniel Huson 6.2023
 */
public class SimpleCommand implements Command {
    private final String name;
    private final Runnable runUndo;
    private final Runnable runRedo;

    /**
     * Constructor for the SimpleCommand.
     * @param name (String): name of the command
     * @param runUndo (Runnable): what should be done for undo
     * @param runRedo (Runnable): what should be done for redo
     */
    public SimpleCommand(String name, Runnable runUndo, Runnable runRedo) {
        this.name = name;
        this.runUndo = runUndo;
        this.runRedo = runRedo;
    }

    /**
     * Runs the action set on undo.
     */
    @Override
    public void undo() {
        runUndo.run();
    }

    /**
     * Runs the action set on redo.
     */
    @Override
    public void redo() {
        runRedo.run();
    }

    /**
     * Returns the name of the command.
     * @return String: name of the command
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * Checks whether the command can be undone.
     */
    @Override
    public boolean canUndo() {
        return runUndo != null;
    }

    /**
     * Checks whether the command can be re-done.
     */
    @Override
    public boolean canRedo() {
        return runRedo != null;
    }
}
