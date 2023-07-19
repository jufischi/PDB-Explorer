package pdbexplorer.model.selection;

import javafx.collections.ObservableSet;

import java.util.Collection;

/**
 * This interface describes a selection model. Provided by Daniel Huson.
 * @param <T> (Object): interface can be used generically
 */
public interface SelectionModel<T> {
    boolean select(T t);

    boolean setSelected(T t, boolean select);

    boolean selectAll(Collection<T> list);

    void clearSelection();

    boolean clearSelection(T t);

    boolean clearSelection(Collection<T> list);

    boolean isSelected(T t);

    ObservableSet<T> getSelectedItems();
}
