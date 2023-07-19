package pdbexplorer.model.protein;

import java.util.ArrayList;

/**
 * This class defines a molecule. It contains all chains as well as all models that are available from the PDB file.
 */
public class PDBComplex {
    private final ArrayList<PDBPolymer> polymers;
    private final int numberOfModels;
    private final ArrayList<String> chains;
    private final boolean protein;

    /**
     * Constructor for complex object.
     * @param polymers (ArrayList): list of polymers contained in complex
     * @param numberOfModels (int): number of models contained
     * @param chains (ArrayList): list of labels of chains contained
     * @param protein (boolean): whether the complex contains protein
     */
    public PDBComplex(ArrayList<PDBPolymer> polymers, int numberOfModels, ArrayList<String> chains,
                      boolean protein) {
        this.polymers = polymers;
        this.numberOfModels = numberOfModels;
        this.chains = chains;
        this.protein = protein;
    }

    /**
     * Empty constructor for complex object.
     */
    public PDBComplex() {
        this.polymers = new ArrayList<>();
        this.numberOfModels = 0;
        this.chains = new ArrayList<>();
        this.protein = false;
    }

    /**
     * Getter method for list of polymers.
     * @return ArrayList: list of polymers contained in the complex
     */
    public ArrayList<PDBPolymer> getPolymers() {
        return polymers;
    }

    /**
     * Getter method for number of models.
     * @return int: number of models contained in the complex
     */
    public int getNumberOfModels() {
        return numberOfModels;
    }

    /**
     * Getter method for list of chains.
     * @return ArrayList: list of labels of chains contained in the complex
     */
    public ArrayList<String> getChains() {
        return chains;
    }

    /**
     * Getter method for whether complex contains protein.
     * @return boolean: whether complex contains protein
     */
    public boolean isProtein() {
        return protein;
    }
}
