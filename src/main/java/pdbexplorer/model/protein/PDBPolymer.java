package pdbexplorer.model.protein;

import javafx.util.Pair;

import java.util.ArrayList;

/**
 * This class defines a polymer, i.e., a chain, in a molecule.
 */
public class PDBPolymer {
    private final ArrayList<PDBMonomer> monomers;
    private final int number;
    private final String label;
    private final int modelNumber;

    /**
     * Constructor for polymer object.
     * @param monomers (ArrayList): list of monomers contained in polymer
     * @param number (int): consecutive number of polymer in complex
     * @param label (String): label of the polymer (chain)
     */
    public PDBPolymer(ArrayList<PDBMonomer> monomers, int number, String label, int modelNumber) {
        this.monomers = monomers;
        this.number = number;
        this.label = label;
        this.modelNumber = modelNumber;
    }

    /**
     * Returns all atoms contained in the polymer.
     * @return ArrayList: all contained atoms
     */
    public ArrayList<PDBAtom> getAtoms() {
        ArrayList<PDBAtom> allAtoms = new ArrayList<>();
        // Add all atoms of all monomers contained in the polymer
        for (PDBMonomer monomer : monomers) {
            allAtoms.addAll(monomer.getAtoms());
        }
        return allAtoms;
    }

    /**
     * Computes all bonds between atoms based on the following heuristic: distance between location of two atoms <= 2.
     * Returns a list of pairs of atoms that are bonded with each other.
     * @return ArrayList: list of pairs of atoms that are bonded with each other
     */
    public ArrayList<Pair<PDBAtom, PDBAtom>> getBonds() {
        ArrayList<PDBAtom> allAtoms = this.getAtoms(); // get all atoms of the current polymer
        ArrayList<Pair<PDBAtom, PDBAtom>> allBonds = new ArrayList<>();
        // compute all bonds according to heuristic: distance between atom locations <= 2
        for (int i = 0; i < allAtoms.size()-1; i++) {
            for (int j = i+1; j < allAtoms.size(); j++) {
                if (allAtoms.get(i).getCoordinates().distance(allAtoms.get(j).getCoordinates()) <= 2
                        && allAtoms.get(i).getModel() == allAtoms.get(j).getModel())
                    allBonds.add(new Pair<>(allAtoms.get(i), allAtoms.get(j)));
            }
        }
        return allBonds;
    }

    /**
     * Getter method for list of monomers.
     * @return ArrayList: list of monomers contained in polymer
     */
    public ArrayList<PDBMonomer> getMonomers() {
        return monomers;
    }

    /**
     * Getter method for number of polymer.
     * @return int: consecutive number of polymer in complex
     */
    public int getNumber() {
        return number;
    }

    /**
     * Getter method for polymer/chain label.
     * @return String: label of the polymer (chain)
     */
    public String getLabel() {
        return label;
    }

    /**
     * Getter method for model number.
     * @return int: number of model the polymer is contained in
     */
    public int getModelNumber() {
        return modelNumber;
    }
}
