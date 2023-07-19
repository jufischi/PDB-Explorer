package pdbexplorer.window;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.SetChangeListener;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import pdbexplorer.model.protein.PDBAtom;
import pdbexplorer.model.protein.PDBComplex;
import pdbexplorer.model.protein.PDBMonomer;
import pdbexplorer.model.protein.PDBPolymer;
import pdbexplorer.model.selection.MonomerSelectionModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Connects the figure objects of a molecule with the selection model, making PDBMonomers contained in the molecule
 * selectable.
 */
public class ComplexSelectionHandler {
    private final PDBComplex model;
    private final Group balls;
    private final Group sticks;
    private final ArrayList<TextFlow> sequences;
    private final Button deselect;
    private final HashMap<PDBAtom, Sphere> atomToSphere;
    private final HashMap<PDBMonomer, Text> monomerToText;
    private final HashMap<Sphere, PDBMonomer> ballToMonomer;
    private final HashMap<PDBMonomer, ArrayList<Sphere>> monomerToBalls;
    private final HashMap<Text, PDBMonomer> textToMonomer;
    private final ArrayList<Cylinder> listOfSticks = new ArrayList<>();
    private final MonomerSelectionModel monomerSelectionModel = new MonomerSelectionModel();
    private static final int DEFAULT_FONT_SIZE = 18;
    private final PieChart pieChartRes;
    private final PieChart pieChartSec;
    private final PieChart pieChartProp;

    /**
     * Constructor for a ComplexSelectionHandler object.
     * @param model (PDBComplex): Molecule currently displayed in the Viewer
     * @param balls (Group): Group containing all balls of the figure
     * @param sticks (Group): Group containing all sticks of the figure
     * @param atomToSphere (HashMap): Maps atoms to their corresponding spheres
     * @param sequences (ArrayList): List containing of sequence TextFlow objects
     * @param monomerToText (HashMap): Maps monomers to their corresponding texts
     * @param deselect (Button): Button to clear the whole selection
     * @param pieChartRes (PieChart): the object to display the residue pie chart in, needed for re-computation
     * @param pieChartSec (PieChart): the object to display the sec. structure pie chart in, needed for re-computation
     */
    public ComplexSelectionHandler(PDBComplex model, Group balls, Group sticks, HashMap<PDBAtom, Sphere> atomToSphere,
                                   ArrayList<TextFlow> sequences, HashMap<PDBMonomer, Text> monomerToText,
                                   Button deselect, PieChart pieChartRes,
                                   PieChart pieChartSec, PieChart pieChartProp) {
        this.model = model;
        this.balls = balls;
        this.sticks = sticks;
        this.sequences = sequences;
        this.deselect = deselect;
        this.monomerToText = monomerToText;
        this.atomToSphere = atomToSphere;
        this.pieChartRes = pieChartRes;
        this.pieChartSec = pieChartSec;
        this.pieChartProp = pieChartProp;

        // Generate a HashMap that maps Spheres to monomers, vice versa and text to monomers
        ballToMonomer = new HashMap<>();
        monomerToBalls = new HashMap<>();
        textToMonomer = new HashMap<>();
        for (PDBPolymer polymer : model.getPolymers()) {
            for (PDBMonomer monomer : polymer.getMonomers()) {
                monomerToBalls.put(monomer, new ArrayList<>());
                for (PDBAtom atom : monomer.getAtoms()) {
                    ballToMonomer.put(atomToSphere.get(atom), monomer);
                    monomerToBalls.get(monomer).add(atomToSphere.get(atom));
                    textToMonomer.put(monomerToText.get(monomer), monomer);
                }
            }
        }
    }

