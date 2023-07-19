package pdbexplorer.window;

import javafx.animation.Animation;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pdbexplorer.model.CheckBoxListViewItem;
import pdbexplorer.model.PDBWebClient;
import pdbexplorer.model.io.PDBParser;
import pdbexplorer.model.protein.PDBAtom;
import pdbexplorer.model.protein.PDBComplex;
import pdbexplorer.model.protein.PDBMonomer;
import pdbexplorer.model.protein.PDBPolymer;
import pdbexplorer.model.selection.MonomerSelectionModel;
import pdbexplorer.model.undo.PropertyCommand;
import pdbexplorer.model.undo.UndoRedoManager;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * This class implements the Presenter part of the ModelViewPresenter programming pattern.
 */
public class WindowPresenter {
    // Controller class used to get access to the UI components
    private final WindowController controller;

    // Model class containing all functions concerning the computations
    private PDBComplex model;

    // Groups for display of balls, sticks and ribbons
    private final Group balls = new Group();
    private final Group sticks = new Group();
    private final Group ribbons = new Group();
    private final Group outerGroup = new Group();
    // ArrayList to store sequences TextFlows
    private final ArrayList<TextFlow> sequences = new ArrayList<>();

    // Define the camera globally as well as default camera settings
    private final PerspectiveCamera camera;
    private final double defaultNearClip = 0.1;
    private final int defaultFarClip = 10000;
    private final double defaultTranslateZ = -30;

    // previous coordinates (needed for rotation of the molecule)
    private double xPrev;
    private double yPrev;

    // Hashmap to store references of PDBAtoms to Spheres and PDBMonomers to Texts
    private HashMap<PDBAtom, Sphere> atomToSphere = new HashMap<>();
    private HashMap<PDBMonomer, Text> monomerToText = new HashMap<>();

    // Filtered List for display of and search in PDB entries
    private FilteredList<String> inputList;

    // Services that are needed globally
    private Service<Void> serviceRibbon;

    // Text contained in PDB file
    private String pdbFileContent;
    private String pdbFileName;

    // Undo Redo Manager
    private final UndoRedoManager undoManager = new UndoRedoManager();

    // Selection Model
    MonomerSelectionModel selectionModel;

    // Timelines for Animations
    private Timeline rotateAnimation;
    private final ObservableList<String> chains = FXCollections.observableArrayList();
    private final IntegerProperty numberOfModels = new SimpleIntegerProperty(0);

    // To make sure that PDB file does not get randomly selected when searching for PDB file
    private final BooleanProperty inSearch = new SimpleBooleanProperty(false);

    /**
     * Constructor of the Presenter that handles all communication between the UI (Controller) and the Model.
     *
     * @param stage: Stage, Main stage of the program
     * @param view:  WindowView, View of the program, it contains the controller that is needed to access the UI
     * @param model: Tree, Model containing all the data structures for the data and all algorithms
     */
    public WindowPresenter(Stage stage, WindowView view, PDBComplex model) {
        // Define Controller and Model
        this.controller = view.getController();
        this.model = model;

        // Generate a camera for better visualization
        camera = new PerspectiveCamera(true);
        camera.setNearClip(defaultNearClip);
        camera.setFarClip(defaultFarClip);
        camera.setTranslateZ(defaultTranslateZ);

        // Setup of the pane and the sub-scene with the camera
        outerGroup.getChildren().add(balls);
        outerGroup.getChildren().add(sticks);
        outerGroup.getChildren().add(ribbons);
        SubScene subScene = new SubScene(outerGroup, 1000, 600, true, SceneAntialiasing.BALANCED);
        subScene.setCamera(camera);

        // bind the width and height to have everything centered
        subScene.heightProperty().bind(controller.getMainPane().heightProperty());
        subScene.widthProperty().bind(controller.getMainPane().widthProperty());

        // add the sub-scene to the pane
        controller.getMainPane().getChildren().add(subScene);

        // Setup mouse pane action (rotation)
        setupPaneMouseAction();

        // Give functionality to menu
        setMenuFunctionality(stage);

        // Give functionality to button bars
        setToolBarFunctionality(stage);

        // Setup model and chain selection from ListView in models and chains tabs
        setupSelectModelNumberAndChain();

        // Setup visibility of Scrollbar showing sequence
        controller.getSequenceScrollBar().visibleProperty().bind(Bindings.isEmpty(balls.getChildren()).not());

        // Setup Service for Tasks: PDBWebClient
        setupWebClientService(stage);

        // Clear undo redo manager at beginning
        undoManager.clear();

        // Bind disable property of figure tab
        controller.getStatsTab().disableProperty().bind(Bindings.isEmpty(balls.getChildren()));
        controller.getChartsScrollPane().setFitToWidth(true); // to make sure the charts are centered

        // Setup color legend (adjust height to content and fill legend with atom colors at beginning)
        controller.getLegendWV().getEngine().getLoadWorker().stateProperty().addListener((v, o, n) -> {
            if (n == Worker.State.SUCCEEDED)
                controller.getLegendWV().setPrefHeight((int) controller.getLegendWV().getEngine()
                        .executeScript("document.documentElement.scrollHeight"));
        });
        controller.getLegendWV().getEngine().loadContent(ComplexFigure.generateLegendContent(0,
                model.getChains()));
        controller.getLegendTP().disableProperty().bind(Bindings.isEmpty(balls.getChildren()));
        controller.getLegendTP().setExpanded(false);
    }

