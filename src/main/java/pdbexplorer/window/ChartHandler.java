package pdbexplorer.window;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import pdbexplorer.model.protein.PDBAtom;
import pdbexplorer.model.protein.PDBComplex;
import pdbexplorer.model.protein.PDBMonomer;
import pdbexplorer.model.protein.PDBPolymer;
import pdbexplorer.model.selection.MonomerSelectionModel;

import java.util.*;

/**
 * This class handles all charts present in the program.
 */
public class ChartHandler {
    /**
     * Computes a list of monomers from the given molecule to use for the creation of pie charts. The pie charts are
     * then created by calling upon the private function createPieCharts.
     * @param model (PDBComplex): Molecule currently displayed in the Viewer
     * @param pieChartResidue (PieChart): the object to display the pie chart for residue count
     * @param pieChartSecStruc (PieChart): the object to display the pie chart for secondary structure count
     * @param pieChartProperties (PieChart): the object to display the properties of contained amino acids
     */
    public static void createPieCharts(PDBComplex model, PieChart pieChartResidue, PieChart pieChartSecStruc,
                                       PieChart pieChartProperties) {
        // Prepare list of monomers used for creation of pie charts, Take into account all monomers of first model,
        // assumes that all models have the same composition
        List<PDBMonomer> monomers = new ArrayList<>();

        for (PDBPolymer polymer : model.getPolymers()) {
            if (polymer.getModelNumber() == 0 || polymer.getModelNumber() == 1) {
                monomers.addAll(polymer.getMonomers());
            }
        }

        createPieCharts(monomers, pieChartResidue, pieChartSecStruc, pieChartProperties);
    }

    /**
     * Decides whether to use the whole list of monomers from the given molecule for the creation of the pie charts or
     * just selected ones. The pie charts are then created by calling upon the private function createPieCharts.
     * @param model (PDBComplex): Molecule currently displayed in the Viewer
     * @param selectionModel (MonomerSelectionModel): the selection model of the currently displayed molecule
     * @param pieChartResidue (PieChart): the object to display the pie chart for residue count in
     * @param pieChartSecStruc (PieChart): the object to display the pie chart for secondary structure count in
     * @param pieChartProperties (PieChart): the object to display the properties of contained amino acids
     */
    public static void createPieCharts(PDBComplex model, MonomerSelectionModel selectionModel,
                                       PieChart pieChartResidue, PieChart pieChartSecStruc,
                                       PieChart pieChartProperties) {
        // Prepare list of monomers used for creation of pie charts
        if (selectionModel.getSelectedItems().isEmpty()) {
            // Take into account all monomers
            createPieCharts(model, pieChartResidue, pieChartSecStruc, pieChartProperties);
        } else {
            createPieCharts(FXCollections.observableArrayList(selectionModel.getSelectedItems()),
                    pieChartResidue, pieChartSecStruc, pieChartProperties);
        }
    }

    /**
     * Computes all pie charts for the given list of monomers, i.e. the total residue count, the number of secondary
     * structures and the distribution of amino acid properties.
     * @param monomers (List): a list of monomers to use for making the pie chart
     * @param pieChartResidue (PieChart): the object to display the pie chart for residue count in
     * @param pieChartSecStruc (PieChart): the object to display the pie chart for secondary structure count in
     * @param pieChartProperties (PieChart): the object to display the properties of contained amino acids
     */
    private static void createPieCharts(List<PDBMonomer> monomers, PieChart pieChartResidue, PieChart pieChartSecStruc,
                                        PieChart pieChartProperties) {
        // To have three-letter codes in chart; unknown amino acids are added as "UNK"
        List<String> oneLetterCode = Arrays.asList("A", "C", "D", "E", "F", "H", "I", "K", "L", "M", "N",
                "P", "Q", "R", "S", "T", "V", "W", "Y", "G", "O", "U", "X");
        List<String> threeLetterCode = Arrays.asList("ALA", "CYS", "ASP", "GLU", "PHE", "HIS", "ILE",
                "LYS", "LEU", "MET", "ASN", "PRO", "GLN", "ARG", "SER", "THR", "VAL", "TRP", "TYR", "GLY",
                "PYL", "SEC", "UNK");

        HashMap<String, String> oneToThreeLetterCode = new HashMap<>();
        for (int i = 0; i < oneLetterCode.size(); i++) {
            oneToThreeLetterCode.put(oneLetterCode.get(i), threeLetterCode.get(i));
        }

        // To map amino acids to their corresponding property; properties taken from
        // http://www.geneinfinity.org/sp/sp_aaprops.html; O and U treated like K and C, respectively
        List<String> properties = Arrays.asList("Nonpolar", "Polar", "Neg. charged", "Neg. charged", "Aromatic",
                "Pos. charged", "Nonpolar", "Pos. charged", "Nonpolar", "Nonpolar", "Polar", "Polar", "Polar",
                "Pos. charged", "Polar", "Polar", "Nonpolar", "Aromatic", "Aromatic", "Nonpolar", "Pos. charged",
                "Polar", "Unknown");

        HashMap<String, String> residueToProperty = new HashMap<>();
        for (int i = 0; i < oneLetterCode.size(); i++) {
            residueToProperty.put(oneLetterCode.get(i), properties.get(i));
        }

        // Clear previous pie charts
        pieChartResidue.getData().clear();
        pieChartSecStruc.getData().clear();
        pieChartProperties.getData().clear();

        // HashMap to compute residue and secondary structure counts
        HashMap<String, Integer> residueCount = new HashMap<>();
        HashMap<String, Integer> secStrucCount = new HashMap<>();
        HashMap<String, Integer> propertyCount = new HashMap<>();
        int totalCount = 0;

        // Prepare data for PieChart
        for (PDBMonomer monomer : monomers) {
            // Count occurrence of different residues
            String monomerLabel = oneToThreeLetterCode.get(monomer.getLabel());
            residueCount.put(monomerLabel, residueCount.getOrDefault(monomerLabel, 0) + 1);
            totalCount += 1;

            // Count occurrence of different secondary structure types
            String monomerSecStruc;
            if (monomer.getSecondaryStructureType() == null)
                monomerSecStruc = "COIL";
            else
                monomerSecStruc = switch (monomer.getSecondaryStructureType()) {
                case "H" -> "HELIX";
                case "S" -> "SHEET";
                default -> "COIL";
            };
            secStrucCount.put(monomerSecStruc, secStrucCount.getOrDefault(monomerSecStruc, 0) + 1);

            // Count occurrences of amino acid properties
            String property = residueToProperty.get(monomer.getLabel());
            propertyCount.put(property, propertyCount.getOrDefault(property, 0) + 1);
        }

        // Compute Pie Charts
        computePieChart(residueCount, "Residue Composition (n = " + totalCount + ")", pieChartResidue, totalCount);
        computePieChart(secStrucCount, "Secondary Structure Composition", pieChartSecStruc, totalCount);
        computePieChart(propertyCount, "Amino Acid Properties", pieChartProperties, totalCount);
    }

