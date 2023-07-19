package pdbexplorer.model.io;

import javafx.concurrent.Task;
import javafx.geometry.Point3D;
import pdbexplorer.model.protein.PDBAtom;
import pdbexplorer.model.protein.PDBComplex;
import pdbexplorer.model.protein.PDBMonomer;
import pdbexplorer.model.protein.PDBPolymer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * This class parses a PDB file in String format and extracts the information about the amino acid polymer(s)
 * contained within.
 */
public class PDBParser {
    /**
     * This task offers the main function of the PDB parser. This parses the given PDB file and extracts information
     * about the atoms that belong to amino acid residues as well as information about their secondary structure.
     */
    public static class ParsePDB extends Task<PDBComplex> {
        private final String pdbContent;

        /**
         * Constructor of the ParsePDB task.
         * @param pdbContent (String): PDB file provided in String format
         */
        public ParsePDB(String pdbContent) {
            this.pdbContent = pdbContent;
        }

        /**
         * Main function of the PDB parser. This parses the given PDB file and extracts information about the atoms that
         * belong to amino acid residues as well as information about their secondary structure.
         * @return PDBComplex: protein complex object
         */
        @Override
        public PDBComplex call() {
            // unknown amino acids are added as "X"
            List<String> oneLetterCode = Arrays.asList("A", "C", "D", "E", "F", "H", "I", "K", "L", "M", "N",
                    "P", "Q", "R", "S", "T", "V", "W", "Y", "G", "O", "U", "X");
            List<String> threeLetterCode = Arrays.asList("ALA", "CYS", "ASP", "GLU", "PHE", "HIS", "ILE",
                    "LYS", "LEU", "MET", "ASN", "PRO", "GLN", "ARG", "SER", "THR", "VAL", "TRP", "TYR", "GLY",
                    "PYL", "SEC", "UNK");
            List<String> threeLetterDAA = Arrays.asList("DAL", "DCY", "DAS", "DGL", "DPN", "DHI", "DIL",
                    "DLY", "DLE", "MED", "DSG", "DPR", "DGN", "DAR", "DSN", "DTH", "DVA", "DTR", "DTY");

            HashMap<String, String> threeToOneLetterCode = new HashMap<>();
            for (int i = 0; i < oneLetterCode.size(); i++) {
                threeToOneLetterCode.put(threeLetterCode.get(i), oneLetterCode.get(i));
                if (i < oneLetterCode.size() - 4)
                    threeToOneLetterCode.put(threeLetterDAA.get(i), oneLetterCode.get(i));
            }

            // Define needed variables
            String previousChainID = "";
            int previousResidueID = Integer.MAX_VALUE;
            String previousResName = "";
            ArrayList<PDBAtom> atoms = new ArrayList<>();
            ArrayList<PDBMonomer> monomers = new ArrayList<>();
            ArrayList<PDBPolymer> complex = new ArrayList<>();
            int polymerCount = 1;
            HashMap<String, ArrayList<Integer>> helices = new HashMap<>();
            HashMap<String, ArrayList<Integer>> sheets = new HashMap<>();
            int model = 0;
            int previousModel = 0;
            TreeSet<String> chains = new TreeSet<>();
            boolean containsProtein = false;

            // Go over PDB file line by line
            for (String line : pdbContent.split("\\R")) {

                // Get atom entries
                if (line.startsWith("ATOM")) {
                    // Get residue name
                    String resName = line.substring(17, 20).strip(); // get residue name
                    // Check for amino acids
                    if (resName.length() == 3) {  // only interested in proteins -> amino acids 3-letter, others 1 or 2
                        // Set boolean to true
                        containsProtein = true;

                        // Get atom and only continue parsing the line if atom is not H
                        String atomSymbol = line.substring(76, 78).strip();
                        if (atomSymbol.equals("H")) // ignore H-atoms
                            continue;

                        // get chain ID
                        String chainID = line.substring(21, 22);
                        chains.add(chainID);
                        // Set previous chain ID and model number in case of start of new chain
                        if (monomers.isEmpty()) {
                            previousChainID = chainID;
                            previousModel = model;
                        }

                        // Get residue ID
                        int residueID = Integer.parseInt(line.substring(22, 26).strip());

                        // Set previous residue ID at beginning
                        if (previousResidueID == Integer.MAX_VALUE)
                            previousResidueID = residueID;

                        // Set previous residue name for first time
                        if (previousResName.equals(""))
                            previousResName = resName;

                        if (!threeToOneLetterCode.containsKey(previousResName))
                            previousResName = "UNK"; // for non-typical or unknown residues -> set to X

                        if (previousResidueID != residueID) {
                            // HELIX and SHEET information should be before ATOM info -> add secondary structure info here
                            if (helices.containsKey(previousChainID) &&
                                    helices.get(previousChainID).contains(previousResidueID))
                                monomers.add(new PDBMonomer(atoms, threeToOneLetterCode.get(previousResName),
                                        previousResidueID, "H"));
                            else if (sheets.containsKey(previousChainID)
                                    && sheets.get(previousChainID).contains(previousResidueID))
                                monomers.add(new PDBMonomer(atoms, threeToOneLetterCode.get(previousResName),
                                        previousResidueID, "S"));
                            else
                                monomers.add(new PDBMonomer(atoms, threeToOneLetterCode.get(previousResName),
                                        previousResidueID));

                            // Reset atoms list as well as info of previous residue
                            atoms = new ArrayList<>();
                            previousResidueID = residueID;
                            previousResName = resName;
                        }

                        // Add Polymer to list if new model is started
                        if (model != previousModel) {
                            complex.add(new PDBPolymer(monomers, polymerCount, previousChainID, previousModel));
                            monomers = new ArrayList<>();
                            polymerCount = 1;
                        } else if (!chainID.equals(previousChainID)) { // Add Polymer to list if new chain is started
                            complex.add(new PDBPolymer(monomers, polymerCount, previousChainID, previousModel));
                            monomers = new ArrayList<>();
                            polymerCount += 1;
                        }

                        // Add new atom to atoms list; alternative conformers are not taken into account
                        if (line.charAt(16) == 'A' || line.charAt(16) == ' ') {
                            String atomFullName = line.substring(12, 16).strip();
                            int atomID = Integer.parseInt(line.substring(6, 11).strip());
                            double atomX = Double.parseDouble(line.substring(30, 38).strip());
                            double atomY = Double.parseDouble(line.substring(38, 46).strip());
                            double atomZ = Double.parseDouble(line.substring(46, 54).strip());
                            Point3D atomCoords = new Point3D(atomX, atomY, atomZ);
                            atoms.add(new PDBAtom(atomSymbol, atomFullName, atomID, atomCoords, model, chainID));
                        }
                    }
                } else if (line.startsWith("HELIX")) {
                    String chain = line.substring(19, 20);
                    int startResidue = Integer.parseInt(line.substring(21, 25).strip());
                    int stopResidue = Integer.parseInt(line.substring(33, 37).strip());

                    // add chain as key in hashmap if absent
                    if (!helices.containsKey(chain))
                        helices.put(chain, new ArrayList<>());

                    // add all numbers of residues contained in secondary structure
                    for (int i = startResidue; i <= stopResidue; i++) {
                        helices.get(chain).add(i);
                    }
                } else if (line.startsWith("SHEET")) {
                    String chain = line.substring(21, 22);
                    int startResidue = Integer.parseInt(line.substring(22, 26).strip());
                    int stopResidue = Integer.parseInt(line.substring(33, 37).strip());

                    // add chain as key in hashmap if absent
                    if (!sheets.containsKey(chain))
                        sheets.put(chain, new ArrayList<>());

                    // add all numbers of residues contained in secondary structure
                    for (int i = startResidue; i <= stopResidue; i++) {
                        sheets.get(chain).add(i);
                    }
                } else if (line.startsWith("MODEL")) {
                    model += 1;
                }
            }
            // add last monomer to list of monomers and last chain of monomers to complex as polymer
            monomers.add(new PDBMonomer(atoms, threeToOneLetterCode.get(previousResName), previousResidueID));
            complex.add(new PDBPolymer(monomers, polymerCount, previousChainID, previousModel));

            return new PDBComplex(complex, model, new ArrayList<>(chains), containsProtein);
        }
    }

    /**
     * This task reads in a PDB file and generates and returns a String from it.
     */
    public static class ReadPDB extends Task<String> {
        private final String file;

        /**
         * Constructor of the ReadPDB task.
         * @param file (String): Path to the PDB file
         */
        public ReadPDB(String file) {
            this.file = file;
        }

        /**
         * Reads in a PDB file and generates and returns a String from it.
         * @return String: The PDB file in String format
         */
        @Override
        public String call() throws IOException {
            try(BufferedReader r = new BufferedReader(new FileReader(file))) {
                StringBuilder lines = new StringBuilder();

                while (r.ready()) {
                    lines.append(r.readLine()).append("\n");
                }

                return lines.toString();
            }
        }
    }
}