    /**
     * Updates the molecule on the pane when a new molecule is selected or loaded. For this, the old model is cleared,
     * balls and sticks for the new model computed as well as the ribbonService set up (this will be called later, when
     * the ribbon display is wanted by the user). Also, all the bindings of balls and sticks groups to the respective
     * Checkboxes and Sliders are created. Further, Camera and Checkboxes as well as Sliders reset.
     */
    private void updateMoleculeOnPane(Stage stage) {
        // clear everything from previous model
        balls.getChildren().clear();
        sticks.getChildren().clear();
        ribbons.getChildren().clear();
        sequences.clear();
        controller.getChainListView().getItems().clear();
        chains.clear();
        controller.getInfoLabel().setText("");
        controller.getResiduePieChart().getData().clear();
        controller.getSecStrucPieChart().getData().clear();
        controller.getPropertiesPC().getData().clear();
        controller.getRamachandranPlot().getData().clear();

        // Stop rotation animation (if ongoing)
        if (rotateAnimation != null) {
            rotateAnimation.stop();
        }

        // Get size of protein to determine translateZ for camera
        int proteinSize = 0;
        for (PDBPolymer polymer : model.getPolymers()) {
            if (polymer.getModelNumber() == 0 || polymer.getModelNumber() == 1)
                proteinSize += polymer.getMonomers().size();
            else
                break; // assuming that model 1 is always written first in PDB file
        }

        // Reset camera
        camera.setNearClip(defaultNearClip);
        camera.setFarClip(defaultFarClip);
        camera.setTranslateZ(defaultTranslateZ * Math.log(proteinSize == 0 ? 1000 : proteinSize));

        if (model.isProtein()) { // Compute new figure in case PDB file contains protein
            // Reset chains list
            chains.addAll(model.getChains());
            numberOfModels.set(model.getNumberOfModels());

            // Create service to compute nodes for the new model
            Service<HashMap<PDBAtom, Sphere>> serviceFigure = new Service<>() {
                @Override
                protected Task<HashMap<PDBAtom, Sphere>> createTask() {
                    return new ComplexFigure.ComputeFigure(model, balls, sticks);
                }
            };
            // In case of failure:
            serviceFigure.setOnFailed((WorkerStateEvent event) -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "The molecule could not be rendered. Please select another PDB file.");
                alert.show();
            });
            serviceFigure.setOnSucceeded((WorkerStateEvent event) -> {
                atomToSphere = serviceFigure.getValue(); // retrieve atomToSphere HashMap

                // Apply scales to all shapes
                for (Node modelGroup : balls.getChildren()) {
                    for (Node chainGroup : ((Group) modelGroup).getChildren()) {
                        ((Group) chainGroup).getChildren().forEach(ball -> ((Sphere) ball).radiusProperty().bind(
                                controller.getAtomsSlider().valueProperty().multiply(((Sphere) ball).getRadius())));
                    }
                }
                for (Node modelGroup : sticks.getChildren()) {
                    for (Node chainGroup : ((Group) modelGroup).getChildren()) {
                        ((Group) chainGroup).getChildren().forEach(ball -> ((Cylinder) ball).radiusProperty().bind(
                                controller.getBondsSlider().valueProperty().multiply(((Cylinder) ball).getRadius())));
                    }
                }

                // Fill list of chains
                for (String chain : model.getChains()) {
                    CheckBoxListViewItem item = new CheckBoxListViewItem("Chain " + chain, true);

                    // Bind CheckBox to visibility of respective chain
                    for (Node modelGroup : balls.getChildren()) {
                        ((Group) modelGroup).getChildren().get(model.getChains().indexOf(chain)).visibleProperty()
                                .bind(item.onProperty());
                    }
                    for (Node modelGroup : sticks.getChildren()) {
                        ((Group) modelGroup).getChildren().get(model.getChains().indexOf(chain)).visibleProperty()
                                .bind(item.onProperty());
                    }

                    // Add Undo/Redo functionality
                    item.onProperty().addListener((v, o, n) ->
                            undoManager.add(new PropertyCommand<>("chain selection", (BooleanProperty) v, o, n)));

                    // Add CheckBox to ChainListView
                    controller.getChainListView().getItems().add(item);
                }
                // Disable chains tab in case only one chain is present
                controller.getChainsTab().setDisable(model.getChains().size() == 1 || model.getChains().size() == 0);

                // Fill ArrayList of models
                ArrayList<String> models = new ArrayList<>();
                for (int i = 0; i < (model.getNumberOfModels() == 0 ? 1 : model.getNumberOfModels()); i++) {
                    models.add("Model " + (i + 1));
                }
                // Add list of models to display
                controller.getModelListView().setItems(FXCollections.observableArrayList(models));
                controller.getModelListView().getSelectionModel().clearSelection();
                if (model.getNumberOfModels() == 0) // disable models tab if only one model present
                    controller.getModelsTab().setDisable(true);
                else { // else enable models tab and show first model only
                    controller.getModelsTab().setDisable(false);

                    // First set all invisible
                    sticks.getChildren().forEach(group -> group.setVisible(false));
                    balls.getChildren().forEach(group -> group.setVisible(false));
                    // Then set only first model visible through modelList listener
                    controller.getModelListView().getSelectionModel().selectFirst();
                }

                // Reset color scheme choice
                controller.getColorSchemeChoiceBox().getSelectionModel().selectFirst();

                // Apply selection model to the molecule
                ComplexSelectionHandler selectionHandler = new ComplexSelectionHandler(model, balls, sticks,
                        atomToSphere, sequences, monomerToText, controller.getDeselectButton(),
                        controller.getResiduePieChart(), controller.getSecStrucPieChart(), controller.getPropertiesPC());
                selectionModel = selectionHandler.computeSelectionModel();
            });
            // bind visibility and progress of ProgressBar to the service (only show in case of loading PDB file)
            controller.getGeneralProgress().visibleProperty().bind(serviceFigure.runningProperty());
            controller.getGeneralProgress().progressProperty().bind(serviceFigure.progressProperty());
            serviceFigure.restart(); // Start the calculation