    /**
     * Computes a pie chart using the given data and title.
     * @param countsToDisplay (HashMap): contains values and counts to display in Pie Chart
     * @param title (String): title of the HashMap
     * @param pieChart (PieChart): the object to display the pie chart in
     * @param totalCount (int): the total amino acid count
     */
    private static void computePieChart(HashMap<String, Integer> countsToDisplay, String title, PieChart pieChart,
                                        int totalCount) {
        HashMap<String, Double> percentages = new HashMap<>(); // to compute percentages
        // Add residue counts to Pie Chart
        for (String key : countsToDisplay.keySet()) {
            pieChart.getData().add(new PieChart.Data(key, countsToDisplay.get(key)));
            percentages.put(key, (double) countsToDisplay.get(key) / totalCount);
        }

        // Define a custom color palette with 23 distinct colors; generated using ChatGPT
        Color[] colors = new Color[]{
                Color.web("#1f77b4"), Color.web("#ff7f0e"), Color.web("#2ca02c"), Color.web("#d62728"),
                Color.web("#9467bd"), Color.web("#8c564b"), Color.web("#e377c2"), Color.web("#7f7f7f"),
                Color.web("#bcbd22"), Color.web("#17becf"), Color.web("#aec7e8"), Color.web("#ffbb78"),
                Color.web("#98df8a"), Color.web("#ff9896"), Color.web("#c5b0d5"), Color.web("#c49c94"),
                Color.web("#f7b6d2"), Color.web("#c7c7c7"), Color.web("#dbdb8d"), Color.web("#9edae5"),
                Color.web("#1c5c68"), Color.web("#f28455"), Color.web("#8dd3c7")
        };

        // Set custom colors for each data item in the PieChart; code taken from ChatGPT
        for (int i = 0; i < pieChart.getData().size(); i++) {
            pieChart.getData().get(i).getNode().setStyle("-fx-pie-color: " + toRGBCode(colors[i]) + ";");
        }

        // Set custom colors for legend as well as add percentage of occurrence
        // Legend coloring adjusted from https://gist.github.com/jewelsea/1422628
        Set<Node> items = pieChart.lookupAll("Label.chart-legend-item");
        int i = 0;
        for (Node item : items) {
            // Adjust legend colors
            Label label = (Label) item;
            final Circle circle = new Circle(7, colors[i]);
            label.setGraphic(circle);

            // Set text of legend to include percentage
            label.setText(label.getText() + ": " + Math.round(
                    percentages.get(label.getText()) * 1000.0) / 10.0 + " %");
            i++;
        }

        // Set title of pie chart
        pieChart.setTitle(title);
    }

