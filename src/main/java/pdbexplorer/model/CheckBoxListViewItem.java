package pdbexplorer.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * This class defines a ListViewItem, which has a CheckBox. The code for this class was adjusted from
 * https://stackoverflow.com/questions/28843858/javafx-8-listview-with-checkboxes.
 */
public class CheckBoxListViewItem {
    private final StringProperty name = new SimpleStringProperty();
    private final BooleanProperty on = new SimpleBooleanProperty();

    /**
     * Constructor for the CheckBoxListViewItem.
     * @param name (String): name of the list item
     * @param on (boolean): whether list item should be selected
     */
    public CheckBoxListViewItem(String name, boolean on) {
        setName(name);
        setOn(on);
    }

    /**
     * Returns the name property of the item.
     * @return StringProperty: the name property
     */
    public final StringProperty nameProperty() {
        return this.name;
    }

    /**
     * Returns the name of the item as a String.
     * @return String: the name of the list item
     */
    public final String getName() {
        return this.nameProperty().get();
    }

    /**
     * Sets the name of the list item.
     * @param name (String) name of the list item
     */
    private void setName(final String name) {
        this.nameProperty().set(name);
    }

    /**
     * Returns a BooleanProperty specifying whether the Checkbox is selected or not.
     * @return BooleanProperty: whether Checkbox is selected
     */
    public final BooleanProperty onProperty() {
        return this.on;
    }

    /**
     * Returns whether the Checkbox is selected or not.
     * @return boolean: whether Checkbox is selected
     */
    public final boolean isOn() {
        return this.onProperty().get();
    }

    /**
     * Sets the onProperty for the list item.
     * @param on (boolean): whether Checkbox is selected
     */
    private void setOn(final boolean on) {
        this.onProperty().set(on);
    }

    /**
     * Returns the name of the list item.
     * @return String: name of the list item
     */
    @Override
    public String toString() {
        return getName();
    }
}