    /**
     * Computes the monomer selection model for a given molecule. It first connects the selection gestures to all
     * Spheres and Text objects of the structure. Then, a Change Listener is added that updates the opacity of the
     * molecule based on the selected items.
     * @return MonomerSelectionModel: the monomer selection model for the given molecule
     */
    public MonomerSelectionModel computeSelectionModel() {
        // Connect selection gestures to each sphere
        for (Node modelGroup : balls.getChildren()) {
            for (Node chainGroup : ((Group) modelGroup).getChildren()) {
                ((Group) chainGroup).getChildren().forEach(ball -> ball.setOnMouseClicked(e -> {
                    if (!e.isShiftDown())
                        monomerSelectionModel.clearSelection();
                    boolean isSelected = monomerSelectionModel.isSelected(ballToMonomer.get((Sphere) ball));
                    monomerSelectionModel.setSelected(ballToMonomer.get(ball), !isSelected);
                }));
            }
        }

        // Connect selection gestures to each text
        for (TextFlow modelTF : sequences) {
            for (Node residue : ((TextFlow) modelTF.getChildren().get(0)).getChildren()) {
                if (textToMonomer.containsKey((Text) residue))
                    residue.setOnMouseClicked(e -> {
                        if (!e.isShiftDown())
                            monomerSelectionModel.clearSelection();
                        boolean isSelected = monomerSelectionModel.isSelected(textToMonomer.get(residue));
                        monomerSelectionModel.setSelected(textToMonomer.get(residue), !isSelected);
                    });
            }
        }

        // Generate list of all sticks
        for (Node modelGroup : sticks.getChildren()) {
            for (Node chainGroup : ((Group) modelGroup).getChildren()) {
                ((Group) chainGroup).getChildren().forEach(stick -> listOfSticks.add((Cylinder) stick));
            }
        }

        // Add Change Listener for the selected items
        double selected = 1.0; // opacity to set for selected items
        double notSelected = 0.2; // opacity to set for not selected items
        monomerSelectionModel.getSelectedItems().addListener((SetChangeListener<? super PDBMonomer>) c -> {
            if (c.wasAdded()) {
                Platform.runLater(() -> {
                    if (monomerSelectionModel.getSelectedItems().size() == 1) { // previously nothing selected
                        updateOpacity(atomToSphere.values(), notSelected);
                        updateOpacity(listOfSticks, notSelected); // set all sticks to low opacity
                        monomerToText.values().forEach(text -> text.setOpacity(notSelected));
                    }
                    updateOpacity(monomerToBalls.get(c.getElementAdded()), selected);
                    monomerToText.get(c.getElementAdded()).setFont(Font.font("Monospaced", FontWeight.BOLD, DEFAULT_FONT_SIZE));
                    monomerToText.get(c.getElementAdded()).setOpacity(selected);
                });
            } else if (c.wasRemoved()) {
                Platform.runLater(() -> {
                    if (monomerSelectionModel.getSelectedItems().size() > 0) {
                        updateOpacity(monomerToBalls.get(c.getElementRemoved()), notSelected);
                        monomerToText.get(c.getElementRemoved()).setOpacity(notSelected);
                    }
                    else {
                        updateOpacity(atomToSphere.values(), selected); // nothing selected
                        updateOpacity(listOfSticks, selected);
                        monomerToText.values().forEach(text -> text.setOpacity(selected));
                    }
                });
                monomerToText.get(c.getElementRemoved()).setFont(Font.font("Monospaced", FontWeight.NORMAL, DEFAULT_FONT_SIZE));
            }
            // Update Residue Pie Chart
            ChartHandler.createPieCharts(model, monomerSelectionModel, pieChartRes, pieChartSec, pieChartProp);
        });

        // Add function to deselect button
        deselect.setOnAction(e -> monomerSelectionModel.clearSelection());
        deselect.disableProperty().bind(Bindings.isEmpty(monomerSelectionModel.getSelectedItems()));

        return monomerSelectionModel;
    }

    /**
     * Updates the opacity of given Shape objects according to the given opacity.
     * @param list (Collection): list of spheres or cylinders
     * @param opacity (double): the new opacity value to be applied to the shapes
     */
    private static void updateOpacity(Collection<? extends Shape3D> list, double opacity) {
        for (var shape : list) {
            Color color = ((PhongMaterial) shape.getMaterial()).getDiffuseColor();
            color = new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
            ((PhongMaterial) shape.getMaterial()).setDiffuseColor(color);
        }
    }
}
