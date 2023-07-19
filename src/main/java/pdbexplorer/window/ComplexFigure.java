package pdbexplorer.window;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Pair;
import pdbexplorer.model.protein.PDBAtom;
import pdbexplorer.model.protein.PDBComplex;
import pdbexplorer.model.protein.PDBMonomer;
import pdbexplorer.model.protein.PDBPolymer;

import java.util.*;

/**
 * This class contains methods and tasks needed for the display of the figure corresponding to the molecule contained
 * in a PDB file. The task ComputeFigure computes the balls- and sticks-representation of the molecule, the task
 * ComputeRibbon the ribbon-representation. The method setAtomColor() is used to change the color scheme of the atoms.
 * The method computeSequence() generates Text(Flow) objects for the sequence taking into account all models.
 */
public class ComplexFigure {
    private static final double DEFAULT_RADIUS = 0.1;
    private static final int DEFAULT_FONT_SIZE = 18;

    /**
     * This Task gets as input a model and computes all sticks and balls for this. These are then added to the
     * respective groups. The calculated 3D objects will be centered to the origin according to the Balls.
     */
    public static class ComputeFigure extends Task<HashMap<PDBAtom, Sphere>> {
        private final PDBComplex model;
        private final Group balls;
        private final Group sticks;
        private final int numberOfModels;
        private final ArrayList<String> chains;

        /**
         * Constructor for the Task ComputeFigure object.
         * @param model (PDBComplex): Molecule for which its 3D Ball and stick representation will be calculated.
         * @param balls (Group): Grouping to which the balls for display are added.
         * @param sticks (Group): Grouping to which the sticks for display are added.
         */
        public ComputeFigure(PDBComplex model, Group balls, Group sticks) {
            this.model = model;
            this.balls = balls;
            this.sticks = sticks;
            this.numberOfModels = model.getNumberOfModels();
            this.chains = model.getChains();
        }

        @Override
        public HashMap<PDBAtom, Sphere> call() {
            // Create hashmap from atom to sphere
            HashMap<PDBAtom, Sphere> atomToSphere = new HashMap<>();

            // Create as many "sub"groups in balls and sticks as numberOfModels (in case there is more than one)
            Platform.runLater(() -> {
                createSubGroups(balls, numberOfModels, chains);
                createSubGroups(sticks, numberOfModels, chains);
            });

            // Get all atoms and bonds contained in the complex (i.e. in all contained polymers)
            ArrayList<PDBAtom> atoms = new ArrayList<>();
            ArrayList<Pair<PDBAtom, PDBAtom>> bonds = new ArrayList<>();
            for (PDBPolymer polymer : model.getPolymers()) { // get all atoms
                atoms.addAll(polymer.getAtoms());
                bonds.addAll(polymer.getBonds());
            }

            int total = atoms.size() + bonds.size();

            // Compute the mean coordinate that is needed for centering
            ArrayList<Point3D> meanPoints = computeMeanPoint(model, numberOfModels);

            // Add all balls
            for (int i = 0; i < atoms.size(); i++) {
                PDBAtom atom = atoms.get(i);

                // generate the respective ball
                Sphere ball = new Sphere((float) atom.getRadius(), 18);

                // compute centered coordinates
                Point3D meanCoordinate = atom.getCoordinates().subtract(meanPoints
                        .get(atom.getModel() == 0 ? 0 : atom.getModel() - 1));

                // set the centered coordinates
                ball.setTranslateX(meanCoordinate.getX());
                ball.setTranslateY(meanCoordinate.getY());
                ball.setTranslateZ(meanCoordinate.getZ());

                // setup and add the material/color
                ball.setMaterial(new PhongMaterial(atom.getColor()));

                // add ball (i.e., atom) to the respective group
                Platform.runLater(() -> putIntoSubGroups(atom, ball, balls, chains));
                // add atom and ball to hashmap
                atomToSphere.put(atom, ball);

                int finalI = i;
                Platform.runLater(() -> updateProgress(finalI, total));
            }

            // Add all sticks (i.e., bonds)
            for (int i = 0; i < bonds.size(); i++) {
                Pair<PDBAtom, PDBAtom> bond = bonds.get(i);

                PDBAtom atom1 = bond.getKey();
                PDBAtom atom2 = bond.getValue();

                int currentModel = atom1.getModel() == 0 ? 0 : atom1.getModel() - 1;

                Cylinder stick = createStickBetweenPoints(atom1.getCoordinates().subtract(meanPoints.get(currentModel)),
                        atom2.getCoordinates().subtract(meanPoints.get(currentModel)));

                // add stick (i.e., bond) to the respective group
                Platform.runLater(() -> putIntoSubGroups(atom1, stick, sticks, chains));

                int finalI = i + atoms.size();
                Platform.runLater(() -> updateProgress(finalI, total));
            }

            return atomToSphere;
        }
    }