    /**
     * Converts the given hex web value color to RGB color code. Code generated using ChatGPT.
     * @param color (Color): color as hex web value
     * @return Color: color as RGB code
     */
    private static String toRGBCode(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    /**
     * Computes a Ramachandran plot of phi and psi angles of the protein backbone. If more than one model is available
     * for a molecule, only the first one is used for the computation.
     */
    public static void createRamachandranPlot(PDBComplex model, ScatterChart<Number, Number> scatterChart) {
        // Set up the chart
        NumberAxis xAxis = (NumberAxis) scatterChart.getXAxis();
        NumberAxis yAxis = (NumberAxis) scatterChart.getYAxis();
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(-180);
        xAxis.setUpperBound(180);
        xAxis.setTickUnit(60);
        xAxis.setMinorTickCount(6);
        xAxis.setLabel("Φ");
        xAxis.setTickLabelFormatter(new DegreeLabelFormatter(xAxis));
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(-180);
        yAxis.setUpperBound(180);
        yAxis.setTickUnit(60);
        yAxis.setMinorTickCount(6);
        yAxis.setLabel("Ψ");
        yAxis.setTickLabelFormatter(new DegreeLabelFormatter(yAxis));

        scatterChart.setTitle("Ramachandran Plot");
        scatterChart.getStyleClass().add("small-chart-symbols");

        // Computation of points for scatter plot
        XYChart.Series<Number, Number> series = new XYChart.Series<>(); // x = phi, y = psi angles
        series.setName("Torsion Angles");

        // Loop through model
        for (PDBPolymer polymer : model.getPolymers()) {
            if (polymer.getModelNumber() == 0 || polymer.getModelNumber() == 1) {
                // Go over all monomers contained (skip first and last residue because we cannot compute both angles)
                for (int i = 1; i < polymer.getMonomers().size()-1; i++) {
                    // Extract needed atoms
                    // N, C, C-alpha of current monomer
                    PDBAtom n = polymer.getMonomers().get(i).getN();
                    PDBAtom c = polymer.getMonomers().get(i).getC();
                    PDBAtom cAlpha = polymer.getMonomers().get(i).getCAlpha();
                    // C of previous monomer
                    PDBAtom cPrior = polymer.getMonomers().get(i-1).getC();
                    // N of next monomer
                    PDBAtom nNext = polymer.getMonomers().get(i+1).getN();

                    // Only continue if all needed atoms are present
                    if (n != null && c != null && cAlpha != null && cPrior != null && nNext != null) {
                        // Compute phi and psi angles
                        double phi = computeDihedralAngle(cPrior.getCoordinates(), n.getCoordinates(),
                                cAlpha.getCoordinates(), c.getCoordinates());
                        double psi = computeDihedralAngle(n.getCoordinates(), cAlpha.getCoordinates(),
                                c.getCoordinates(), nNext.getCoordinates());

                        // Add to series
                        series.getData().add(new XYChart.Data<>(phi, psi));
                    }
                }
            }
        }

        // Add data to Ramachandran plot
        ObservableList<XYChart.Series<Number, Number>> data = FXCollections.observableArrayList();
        data.add(series);
        scatterChart.setData(data);
        scatterChart.setLegendVisible(false);
    }

    /**
     * Computes dihedral angle between four atoms. Adapted for Java from
     * https://stackoverflow.com/questions/20305272/dihedral-torsion-angle-from-four-points-in-cartesian-coordinates-in-python.
     * @param p1 (Point3D): coordinates of first atom
     * @param p2 (Point3D): coordinates of second atom
     * @param p3 (Point3D): coordinates of third atom
     * @param p4 (Point3D): coordinates of fourth atom
     * @return double: the dihedral angle
     */
    private static double computeDihedralAngle(Point3D p1, Point3D p2, Point3D p3, Point3D p4) {
        // Compute vectors of the three atom bonds, normalize b2 such that it does not influence magnitude of vector
        // rejection
        Point3D b1 = p2.subtract(p1).multiply(-1.0);
        Point3D b2 = p3.subtract(p2).normalize();
        Point3D b3 = p4.subtract(p3);

        // vector rejections
        Point3D n1 = b1.subtract(b2.multiply(b1.dotProduct(b2)));
        Point3D n2 = b3.subtract(b2.multiply(b3.dotProduct(b2)));

        // torsion angle: angle between n_1 and n_2 in a plane
        double x = n1.dotProduct(n2);
        double y = b2.crossProduct(n1).dotProduct(n2);

        return Math.toDegrees(Math.atan2(y, x));
    }

    /**
     * This class is used to format the Ramachandran plot in a way that the x- and y-axis labels have a degree sign
     * after them. The class was generated using ChatGPT.
     */
    private static class DegreeLabelFormatter extends NumberAxis.DefaultFormatter {
        /**
         * Constructor for DegreeLabelFormatter.
         * @param numberAxis (NumberAxis): the axis the format should be applied to
         */
        public DegreeLabelFormatter(NumberAxis numberAxis) {
            super(numberAxis);
        }

        /**
         * Adds a degree sign to the axis label.
         * @param object (Number): the axis label
         * @return String: the value to put as axis tick-labels
         */
        public String toString(Number object) {
            // Add the degree sign to the tick label
            return super.toString(object) + "°";
        }
    }
}
