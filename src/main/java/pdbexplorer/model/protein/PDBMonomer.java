package pdbexplorer.model.protein;

import java.util.ArrayList;

/**
 * This class defines a monomer, i.e., a residue, in a molecule.
 */
public class PDBMonomer {
    private final ArrayList<PDBAtom> atoms;
    private final String label;
    private final int id;
    private String secondaryStructureType;

    /**
     * Constructor in case secondary structure type is assigned to the Monomer.
     * @param atoms (ArrayList): atoms contained in the monomer
     * @param label (String): amino acid one-letter-code
     * @param id (int): ID of residue
     * @param secondaryStructureType (String): one of H (helix) and S (sheet)
     */
    public PDBMonomer(ArrayList<PDBAtom> atoms, String label, int id, String secondaryStructureType) {
        this.atoms = atoms;
        this.label = label;
        this.id = id;
        this.secondaryStructureType = secondaryStructureType;
    }

    /**
     * Constructor if only residue label and contained atoms are known.
     * @param atoms (ArrayList): atoms contained in the monomer
     * @param label (String): amino acid one-letter-code
     * @param id (int): ID of residue
     */
    public PDBMonomer(ArrayList<PDBAtom> atoms, String label, int id) {
        this.atoms = atoms;
        this.label = label;
        this.id = id;
    }

    /**
     * Getter method for list of atoms.
     * @return ArrayList: atoms contained in the monomer
     */
    public ArrayList<PDBAtom> getAtoms() {
        return atoms;
    }

    /**
     * Getter method for residue label.
     * @return String: amino acid one-letter-code
     */
    public String getLabel() {
        return label;
    }

    /**
     * Getter method for residue ID.
     * @return int: ID of residue
     */
    public int getId() {
        return id;
    }

    /**
     * Getter method for secondary structure type.
     * @return String: one of H (helix) and S (sheet) or null if not secondary structure type present
     */
    public String getSecondaryStructureType() {
        return secondaryStructureType;
    }

    /**
     * Returns the c-alpha atom of the monomer. Returns null if no c-alpha is present.
     * @return PDBAtom: the c-alpha atom
     */
    public PDBAtom getCAlpha() {
        PDBAtom cAlpha = null;

        for (PDBAtom atom : this.atoms) {
            if (atom.getRole().equals("CA")) {
                cAlpha = atom;
                break; // assume first atom declared as CA is right one in case there has been a mis-classification
            }
        }

        return cAlpha;
    }

    /**
     * Returns the c-beta atom of the monomer. Returns null if no c-beta is present.
     * @return PDBAtom: the c-beta atom
     */
    public PDBAtom getCBeta() {
        PDBAtom cBeta = null;

        for (PDBAtom atom : this.atoms) {
            if (atom.getRole().equals("CB")) {
                cBeta = atom;
                break; // assume first atom declared as CB is right one in case there has been a mis-classification
            }
        }

        return cBeta;
    }

    /**
     * Returns the C atom of the monomer, which is contained in the backbone in addition to c-alpha.
     * Returns null if not present.
     * @return PDBAtom: the backbone C atom
     */
    public PDBAtom getC() {
        PDBAtom c = null;

        for (PDBAtom atom : this.atoms) {
            if (atom.getRole().equals("C")) {
                c = atom;
                break; // assume first atom declared as C is right one in case there has been a mis-classification
            }
        }

        return c;
    }

    /**
     * Returns the backbone N atom of the monomer. Returns null if not present.
     * @return PDBAtom: the backbone N atom
     */
    public PDBAtom getN() {
        PDBAtom n = null;

        for (PDBAtom atom : this.atoms) {
            if (atom.getRole().equals("N")) {
                n = atom;
                break; // assume first atom declared as N is right one in case there has been a mis-classification
            }
        }

        return n;
    }
}
