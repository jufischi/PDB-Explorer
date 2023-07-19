package pdbexplorer.model.undo;

import javafx.beans.property.Property;

/**
 * This class implements a PropertyCommand.
 * Implemented and provided by Daniel Huson 6.2023
 */
public class PropertyCommand<T> extends SimpleCommand {
    /**
     * Constructor for a PropertyCommand.
     * @param name (String): name of the command
     * @param v (Property<T>): property of command that changed
     * @param oldValue (T): the old value
     * @param newValue (T): the new value
     */
    public PropertyCommand(String name, Property<T> v, T oldValue, T newValue) {
        super(name, () -> v.setValue(oldValue), () -> v.setValue(newValue));
    }
}
