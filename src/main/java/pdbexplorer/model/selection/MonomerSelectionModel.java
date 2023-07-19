package pdbexplorer.model.selection;

import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import pdbexplorer.model.protein.PDBMonomer;

import java.util.Collection;
import java.util.HashSet;

/**
 * This class implements a selection model for a PDBMonomer object.
 */
public class MonomerSelectionModel implements SelectionModel<PDBMonomer> {
    private final ObservableSet<PDBMonomer> selectedMonomers; // to store currently selected monomers

    /**
     * Constructor for a MonomerSelectionModelObject.
     */
    public MonomerSelectionModel() {
        selectedMonomers = FXCollections.observableSet(new HashSet<>());
    }

    /**
     * Adds the given monomer to the set of selected monomers, thus, selecting it.
     * @param monomer (PDBMonomer): the monomer that should be added to the selection
     * @return boolean: whether the selection changed as a result of the call
     */
    public boolean select(PDBMonomer monomer) {
        return selectedMonomers.add(monomer);
    }

    /**
     * Sets the selection status of the given monomer based on the second argument. If select is set to true, the
     * monomer is added to the selection, otherwise, it is removed from the selection.
     * @param monomer (PDBMonomer): the monomer that should be added to or removed from the selection
     * @param select (boolean): whether the monomer should be selected (true) or deselected (false)
     * @return boolean: whether the selection changed as a result of the call
     */
    public boolean setSelected(PDBMonomer monomer, boolean select) {
        if (select) {
            return selectedMonomers.add(monomer);
        } else {
            return selectedMonomers.remove(monomer);
        }
    }

    /**
     * Selects all monomers contained in the given list by adding them to the set of selected monomers.
     * @param list (Collection<PDBMonomer>): list of monomers to select
     * @return boolean: whether the selection changed as a result of the call
     */
    public boolean selectAll(Collection<PDBMonomer> list) {
        return selectedMonomers.addAll(list);
    }

    /**
     * Clears the whole selection by emptying the set of selected monomers.
     */
    public void clearSelection() {
        selectedMonomers.clear();
    }

    /**
     * Removes the given monomer from the set of selected monomers.
     * @param monomer (PDBMonomer): the monomer that should be removed from the selection
     * @return boolean: whether the selection changed as a result of the call
     */
    public boolean clearSelection(PDBMonomer monomer) {
        return selectedMonomers.remove(monomer);
    }

    /**
     * Removes all monomers in the given list from the set of selected monomers.
     * @param list (Collection<PDBMonomer>): list of monomers to deselect
     * @return boolean: whether the selection changed as a result of the call
     */
    public boolean clearSelection(Collection<PDBMonomer> list) {
        return selectedMonomers.removeAll(list);
    }

    /**
     * Returns whether a chosen PDBMonomer is contained in the set of selected monomers and thus selected or not.
     * @param monomer (PDBMonomer): a PDBMonomer object
     * @return boolean: whether the monomer is contained in the selected monomers
     */
    public boolean isSelected(PDBMonomer monomer) {
        return selectedMonomers.contains(monomer);
    }

    /**
     * Returns the set of PDBMonomers that are selected.
     * @return ObservableSet: the set of PDBMonomers in the list of selected monomers.
     */
    public ObservableSet<PDBMonomer> getSelectedItems() {
        return selectedMonomers;
    }
}