            // Setup ribbons service
            serviceRibbon = new Service<>() {
                @Override
                protected Task<Void> createTask() {
                    return new ComplexFigure.ComputeRibbon(model, ribbons);
                }
            };
            // In case of failure:
            serviceRibbon.setOnFailed((WorkerStateEvent event) -> {
                Alert alert = new Alert(Alert.AlertType.WARNING, "The Ribbons could not be rendered properly.");
                alert.show();
            });
            serviceRibbon.setOnSucceeded((WorkerStateEvent event) -> {
                // Bind Chains CheckBox to visibility of respective chain
                if (model.getChains().size() > 1) {
                    for (CheckBoxListViewItem item : controller.getChainListView().getItems()) {
                        for (Node group : ribbons.getChildren()) {
                            if (!((Group) group).getChildren().isEmpty()) // Needed because the first model loaded randomly has one model subgroup in ribbons too much
                                ((Group) group).getChildren().get(controller.getChainListView().getItems()
                                        .indexOf(item)).visibleProperty().bind(item.onProperty());
                        }
                    }
                }
                // Set currently selected model visible
                ribbons.getChildren().forEach(group -> group.setVisible(false));
                int selectedModelIndex = controller.getModelListView().getSelectionModel().getSelectedIndex();
                ribbons.getChildren().get(selectedModelIndex == -1 ? 0 : selectedModelIndex).setVisible(true);
            });

            // Reset atoms and bonds selection and sliders
            controller.getAtomsCB().setSelected(true);
            controller.getBondsCB().setSelected(true);
            controller.getRibbonsCB().setSelected(false);
            controller.getAtomsSlider().setValue(1.0);
            controller.getBondsSlider().setValue(1.0);

            // Display sequence of the molecule
            monomerToText = ComplexFigure.computeSequence(model, sequences, controller.getSequenceTextFlow(),
                    controller.getSequenceScrollBar());

            // Update Charts for the first models
            ChartHandler.createPieCharts(model, controller.getResiduePieChart(), controller.getSecStrucPieChart(),
                    controller.getPropertiesPC());
            ChartHandler.createRamachandranPlot(model, controller.getRamachandranPlot());