    /**
     * This function creates a stick (Cylinder) between the two given points.
     *
     * @param a first 3D point
     * @param b second 3D point
     * @return Cylinder that spans between the two given points
     */
    private static Cylinder createStickBetweenPoints(Point3D a, Point3D b) {
        Point3D YAXIS = new Point3D(0, 100, 0);
        Point3D midpoint = a.midpoint(b);
        Point3D direction = b.subtract(a);
        Point3D perpendicularAxis = YAXIS.crossProduct(direction);
        double angle = YAXIS.angle(direction);

        Cylinder cylinder = new Cylinder(DEFAULT_RADIUS, 100, 6);
        cylinder.setRotationAxis(perpendicularAxis);
        cylinder.setRotate(angle);
        cylinder.setTranslateX(midpoint.getX());
        cylinder.setTranslateY(midpoint.getY());
        cylinder.setTranslateZ(midpoint.getZ());
        cylinder.setScaleY(a.distance(b) / cylinder.getHeight());
        cylinder.setMaterial(new PhongMaterial(Color.ORANGE));
        return cylinder;
    }

    /**
     * Changes the color of the atoms according to the selected color scheme.
     * @param model (PDBComplex): Molecule for which its 3D Ball and stick representation will be calculated.
     * @param atomToSphere (HashMap): Maps atoms to their corresponding spheres.
     * @param colorScheme (int): The new color scheme, which should be employed.
     */
    public static void setAtomColor(PDBComplex model, HashMap<PDBAtom, Sphere> atomToSphere, int colorScheme) {
        // Residue coloring according to CINEMA scheme taken from https://www.bioinformatics.nl/~berndb/aacolour.html
        HashMap<String, Color> colorByResidue = getColorByResidue();

        // color chains by rainbow colors: red, orange, yellow, green, cyan, blue, violet
        // repeat for more than 7 chains
        final List<Color> colorByChain = Arrays.asList(Color.VIOLET, Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN,
                Color.CYAN, Color.BLUE);

        for (PDBPolymer polymer : model.getPolymers()) {
            int chainNumber = polymer.getNumber() % colorByChain.size();
            for (PDBMonomer monomer : polymer.getMonomers()) {
                for (PDBAtom atom : monomer.getAtoms()) {
                    double opacity = ((PhongMaterial) atomToSphere.get(atom).getMaterial())
                            .getDiffuseColor().getOpacity();
                    if (colorScheme == 0) { // color by atom
                        Color color = atom.getColor();
                        atomToSphere.get(atom).setMaterial(new PhongMaterial(
                                new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity)));
                    } else if (colorScheme == 1) { // color by residue
                        Color color = colorByResidue.get(monomer.getLabel());
                        atomToSphere.get(atom).setMaterial(new PhongMaterial(
                                new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity)));
                    } else if (colorScheme == 2) { // color by secondary structure
                        Color color;
                        if (monomer.getSecondaryStructureType() == null)
                            color = Color.GRAY;
                        else if (monomer.getSecondaryStructureType().equals("H"))
                            color = Color.RED;
                        else
                            color = Color.LIGHTGREEN;
                        atomToSphere.get(atom).setMaterial(new PhongMaterial(
                                new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity)));
                    } else if (colorScheme == 3) { // color by molecule
                        Color color = colorByChain.get(chainNumber);
                        atomToSphere.get(atom).setMaterial(new PhongMaterial(
                                new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity)));
                    }
                }
            }
        }
    }

    /**
     * This Task gets as input a model and computes the ribbon for this in form of meshViews. These are then added to
     * the respective groups. The calculated 3D object will be centered to the origin.
     */
    public static class ComputeRibbon extends Task<Void> {
        private final PDBComplex model;
        private final Group ribbons;
        private final int numberOfModels;
        private final ArrayList<String> chains;

        /**
         * Constructor for the Task ComputeRibbon object.
         * @param model (PDBComplex): Molecule for which its 3D Ball and stick representation will be calculated.
         * @param ribbons (Group): Grouping to which the meshViews for display are added.
         */
        public ComputeRibbon(PDBComplex model, Group ribbons) {
            this.model = model;
            this.ribbons = ribbons;
            this.numberOfModels = model.getNumberOfModels();
            this.chains = model.getChains();
        }

        @Override
        public Void call() {
            // Create as many "sub"groups in ribbons as numberOfModels (in case there is more than one)
            Platform.runLater(() -> createSubGroups(ribbons, numberOfModels, chains));

            // Compute the mean coordinate that is needed for centering
            ArrayList<Point3D> meanPoints = computeMeanPoint(model, numberOfModels);

            // Compute the mean distance of c-betas to c-alphas
            Point3D meanDistance = meanDistanceOfCBeta(model);

            // Compute ribbons
            ArrayList<Point3D> prevMonomerCoords;
            ArrayList<Point3D> currentMonomerCoords;

            // Compute total
            int total = model.getPolymers().size();

            for (int i = 0; i < model.getPolymers().size(); i++) {
                PDBPolymer polymer = model.getPolymers().get(i);

                prevMonomerCoords = null; // Make coordinates null for the start of each polymer

                // Get mean point corresponding to current model
                Point3D meanPoint = meanPoints.get(polymer.getModelNumber() == 0 ? 0 : polymer.getModelNumber() - 1);

                for (PDBMonomer monomer : polymer.getMonomers()) {
                    if (prevMonomerCoords == null) { // Get coordinates for the first monomer in the polymer
                        prevMonomerCoords = getMoleculeCoordinates(monomer, meanPoint, meanDistance);
                    } else {
                        currentMonomerCoords = getMoleculeCoordinates(monomer, meanPoint, meanDistance);

                        // Define points for the Mesh
                        float[] points = {
                                (float) prevMonomerCoords.get(0).getX(), (float) prevMonomerCoords.get(0).getY(), (float) prevMonomerCoords.get(0).getZ(),
                                (float) prevMonomerCoords.get(1).getX(), (float) prevMonomerCoords.get(1).getY(), (float) prevMonomerCoords.get(1).getZ(),
                                (float) prevMonomerCoords.get(2).getX(), (float) prevMonomerCoords.get(2).getY(), (float) prevMonomerCoords.get(2).getZ(),
                                (float) currentMonomerCoords.get(0).getX(), (float) currentMonomerCoords.get(0).getY(), (float) currentMonomerCoords.get(0).getZ(),
                                (float) currentMonomerCoords.get(1).getX(), (float) currentMonomerCoords.get(1).getY(), (float) currentMonomerCoords.get(1).getZ(),
                                (float) currentMonomerCoords.get(2).getX(), (float) currentMonomerCoords.get(2).getY(), (float) currentMonomerCoords.get(2).getZ()
                        };

                        // Define texture mapping coordinates
                        float[] texCoords = {
                                0, 0,       // t0
                                0, 0.5f,    // t1
                                0, 1,       // . . .
                                1, 1,
                                1, 0.5f,
                                1, 0        // t5
                        };

                        // Define faces for the Mesh
                        int[] faces = {
                                0, 0, 1, 1, 4, 4,
                                0, 0, 4, 4, 5, 5,
                                1, 1, 2, 2, 3, 3,
                                1, 1, 3, 3, 4, 4,

                                0, 0, 4, 4, 1, 1, // same triangles, facing the other way
                                0, 0, 5, 5, 4, 4,
                                1, 1, 3, 3, 2, 2,
                                1, 1, 4, 4, 3, 3,
                        };

                        // Define smoothing groups for the mesh
                        int[] smoothing = {1, 1, 1, 1, 2, 2, 2, 2};

                        // Define TriangleMesh
                        TriangleMesh mesh = new TriangleMesh();
                        mesh.getPoints().addAll(points);
                        mesh.getTexCoords().addAll(texCoords);
                        mesh.getFaces().addAll(faces);
                        mesh.getFaceSmoothingGroups().addAll(smoothing);

                        // Create MeshView
                        MeshView meshView = new MeshView(mesh);
                        meshView.setMaterial(new PhongMaterial(Color.YELLOW));
                        meshView.setDrawMode(DrawMode.FILL);

                        // add mesh (i.e., ribbon) to the respective group
                        PDBAtom atom = monomer.getAtoms().get(0);
                        Platform.runLater(() -> putIntoSubGroups(atom, meshView, ribbons, chains));

                        // Set this monomer as the new previous monomer
                        prevMonomerCoords = currentMonomerCoords;

                        // Update progress for the progress bar
                        int finalI = i;
                        Platform.runLater(() -> updateProgress(finalI, total));
                    }
                }
            }
            return null;
        }
    }

    /**
     * Computes the molecule coordinates needed for the computation of ribbons.
     * @param monomer (PDBMonomer): one monomer object
     * @param meanPoint (Point3D): the mean point of the whole molecule
     * @param meanDist (Point3D): the mean distance of the c-beta atoms to their respective c-alphas
     * @return ArrayList: a list of the three needed coordinates - of C_alpha, C_beta and opposite
     */
    private static ArrayList<Point3D> getMoleculeCoordinates(PDBMonomer monomer, Point3D meanPoint, Point3D meanDist) {
        Point3D cAlpha = null;
        Point3D cBeta = null;
        Point3D opposite;

        // Get coordinates for C_alpha and C_beta
        for (PDBAtom atom : monomer.getAtoms()) {
            switch (atom.getRole()) { // in case of cAlpha check that it has not been assigned already (possible in erroneous PDB file)
                case "CA" -> cAlpha = cAlpha == null ? atom.getCoordinates().subtract(meanPoint) : cAlpha;
                case "CB" -> cBeta = atom.getCoordinates().subtract(meanPoint);
            }
        }

        // Compute C_beta in case of Glycine and compute the "opposite" coordinate
        assert cAlpha != null;
        if (cBeta == null) { // in case of Glycine or wrong notation in PDB file
            cBeta = cAlpha.subtract(meanDist);
            opposite = cAlpha.add(meanDist);
        } else {
            // Compute opposite point from cBeta through cAlpha
            opposite = cAlpha.subtract(cBeta.subtract(cAlpha));
        }

        return new ArrayList<>(List.of(opposite, cAlpha, cBeta));
    }

    /**
     * Computes the mean distance of c-betas to their respective c-alphas for the given model.
     * @param model (PDBComplex): Molecule for which its 3D Ball and stick representation will be calculated.
     * @return Point3D: Mean distance of all c-betas to their respective c-alpha of the model
     */
    private static Point3D meanDistanceOfCBeta(PDBComplex model) {
        // Compute the mean distance of c-betas to c-alphas in the model
        Point3D meanDistance = new Point3D(0, 0, 0);
        int numberOfCBeta = 0;
        for (PDBPolymer polymer : model.getPolymers()) {
            for (PDBMonomer monomer : polymer.getMonomers()) {
                PDBAtom cAlpha = monomer.getCAlpha();
                PDBAtom cBeta = monomer.getCBeta();
                if (cBeta != null && cAlpha != null) {
                    double newX = cBeta.getCoordinates().getX() - cAlpha.getCoordinates().getX();
                    double newY = cBeta.getCoordinates().getY() - cAlpha.getCoordinates().getY();
                    double newZ = cBeta.getCoordinates().getZ() - cAlpha.getCoordinates().getZ();

                    // add absolute distance because otherwise mean distance would be close to zero
                    meanDistance = meanDistance.add(Math.abs(newX), Math.abs(newY), Math.abs(newZ));
                    numberOfCBeta += 1;
                }
            }
        }
        // Divide by total number of cBeta atoms present in molecule; if else to avoid division by zero
        meanDistance = numberOfCBeta != 0 ? meanDistance.multiply((1.0 / numberOfCBeta)) : new Point3D(1, 0, 0);
        return meanDistance;
    }

    /**
     * Computes the mean point of a molecule per model that is contained. This is needed for subsequent centering of
     * the molecule in the pane.
     * @param model (PDBComplex): Molecule for which its 3D Ball and stick representation will be calculated.
     * @return Point3D: Mean point of the coordinates of the model
     */
    private static ArrayList<Point3D> computeMeanPoint(PDBComplex model, int numberOfModels) {
        ArrayList<Point3D> meanPoints = new ArrayList<>();
        ArrayList<Integer> numberofAtoms = new ArrayList<>();
        for (int i = 0; i < (numberOfModels == 0 ? 1 : numberOfModels); i++) {
            meanPoints.add(new Point3D(0, 0, 0));
            numberofAtoms.add(0);
        }

        // Compute the mean coordinate that is needed for centering
        for (PDBPolymer polymer : model.getPolymers()) {
            int modelNumber = polymer.getModelNumber() == 0 ? 0 : polymer.getModelNumber() - 1;

            for (PDBAtom atom : polymer.getAtoms()) { // add up the coordinates of all atoms contained
                meanPoints.set(modelNumber, meanPoints.get(modelNumber).add(atom.getCoordinates()));
                numberofAtoms.set(modelNumber, numberofAtoms.get(modelNumber) + 1);
            }
        }

        for (int i = 0; i < meanPoints.size(); i++) { // divide by the number of atoms
            meanPoints.set(i, meanPoints.get(i).multiply(1.0 / numberofAtoms.get(i)));
        }

        return meanPoints;
    }

    /**
     * Creates the needed subgroups for the number of models and chains contained in the PDB-file.
     * @param group (Group): a group contained in the figure (balls, sticks or ribbons)
     * @param numberOfModels (int): the number of models contained in PDB file
     * @param chains (ArrayList): the chains contained in the molecule
     */
    private static void createSubGroups(Group group, int numberOfModels, ArrayList<String> chains) {
        // Create as many "sub"groups in the group as models
        for (int i = 0; i < (numberOfModels == 0 ? 1 : numberOfModels); i++) {
            group.getChildren().add(new Group()); // one new group per model

            // Create as many "sub"groups as there are chains
            for (int j = 0; j < chains.size(); j++) {
                ((Group) group.getChildren().get(i)).getChildren().add(new Group()); // one group per chain
            }
        }
    }

    /**
     * Puts the given atom into the appropriate subgroup in the figure according to its model and chain.
     * @param atom (PDBAtom): an atom object
     * @param node (Node): the node to put into the figure (a Sphere, Cylinder or MeshView)
     * @param group (Group): a group in the Figure
     * @param chains (ArrayList): the chains present in the molecule
     */
    private static void putIntoSubGroups(PDBAtom atom, Node node, Group group, ArrayList<String> chains) {
        ((Group) ((Group) group.getChildren().get(atom.getModel() == 0 ? 0 : atom.getModel() - 1))
                .getChildren().get(chains.indexOf(atom.getChain()))).getChildren().add(node);
    }

    /**
     * Computes TextFlow elements for all models contained in the PDB file.
     * @param model (PDBComplex): Molecule for which its sequence display will be calculated
     * @param sequences (ArrayList): to store all sequence TextFlow objects
     * @param tf (TextFlow): the TextFlow in which the sequence will be displayed in the GUI
     * @param sp (ScrollPane): the ScrollPane in which the TextFlow is contained
     * @return HashMap: to map monomers to their corresponding Text object
     */
    public static HashMap<PDBMonomer, Text> computeSequence(PDBComplex model, ArrayList<TextFlow> sequences,
                                                            TextFlow tf, ScrollPane sp) {
        HashMap<PDBMonomer, Text> monomerToText = new HashMap<>(); // to map monomers to the corresponding Text object
        ArrayList<TextFlow> sequence = new ArrayList<>(); // to store all sequences per model
        ArrayList<TextFlow> secStructure = new ArrayList<>(); // to store all secondary structures per model
        HashMap<String, Color> colorByResidue = getColorByResidue();

        // Add as many TextFlow objects as models
        for (int i = 0; i < (model.getNumberOfModels() == 0 ? 1 : model.getNumberOfModels()); i++) {
            sequence.add(null); // set null at first to check whether we are at the beginning of a sequences
            secStructure.add(new TextFlow());
        }

        boolean containsSecStruc = false; // to check whether a molecule contains information about secondary structure

        for (PDBPolymer polymer : model.getPolymers()) {
            int modelIndex = polymer.getModelNumber() == 0 ? 0 : polymer.getModelNumber() - 1; // the model index
            TextFlow seq = sequence.get(modelIndex); // the current sequence
            TextFlow sec = secStructure.get(modelIndex); // the current secondary structure

            if (seq == null) // Beginning of sequence for that model
                seq = new TextFlow();
            else { // Add spaces between chains
                Text spaceSeq = new Text("   ");
                spaceSeq.setFont(Font.font("Monospaced", FontWeight.THIN, DEFAULT_FONT_SIZE));
                Text spaceSec = new Text("   ");
                spaceSec.setFont(Font.font("Monospaced", FontWeight.THIN, DEFAULT_FONT_SIZE));

                seq.getChildren().add(spaceSeq);
                sec.getChildren().add(spaceSec);
            }

            // Add chain label at beginning of each chain
            Text seqChain = new Text("Chain " + polymer.getLabel() + ": ");
            seqChain.setFont(Font.font("Monospaced", FontWeight.THIN, DEFAULT_FONT_SIZE));
            Text secChain = new Text("         ");
            secChain.setFont(Font.font("Monospaced", FontWeight.THIN, DEFAULT_FONT_SIZE));

            seq.getChildren().add(seqChain);
            sec.getChildren().add(secChain);

            // Add amino acid sequence and secondary structure
            for (PDBMonomer monomer : polymer.getMonomers()) {
                // Get amino acid label
                Text residue = new Text(monomer.getLabel());
                residue.setFont(Font.font("Monospaced", FontWeight.NORMAL, DEFAULT_FONT_SIZE));
                residue.setFill(colorByResidue.get(monomer.getLabel()).saturate());
                seq.getChildren().add(residue);

                // Add to monomer-text HashMap
                monomerToText.put(monomer, residue);

                // Get secondary structure at the position
                String secStruct = monomer.getSecondaryStructureType();
                if (secStruct == null) {
                    Text space = new Text(" ");
                    space.setFont(Font.font("Monospaced", FontWeight.NORMAL, DEFAULT_FONT_SIZE));
                    sec.getChildren().add(space);
                } else {
                    Text secRes = new Text(secStruct);
                    secRes.setFont(Font.font("Monospaced", FontWeight.NORMAL, DEFAULT_FONT_SIZE));
                    secRes.setFill(secStruct.equals("H") ? Color.RED.saturate() : Color.LIGHTGREEN.saturate());
                    sec.getChildren().add(secRes);
                    containsSecStruc = true;
                }
            }

            // Reset the sequence and secondary structure for that model
            sequence.set(modelIndex, seq);
            secStructure.set(modelIndex, sec);
        }

        // Add everything to sequences ArrayList
        for (int i = 0; i < sequence.size(); i++) {
            if (!containsSecStruc) {
                sequences.add(new TextFlow(sequence.get(i))); // wrap in TextFlow to make model selection easier
            } else {
                sequences.add(new TextFlow(sequence.get(i), new Text("\n"), secStructure.get(i)));
            }
        }

        // Clear sequence and sec. structure from before
        tf.getChildren().clear();

        // Add sequence and sec. structure information of first model to current display
        tf.getChildren().add(sequences.get(0));

        // Adjust height of the ScrollPane depending on availability of secondary structure information
        if (!containsSecStruc) {
            sp.setPrefHeight(35);
            sp.setMaxHeight(35);
        } else {
            sp.setPrefHeight(55);
            sp.setMaxHeight(55);
        }

        return monomerToText;
    }

    /**
     * Returns a HashMap that maps residues to a color following the Lesk coloring scheme for amino acids.
     * @return HashMap: to map residues to colors
     */
    private static HashMap<String, Color> getColorByResidue() {
        // Residue coloring according to Lesk scheme taken from https://www.bioinformatics.nl/~berndb/aacolour.html
        // PYL (O) treated as LYS, SEC (U) as CYS; unknown amino acids get assigned dark gray color
        final List<String> oneLetterCode = Arrays.asList("G", "A", "S", "T", "C", "V", "I", "L", "P", "F", "Y", "M",
                "W", "N", "H", "Q", "D", "E", "K", "R", "O", "U", "X");
        final List<Color> residueColor = Arrays.asList(Color.ORANGE, Color.ORANGE, Color.ORANGE, Color.ORANGE,
                Color.LIGHTGREEN, Color.LIGHTGREEN, Color.LIGHTGREEN, Color.LIGHTGREEN, Color.LIGHTGREEN,
                Color.LIGHTGREEN, Color.LIGHTGREEN, Color.LIGHTGREEN, Color.LIGHTGREEN, Color.MAGENTA, Color.MAGENTA,
                Color.MAGENTA, Color.RED, Color.RED, Color.BLUE, Color.BLUE, Color.BLUE, Color.LIGHTGREEN,
                Color.DARKGRAY);
        HashMap<String, Color> colorByResidue = new HashMap<>();
        for (int i = 0; i < oneLetterCode.size(); i++) {
            colorByResidue.put(oneLetterCode.get(i), residueColor.get(i));
        }
        return colorByResidue;
    }

    /**
     * Generates a String as input for the color scheme legend in HTMl-format. Was generated using ChatGPT for the main
     * structure of the HTML-format but highly adapted.
     * @param colorScheme (int): the index of the color scheme currently applied
     * @param chains (ArrayList): a list of all chains contained in the displayed model
     * @return String: content for the legend in HTML format
     */
    public static String generateLegendContent(int colorScheme, ArrayList<String> chains) {
        // Create StringBuilder to generate content for the legend; start is always the same
        StringBuilder htmlContent = new StringBuilder("""
                <!DOCTYPE html><html><head>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                        }
                        .circle {
                            display: inline-block;
                            width: 10px;
                            height: 10px;
                            border-radius: 50%;
                            margin-right: 10px;
                        }
                    </style></head><body>
                """);

        // Add colors and labels depending on displayed color scheme
        switch (colorScheme) {
            case 0 -> { // colored by atoms
                for (String key : PDBAtom.getColorMap().keySet()) {
                    htmlContent.append("<div><span class=\"circle\" style=\"background-color: #");
                    htmlContent.append(PDBAtom.getColorMap().get(key).toString(), 2, 8);
                    htmlContent.append(";\"></span><span style=\"font-size: 13px;\">");
                    htmlContent.append(key);
                    htmlContent.append("</span></div>");
                }
            }
            case 1 -> { // colored by residue
                Map<String, Color> residueMap = Map.of("Small nonpolar", Color.ORANGE, "Hydrophobic",
                        Color.LIGHTGREEN, "Polar", Color.MAGENTA, "Neg. charged", Color.RED,
                        "Pos. charged", Color.BLUE);
                for (String key : residueMap.keySet()) {
                    htmlContent.append("<div><span class=\"circle\" style=\"background-color: #");
                    htmlContent.append(residueMap.get(key).toString(), 2, 8);
                    htmlContent.append(";\"></span><span style=\"font-size: 12px;\">");
                    htmlContent.append(key);
                    htmlContent.append("</span></div>");
                }
            }
            case 2 -> { // colored by secondary structure
                htmlContent.append("<div><span class=\"circle\" style=\"background-color: red;\"></span><span style=\"font-size: 13px;\">Helix</span></div>");
                htmlContent.append("<div><span class=\"circle\" style=\"background-color: lightgreen;\"></span><span style=\"font-size: 13px;\">Sheet</span></div>");
                htmlContent.append("<div><span class=\"circle\" style=\"background-color: gray;\"></span><span style=\"font-size: 13px;\">Coil</span></div>");
            }
            case 3 -> { // colored by molecule
                // chains colored by rainbow colors: red, orange, yellow, green, cyan, blue, violet
                final List<Color> colorByChain = Arrays.asList(Color.VIOLET, Color.RED, Color.ORANGE, Color.YELLOW,
                        Color.GREEN, Color.CYAN, Color.BLUE);
                for (int i = 0; i < chains.size(); i++) {
                    int chainNumber = (i + 1) % colorByChain.size();
                    htmlContent.append("<div><span class=\"circle\" style=\"background-color: #");
                    htmlContent.append(colorByChain.get(chainNumber).toString(), 2, 8);
                    htmlContent.append(";\"></span><span style=\"font-size: 13px;\">Chain ");
                    htmlContent.append(chains.get(i));
                    htmlContent.append("</span></div>");
                }
            }
        }

        // Attach end to the StringBuilder; always the same
        htmlContent.append("</body></html>\n");

        return htmlContent.toString();
    }
}