            // Set stylesheet for Ramachandran plot
            URL stylesURL = getClass().getResource("chart.css");
            if (stylesURL != null) {
                stage.getScene().getStylesheets().add(stylesURL.toExternalForm());
            }
        } else { // In case a PDB file does not contain protein, tell the user
            // Disable chains tab in case no protein is present
            controller.getChainsTab().setDisable(true);
            // Do not expand legend if no protein present
            controller.getLegendTP().setExpanded(false);

            Alert alert = new Alert(Alert.AlertType.WARNING, "This file does not contain any protein!");
            alert.show();
        }

        // Set info label to show information about PDB
        controller.getInfoLabel().setText("Showing " + pdbFileName + ".pdb");

        // Clear undo/redo manager for new molecule
        undoManager.clear();
    }

    /**
     * Sets the functionality for the Model and Chain Tabs that display the different models and chains contained in a
     * PDB file (if applicable). The tabs are disabled if only one model/chain is present. In case there are more models
     * or chains present, the
     * - model to be displayed can be selected by pressing on the respective model in the list. The previously
     * selected model will be made invisible while the new model will be made visible.
     * - chain(s) to be displayed can be selected by selecting the respective CheckBoxes. All selected chains are
     * displayed.
     */
    private void setupSelectModelNumberAndChain() {
        // Setup model selection from ListView in models tab
        controller.getModelListView().getSelectionModel().selectedItemProperty().addListener((v, o, n) -> {
            int selectedModel = controller.getModelListView().getSelectionModel().getSelectedIndex();

            if (selectedModel != -1) {
                // Remove all balls and sticks of the previously displayed model from display
                if (o != null) {
                    int previousModel = Integer.parseInt(o.substring(6).strip()) - 1;
                    balls.getChildren().get(previousModel).setVisible(false);
                    sticks.getChildren().get(previousModel).setVisible(false);
                    if (!ribbons.getChildren().isEmpty())
                        ribbons.getChildren().get(previousModel).setVisible(false);
                    selectionModel.getSelectedItems().clear();
                }

                // Set selected model visible
                balls.getChildren().get(selectedModel).setVisible(true);
                sticks.getChildren().get(selectedModel).setVisible(true);
                if (!ribbons.getChildren().isEmpty())
                    ribbons.getChildren().get(selectedModel).setVisible(true);

                // also update sequence in case different models have different sequence
                controller.getSequenceTextFlow().getChildren().clear();
                controller.getSequenceTextFlow().getChildren().add(sequences.get(selectedModel));
            }
        });

        // Setup chain selection from ListView in Chains Tab
        controller.getChainListView().setCellFactory(CheckBoxListCell.
                forListView(CheckBoxListViewItem::onProperty));
    }

    /**
     * Sets up the functionality of the button bar items.
     */
    private void setToolBarFunctionality(Stage stage) {
        // Open and Save files
        controller.getLoadButton().setOnAction(e -> openFile(stage));
        controller.getSaveButton().setOnAction(e -> saveFile(stage));
        controller.getSaveButton().disableProperty().bind(Bindings.isEmpty(balls.getChildren()));

        // Undo and Redo button function
        controller.getUndoButton().setOnAction(e -> undoManager.undo());
        controller.getUndoButton().textProperty().bind(undoManager.undoLabelProperty());
        controller.getUndoButton().disableProperty().bind(undoManager.canUndoProperty().not());

        controller.getRedoButton().setOnAction(e -> undoManager.redo());
        controller.getRedoButton().textProperty().bind(undoManager.redoLabelProperty());
        controller.getRedoButton().disableProperty().bind(undoManager.canRedoProperty().not());

        // Copy button
        controller.getCopyButton().setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putImage(controller.getMainPane().snapshot(null, null));
            Clipboard.getSystemClipboard().setContent(content);
        });
        controller.getCopyButton().disableProperty().bind(Bindings.isEmpty(balls.getChildren()));

        // Fill the color scheme choice box
        ObservableList<String> colorSchemes = FXCollections.observableArrayList("atom", "residue",
                "sec. structure", "molecule");
        controller.getColorSchemeChoiceBox().setItems(colorSchemes);
        controller.getColorSchemeChoiceBox().setValue("atom");

        controller.getColorSchemeChoiceBox().getSelectionModel().selectedIndexProperty().addListener((v, o, n) -> {
            switch ((int) n) { // Set Color Choice in Menu
                case 0 -> controller.getMenuColAtom().setSelected(true);
                case 1 -> controller.getMenuColResidue().setSelected(true);
                case 2 -> controller.getMenuColSecStruc().setSelected(true);
                case 3 -> controller.getMenuColMolecule().setSelected(true);
            }
            // Compute new colors
            ComplexFigure.setAtomColor(model, atomToSphere, (int) n);

            // Reset legend
            WebEngine webEngine = controller.getLegendWV().getEngine();
            webEngine.loadContent(ComplexFigure.generateLegendContent((int) n, model.getChains()));
        });

        // Give functionality to Checkboxes in ButtonBar
        balls.visibleProperty().bindBidirectional(controller.getAtomsCB().selectedProperty());
        sticks.visibleProperty().bindBidirectional(controller.getBondsCB().selectedProperty());
        ribbons.visibleProperty().bindBidirectional(controller.getRibbonsCB().selectedProperty());

        // Let Ribbons be computed on first selection
        controller.getRibbonsCB().selectedProperty().addListener((v, o, n) -> computeRibbons());

        // Give functionality to zoom in/out buttons
        controller.getZoomInButton().setOnAction(e -> zoomIn());
        controller.getZoomOutButton().setOnAction(e -> zoomOut());

        // Give functionality to Explode button
        controller.getExplodeButton().setOnAction(e -> runExplodeAnimation());

        // Give functionality to Jiggle button
        controller.getJiggleButton().setOnAction(e -> runJiggleAnimation());

        // Bindings of disableProperties of ToolBar
        controller.getAtomsCB().disableProperty().bind(Bindings.isEmpty(balls.getChildren()));
        controller.getBondsCB().disableProperty().bind(Bindings.isEmpty(sticks.getChildren()));
        controller.getAtomsSlider().disableProperty().bind(controller.getAtomsCB().selectedProperty().not()
                .or(Bindings.isEmpty(balls.getChildren())));
        controller.getBondsSlider().disableProperty().bind(controller.getBondsCB().selectedProperty().not()
                .or(Bindings.isEmpty(sticks.getChildren())));
        controller.getZoomInButton().disableProperty().bind((balls.visibleProperty().or(sticks.visibleProperty())
                .or(ribbons.visibleProperty())).not().or(Bindings.isEmpty(balls.getChildren())));
        controller.getZoomOutButton().disableProperty().bind((balls.visibleProperty().or(sticks.visibleProperty())
                .or(ribbons.visibleProperty())).not().or(Bindings.isEmpty(balls.getChildren())));
        controller.getColorSchemeChoiceBox().disableProperty().bind(balls.visibleProperty().not()
                .or(Bindings.isEmpty(balls.getChildren())));
        controller.getRibbonsCB().disableProperty().bind(Bindings.isEmpty(balls.getChildren()));
        controller.getExplodeButton().disableProperty().bind((balls.visibleProperty().or(sticks.visibleProperty())
                .or(ribbons.visibleProperty())).not().or(Bindings.isEmpty(balls.getChildren())
                .or(Bindings.size(chains).isEqualTo(1))));
        controller.getJiggleButton().disableProperty().bind(balls.visibleProperty().not()
                .or(Bindings.isEmpty(balls.getChildren()).or(numberOfModels.isEqualTo(0))));
    }

    /**
     * Sets all functionality of the menu items.
     * @param stage (Stage): the main stage
     */
    private void setMenuFunctionality(Stage stage) {
        // File menu
        controller.getMenuClose().setOnAction(e -> Platform.exit()); // close program
        controller.getMenuOpen().setOnAction(e -> openFile(stage)); // open file from file system
        controller.getMenuSave().setOnAction(e -> saveFile(stage));// save a PDB file
        controller.getMenuSave().disableProperty().bind(Bindings.isEmpty(balls.getChildren()));

        // Edit menu
        // Undo/Redo
        controller.getMenuUndo().setOnAction(e -> undoManager.undo());
        controller.getMenuUndo().textProperty().bind(undoManager.undoLabelProperty());
        controller.getMenuUndo().disableProperty().bind(undoManager.canUndoProperty().not());

        controller.getMenuRedo().setOnAction(e -> undoManager.redo());
        controller.getMenuRedo().textProperty().bind(undoManager.redoLabelProperty());
        controller.getMenuRedo().disableProperty().bind(undoManager.canRedoProperty().not());

        setupUndoRedoFunctionality();

        // Copy image to clipboard
        controller.getMenuCopy().setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putImage(controller.getMainPane().snapshot(null, null));
            Clipboard.getSystemClipboard().setContent(content);
        });
        controller.getMenuCopy().disableProperty().bind(Bindings.isEmpty(balls.getChildren()));

        // View Menu
        // Give functionality to Checkboxes in MenuBar
        balls.visibleProperty().bindBidirectional(controller.getMenuShowBalls().selectedProperty());
        sticks.visibleProperty().bindBidirectional(controller.getMenuShowSticks().selectedProperty());
        ribbons.visibleProperty().bindBidirectional(controller.getMenuShowRibbons().selectedProperty());
        controller.getMenuShowBalls().disableProperty().bind(Bindings.isEmpty(balls.getChildren()));
        controller.getMenuShowSticks().disableProperty().bind(Bindings.isEmpty(balls.getChildren()));
        controller.getMenuShowRibbons().disableProperty().bind(Bindings.isEmpty(balls.getChildren()));
        // Let Ribbons be computed on first selection
        controller.getMenuShowRibbons().selectedProperty().addListener((v, o, n) -> computeRibbons());

        // Create and set toggle group for RadioMenuItems
        ToggleGroup toggleGroup = new ToggleGroup();
        controller.getMenuColAtom().setToggleGroup(toggleGroup);
        controller.getMenuColResidue().setToggleGroup(toggleGroup);
        controller.getMenuColSecStruc().setToggleGroup(toggleGroup);
        controller.getMenuColMolecule().setToggleGroup(toggleGroup);
        // Functionality for color scheme
        toggleGroup.selectedToggleProperty().addListener((v, o, n) -> {
            String newColor = n.toString().substring(24);
            if (newColor.startsWith("Atom"))
                controller.getColorSchemeChoiceBox().getSelectionModel().select(0);
            else if (newColor.startsWith("Residue"))
                controller.getColorSchemeChoiceBox().getSelectionModel().select(1);
            else if (newColor.startsWith("SecStruc"))
                controller.getColorSchemeChoiceBox().getSelectionModel().select(2);
            else
                controller.getColorSchemeChoiceBox().getSelectionModel().select(3);
        });
        controller.getColorByMenu().disableProperty().bind(Bindings.isEmpty(balls.getChildren())
                .or(balls.visibleProperty().not()));

        // Give functionality to Explode and Jiggle menu items
        controller.getExplodeMenu().setOnAction(e -> runExplodeAnimation());
        controller.getExplodeMenu().disableProperty().bind((balls.visibleProperty().or(sticks.visibleProperty())
                .or(ribbons.visibleProperty())).not().or((balls.visibleProperty().or(sticks.visibleProperty())
                .or(ribbons.visibleProperty())).not().or(Bindings.isEmpty(balls.getChildren())
                .or(Bindings.size(chains).isEqualTo(1)))));

        controller.getJiggleMenu().setOnAction(e -> runJiggleAnimation());
        controller.getJiggleMenu().disableProperty().bind(balls.visibleProperty().not()
                .or(Bindings.isEmpty(balls.getChildren()).or(numberOfModels.isEqualTo(0))));

        // Full Screen
        controller.getMenuFullScreen().setOnAction(e -> stage.setFullScreen(!stage.isFullScreen()));
        // Dark Mode
        controller.getMenuDarkMode().selectedProperty().addListener((v, o, n) -> {
            URL stylesURL = getClass().getResource("modena_dark.css");
            if (stylesURL != null) {
                if (n) {
                    stage.getScene().getStylesheets().add(stylesURL.toExternalForm());

                    URL webviewURL = getClass().getResource("webview_dark.css");
                    if (webviewURL != null)
                        controller.getLegendWV().getEngine().setUserStyleSheetLocation(webviewURL.toExternalForm());
                }
                else {
                    stage.getScene().getStylesheets().remove(stylesURL.toExternalForm());

                    URL webviewURL = getClass().getResource("webview_light.css");
                    if (webviewURL != null)
                        controller.getLegendWV().getEngine().setUserStyleSheetLocation(webviewURL.toExternalForm());
                }
            }
        });

        // Help menu
        controller.getMenuHelp().setOnAction(e -> { // Help window
            generateHelpMenu();
        });
        controller.getMenuAbout().setOnAction(e -> { // About window
            // setup new stage for About dialog
            final Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL); // needs to be exited before going back to main window

            // Add About information to the dialog
            BorderPane borderPane = new BorderPane();
            borderPane.setCenter(new Label(
                    """
                            PDBExplorer 1.0.0
                            
                            This program can be used to explore a 3D structure from the PDB
                            database. Structures can be saved to the file system as PDB files
                            and reopened later.
                                                        
                            Created by Julia Fischer 7.2023
                            """
            ));
            borderPane.setStyle("-fx-background-color: white;");

            // Add close button to the dialog
            Button closeAbout = new Button("Close");
            closeAbout.setOnAction(event -> dialog.close());

            BorderPane.setAlignment(closeAbout, Pos.BOTTOM_RIGHT);
            borderPane.setBottom(closeAbout);

            // Setup scene
            Scene aboutScene = new Scene(borderPane, 350, 150);
            dialog.setScene(aboutScene);
            dialog.setResizable(false); // make popup window not resizable
            dialog.setTitle("About");
            dialog.show();
        });
    }

    /**
     * Sets up the undo and redo functionalities for certain actions.
     */
    private void setupUndoRedoFunctionality() {
        controller.getAtomsSlider().valueProperty().addListener((v, o, n) ->
                undoManager.add(new PropertyCommand<>("atom radius", (DoubleProperty) v, o, n)));
        controller.getBondsSlider().valueProperty().addListener((v, o, n) ->
                undoManager.add(new PropertyCommand<>("bond width", (DoubleProperty) v, o, n)));
        controller.getRibbonsCB().selectedProperty().addListener((v, o, n) ->
                undoManager.add(new PropertyCommand<>("ribbons", (BooleanProperty) v, o, n)));
        controller.getAtomsCB().selectedProperty().addListener((v, o, n) ->
                undoManager.add(new PropertyCommand<>("atoms", (BooleanProperty) v, o, n)));
        controller.getBondsCB().selectedProperty().addListener((v, o, n) ->
                undoManager.add(new PropertyCommand<>("bonds", (BooleanProperty) v, o, n)));
        camera.translateZProperty().addListener((v, o, n) ->
                undoManager.add(new PropertyCommand<>("zoom", (DoubleProperty) v, o, n)));
        controller.getColorSchemeChoiceBox().valueProperty().addListener((v, o, n) ->
                undoManager.add(new PropertyCommand<>("coloring", (ObjectProperty<String>) v, o, n)));
    }

    /**
     * Sets up the services that call upon the Tasks of the PDBWebClient.
     * The first service is started once when launching the GUI. This will get a list of all PDB files listed in the
     * database. The list is incorporated into the GUI in a ListView as a FilteredList. The FilteredList can also be
     * used to search for specific PDB files using the specified TextField.
     * This function also handles the ProgressBar as well as bindings related to the service.
     */
    private void setupWebClientService(Stage stage) {
        // Setup service that calls upon the PDBWebClient task for getting the list of pdb files
        Service<ArrayList<String>> serviceWebClientGetList = new Service<>() {
            @Override
            protected Task<ArrayList<String>> createTask() {
                return new PDBWebClient.GetList();
            }
        };
        if (inputList == null)
            serviceWebClientGetList.restart(); // start service at launch of GUI
        // When WebClient starts, set info label
        serviceWebClientGetList.setOnRunning((WorkerStateEvent v) ->
                controller.getInfoLabel().setText("Loading list of PDB files."));
        // In case of failure:
        serviceWebClientGetList.setOnFailed((WorkerStateEvent v) -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, "List of PDB files could not be loaded. Trying again.");
            alert.show();
            // in case the initial loading fails: try again
            if (inputList == null)
                serviceWebClientGetList.restart();
        });
        // In case the service succeeds:
        serviceWebClientGetList.setOnSucceeded((WorkerStateEvent v) -> {
            // get return value from service
            ArrayList<String> input = serviceWebClientGetList.getValue();

            // turn into FilteredList
            inputList = new FilteredList<>(
                    FXCollections.observableArrayList(input), s -> true);

            // set functionality for filtered list to be able to search for specific PDB files
            controller.getPdbSearchTF().textProperty().addListener(o -> {
                inSearch.set(true);
                String filter = controller.getPdbSearchTF().getText().toUpperCase();
                inputList.setPredicate(filter.isEmpty() ? s -> true : s -> s.contains(filter));
                inSearch.set(false);
            });

            // Add filtered list as items of ListView in GUI
            controller.getPdbEntryListView().setItems(inputList);

            // Reset info label
            controller.getInfoLabel().setText("");
        });
        // bind visibility and progress of ProgressBar to the service (only show in case of filling the PDB entry list)
        controller.getPdbEntriesProgress().visibleProperty().bind(serviceWebClientGetList.runningProperty());
        controller.getPdbEntriesProgress().progressProperty().bind(serviceWebClientGetList.progressProperty());

        // Setup service that calls upon the PDBWebClient task for getting a pdb file
        Service<String> serviceWebClientGetPDB = new Service<>() {
            @Override
            protected Task<String> createTask() {
                return new PDBWebClient.GetPDBFile(controller.getPdbEntryListView().getSelectionModel()
                        .getSelectedItem());
            }
        };
        // In case of failure: (fails, e.g. for 8ouc -> file not found error)
        serviceWebClientGetPDB.setOnFailed((WorkerStateEvent v) -> {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "This file could not be downloaded from the PDB website! This could either be due to network problems or the file not being present on the PDB server. Please select another file or try again later.");
            alert.show();
        });
        // In case the service succeeds:
        serviceWebClientGetPDB.setOnSucceeded((WorkerStateEvent v) -> {
            // get return value from service
            pdbFileContent = serviceWebClientGetPDB.getValue();

            // Get name of PDB file
            pdbFileName = controller.getPdbEntryListView().getSelectionModel().getSelectedItem();

            // Parse PDB
            setupParserService(pdbFileContent, stage, true);

            // Add PDB file content to PDB File Tab
            controller.getPdbFileTA().setText(pdbFileContent);
        });
        // Setup file selection from ListView
        controller.getPdbEntryListView().getSelectionModel().selectedItemProperty().addListener((v, o, n) -> {
            // only start service if a pdb file gets selected not if one gets unselected!
            if (controller.getPdbEntryListView().getSelectionModel().getSelectedIndex() != -1 && !inSearch.getValue())
                serviceWebClientGetPDB.restart();
        });

        // bind ListView properties to services
        controller.getPdbEntryListView().disableProperty().bind(serviceWebClientGetList.runningProperty()
                .or(serviceWebClientGetPDB.runningProperty()));
        controller.getPdbSearchTF().disableProperty().bind(serviceWebClientGetList.runningProperty()
                .or(serviceWebClientGetPDB.runningProperty()));
    }

    /**
     * Sets up the mouse actions that can be done on the main pane. These include rotation of the molecule when pressing
     * the mouse and zooming in and out by scrolling.
     */
    private void setupPaneMouseAction() {
        controller.getMainPane().setOnMousePressed(e -> {
            // Stop rotation animation (if ongoing)
            if (rotateAnimation != null) {
                rotateAnimation.stop();
            }

            xPrev = e.getSceneX();
            yPrev = e.getSceneY();
        });

        outerGroup.getTransforms().add(new Rotate());

        controller.getMainPane().setOnMouseDragged(e -> {
            if ((!balls.isVisible() && !sticks.isVisible() && !ribbons.isVisible()) || balls.getChildren().isEmpty())
                return;

            // Stop rotation animation (if ongoing)
            if (rotateAnimation != null) {
                rotateAnimation.stop();
            }

            double dx = e.getSceneX() - xPrev;
            double dy = e.getSceneY() - yPrev;

            final Point3D orthogonalAxis = new Point3D(dy, -dx, 0);

            Rotate rotate = new Rotate(0.25 * orthogonalAxis.magnitude(), orthogonalAxis);
            Transform oldTransform = outerGroup.getTransforms().get(0);
            Transform newTransform = rotate.createConcatenation(oldTransform);
            outerGroup.getTransforms().set(0, newTransform);
            xPrev = e.getSceneX();
            yPrev = e.getSceneY();

            if (e.isShiftDown()) {
                rotateAnimation = AnimationHandler.rotationAnimation(outerGroup, rotate);
                rotateAnimation.play();
            }
        });

        // Setup zoom in/out by mouse
        controller.getMainPane().setOnScroll(e -> {
            if ((!balls.isVisible() && !sticks.isVisible() && !ribbons.isVisible()) || balls.getChildren().isEmpty())
                return;
            var delta = e.getDeltaY();
            if (delta > 0)
                zoomIn();
            else if (delta < 0)
                zoomOut();
        });
    }

    /**
     * Zoom in functionality.
     */
    private void zoomIn() {
        camera.setTranslateZ(1 / 1.1 * camera.getTranslateZ());
    }

    /**
     * Zoom out functionality.
     */
    private void zoomOut() {
        camera.setTranslateZ(1.1 * camera.getTranslateZ());
    }

    /**
     * Opens a FileChooser to open a file from the file system.
     * @param stage (Stage): the main stage
     */
    private void openFile(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDB file (*.pdb)", "*.pdb"));
        File file = chooser.showOpenDialog(stage);

        if (file != null) {
            try {
                String filePath = file.getAbsolutePath();

                // Read and parse PDB file
                setupParserService(filePath, stage, false);

                // Update pdb file name
                pdbFileName = filePath.substring(filePath.length() - 8, filePath.length() - 4).toUpperCase();

                // Add PDB file content to PDB File Tab
                controller.getPdbFileTA().setText(pdbFileContent);

                // Deselect PDB file from ListView
                controller.getPdbEntryListView().getSelectionModel().clearSelection();
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "The given file could not be opened!");
                alert.show();

                controller.getPdbFileTA().setText("");
            }
        }
    }

    /**
     * Sets up the service needed to run the task PDBParser.ParsePDB.
     * @param input (String): the content of the PDB file in String format or the path to the PDB file
     * @param stage (Stage): the main stage
     * @param parse (boolean): whether the parse or read method should be called
     */
    private void setupParserService(String input, Stage stage, boolean parse) {
        if (parse) {
            // Create service to compute nodes for the new model
            Service<PDBComplex> serviceParser = new Service<>() {
                @Override
                protected Task<PDBComplex> createTask() {
                    return new PDBParser.ParsePDB(input);
                }
            };
            // In case of failure:
            serviceParser.setOnFailed((WorkerStateEvent event) -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "This PDB file is corrupted and could not be parsed. Please select another PDB file.");
                alert.show();
            });
            serviceParser.setOnSucceeded((WorkerStateEvent event) -> {
                this.model = serviceParser.getValue(); // retrieve parsed model

                // Update display of molecule
                updateMoleculeOnPane(stage);
            });
            // bind visibility and progress of ProgressBar to the service (only show in case of loading PDB file)
            controller.getGeneralProgress().visibleProperty().bind(serviceParser.runningProperty());
            controller.getGeneralProgress().progressProperty().bind(serviceParser.progressProperty());
            serviceParser.restart(); // Start parsing
        } else {
            // Create service to compute nodes for the new model
            Service<String> serviceParser = new Service<>() {
                @Override
                protected Task<String> createTask() {
                    return new PDBParser.ReadPDB(input);
                }
            };
            // In case of failure:
            serviceParser.setOnFailed((WorkerStateEvent event) -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "This PDB file is corrupted and could not be read. Please select another PDB file.");
                alert.show();
            });
            serviceParser.setOnSucceeded((WorkerStateEvent event) -> {
                pdbFileContent = serviceParser.getValue(); // retrieve parsed model

                // Parse the PDB file
                setupParserService(pdbFileContent, stage, true);
            });
            // bind visibility and progress of ProgressBar to the service (only show in case of loading PDB file)
            controller.getGeneralProgress().visibleProperty().bind(serviceParser.runningProperty());
            controller.getGeneralProgress().progressProperty().bind(serviceParser.progressProperty());
            serviceParser.restart(); // Start reading
        }
    }

    /**
     * Opens a FileChooser to save the displayed PDB file to the file system.
     *
     * @param stage (Stage): the main stage
     */
    private void saveFile(Stage stage) {
        FileChooser chooser = new FileChooser();
        // Add filter such that files are automatically saved as .pdb
        FileChooser.ExtensionFilter pdbFilter = new FileChooser.ExtensionFilter("PDB File (*.pdb)", "*.pdb");
        chooser.getExtensionFilters().add(pdbFilter);

        // Preset the file name to the name of the pdb file
        chooser.setInitialFileName(pdbFileName);

        File file = chooser.showSaveDialog(stage);

        if (file != null) {
            try {
                PrintWriter writer;
                writer = new PrintWriter(file);
                writer.println(pdbFileContent);
                writer.close();
            } catch (IOException ex) {
                System.out.println(Arrays.toString(ex.getStackTrace()));
            }
        }
    }

    /**
     * Computes ribbons, when the Checkbox or MenuItem are clicked for the first time for a molecule.
     */
    private void computeRibbons() {
        if (ribbons.getChildren().isEmpty() && controller.getRibbonsCB().isSelected()) {
            // bind visibility and progress of ProgressBar to the service (only show in case of loading ribbons)
            controller.getGeneralProgress().visibleProperty().bind(serviceRibbon.runningProperty());
            controller.getGeneralProgress().progressProperty().bind(serviceRibbon.progressProperty());
            // restart ribbons service to compute ribbons
            serviceRibbon.restart();
        }
    }

    /**
     * Runs the jiggle animation.
     */
    private void runJiggleAnimation() {
        Timeline jiggleAnimation = AnimationHandler.jiggleAnimation(balls, model.getNumberOfModels(),
                controller.getModelListView().getSelectionModel().getSelectedIndex());

        if (jiggleAnimation != null) {
            controller.getModelsTab().setDisable(true);
            controller.getBondsCB().setSelected(false); // only works for atoms
            controller.getRibbonsCB().setSelected(false); // only works for atoms

            jiggleAnimation.play();
            controller.getJiggleMenu().disableProperty().bind(balls.visibleProperty().not()
                    .or(Bindings.isEmpty(balls.getChildren())
                            .or(Bindings.equal(Animation.Status.RUNNING, jiggleAnimation.statusProperty()))
                    .or(numberOfModels.isEqualTo(0))));
            controller.getJiggleButton().disableProperty().bind(balls.visibleProperty().not()
                    .or(Bindings.isEmpty(balls.getChildren())
                    .or(Bindings.equal(Animation.Status.RUNNING, jiggleAnimation.statusProperty()))
                    .or(numberOfModels.isEqualTo(0))));

            jiggleAnimation.setOnFinished(event -> controller.getModelsTab().setDisable(false));
        }
    }

    /**
     * Runs the explode-animation.
     */
    private void runExplodeAnimation() {
        Timeline explodeAnimation = AnimationHandler.explodeAnimation(outerGroup, model.getChains(),
                model.getNumberOfModels());

        if (explodeAnimation != null) {
            explodeAnimation.play();
            controller.getExplodeButton().disableProperty().bind((balls.visibleProperty().or(sticks.visibleProperty())
                    .or(ribbons.visibleProperty())).not().or(Bindings.isEmpty(balls.getChildren())
                    .or(Bindings.equal(Animation.Status.RUNNING, explodeAnimation.statusProperty())
                            .or(Bindings.size(chains).isEqualTo(1)))));
            controller.getExplodeMenu().disableProperty().bind((balls.visibleProperty().or(sticks.visibleProperty())
                    .or(ribbons.visibleProperty())).not().or(Bindings.isEmpty(balls.getChildren())
                    .or(Bindings.equal(Animation.Status.RUNNING, explodeAnimation.statusProperty())
                            .or(Bindings.size(chains).isEqualTo(1)))));
        }
    }

    /**
     * Generates the Help Window, which displays an HTML-formatted help page in a WebView. The HTML code was generated
     * with the help of ChatGPT.
     */
    private void generateHelpMenu() {
        // setup new stage for Help dialog
        final Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL); // needs to be exited before going back to main window

        // Generate WebView to show help page in
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        webEngine.loadContent("""
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <title>PDBViewer Help Page</title>
                        <style>
                            body {
                                font-family: Arial, sans-serif;
                                line-height: 1.6;
                                max-width: 800px;
                                margin: 0 auto;
                                padding: 20px;
                            }
                            h1 {
                                color: #007bff;
                            }
                            h2 {
                                color: #009688;
                            }
                            p {
                                margin-bottom: 10px;
                            }
                            code {
                                background-color: #f0f0f0;
                                padding: 2px 5px;
                                border-radius: 3px;
                            }
                        </style>
                    </head>
                    <body>
                        <h1>PDBViewer Help Page</h1>
                        <p>Welcome to the help page of the PDBViewer! Below, you'll find useful information for using the application.</p>
                                        
                        <h2>Getting Started</h2>
                        <p>Follow these steps to get started:</p>
                        <ol>
                            <li>Search for a PDB file in the list on the left.</li>
                            <li>Load a PDB file by clicking on it.</li>
                            <li>Explore the contained protein structure.</li>
                        </ol>
                        <p>Alternatively, you can also load a PDB file from the file system. Beware that only proteins are displayed.</p>
                                        
                        <h2>General Use</h2>
                        <p>In addition to the list of PDB files to choose from, there are two tabs on the left where you can choose from the different
                        models and/or chains if applicable. If a PDB file does not contain several models or chains, the tabs are disabled accordingly.</p>
                        
                        <p>The main part of the program consists of three views:</p>
                        
                        <h3>1. Protein View</h3>
                        <p>The protein contained in the PDB file is displayed here. There are several features available:</p>
                        
                        <ul>
                            <li>Atoms, Bonds and Ribbons can be individually turned on or off. Atoms and Bonds can further be changed in size.</li>
                            <li>Atom colors can be adjusted to be colored by atom, residue (following the Lesk scheme), secondary structure or chain.</li>
                            <li>Zoom is possible using the zoom buttons or menu items as well as the scrolling gesture.</li>
                            <li>The model can be rotated by dragging it. If rotated while pressing shift, the molecule will continue to rotate.</li>
                            <li>Residues can be selected by pressing on them. Multiple selection is possible by using shift while pressing. The Deselect all-button can be used to clear the whole selection</li>
                            <li>There are two animations available, which will be explained further down below.</li>
                        </ul>
                        
                        <h3>2. PDB File</h3>
                        <p>This view presents the chosen PDB file. Information and remarks about the protein can be looked up here.</p>
                        
                        <h3>3. Stats</h3>
                        <p>Here, some plots that summarize statistics about the displayed protein are displayed:</p>
                        
                        <ol>
                            <li>PieChart showing the residue composition of the protein.</li>
                            <li>PieChart showing the secondary structure composition of the protein.</li>
                            <li>PieChart showing the distribution of amino acid properties in the protein.</li>
                            <li>Ramachandran Plot showing the phi and psi angles of the backbone. It is only computed for the first model in case several models are available.</li>
                        </ol>
                        
                        <p>The Pie Charts will update based on the selected amino acids.</p>
                        
                        <h2>Animations</h2>
                        <p><strong>Jiggle</strong>:</p>
                        <ul>
                            <li>Jiggles through the different models of a protein if present.</li>
                            <li>Only applicable for atoms, bonds and ribbons are automatically turned off at start.</li>
                            <li>As only a heuristic is used for the computation of bonds, these cannot be jiggled through.</li>
                        </ul>
                        <p><strong>Explode</strong>:</p>
                        <ul>
                            <li>Explodes the different chains of a protein if present.</li>
                            <li>The chains drift away from each other, pause and then go back to their starting positions.</li>
                        </ul>
                        <p>Jiggle and Explode can be applied simultaneously.</p>
                    </body>
                    </html>""");


        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(webView);

        Scene helpScene = new Scene(borderPane, 500, 600);
        dialog.setScene(helpScene);
        dialog.setTitle("Help");
        dialog.show();
    }
}
