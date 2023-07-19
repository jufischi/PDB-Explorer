package pdbexplorer.window;

import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.ScatterChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Pane;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebView;
import pdbexplorer.model.CheckBoxListViewItem;

public class WindowController {

    @FXML
    private CheckBox atomsCB;

    @FXML
    private Slider atomsSlider;

    @FXML
    private CheckBox bondsCB;

    @FXML
    private Slider bondsSlider;

    @FXML
    private ListView<CheckBoxListViewItem> chainListView;

    @FXML
    private Tab chainsTab;

    @FXML
    private ScrollPane chartsScrollPane;

    @FXML
    private Menu colorByMenu;

    @FXML
    private ChoiceBox<String> colorSchemeChoiceBox;

    @FXML
    private Button copyButton;

    @FXML
    private Button deselectButton;

    @FXML
    private Button explodeButton;

    @FXML
    private MenuItem explodeMenu;

    @FXML
    private ProgressBar generalProgress;

    @FXML
    private Label infoLabel;

    @FXML
    private Button jiggleButton;

    @FXML
    private MenuItem jiggleMenu;

    @FXML
    private TitledPane legendTP;

    @FXML
    private WebView legendWV;

    @FXML
    private Button loadButton;

    @FXML
    private Pane mainPane;

    @FXML
    private MenuItem menuAbout;

    @FXML
    private MenuItem menuClose;

    @FXML
    private RadioMenuItem menuColAtom;

    @FXML
    private RadioMenuItem menuColMolecule;

    @FXML
    private RadioMenuItem menuColResidue;

    @FXML
    private RadioMenuItem menuColSecStruc;

    @FXML
    private MenuItem menuCopy;

    @FXML
    private CheckMenuItem menuDarkMode;

    @FXML
    private MenuItem menuFullScreen;

    @FXML
    private MenuItem menuHelp;

    @FXML
    private MenuItem menuOpen;

    @FXML
    private MenuItem menuRedo;

    @FXML
    private MenuItem menuSave;

    @FXML
    private CheckMenuItem menuShowBalls;

    @FXML
    private CheckMenuItem menuShowRibbons;

    @FXML
    private CheckMenuItem menuShowSticks;

    @FXML
    private MenuItem menuUndo;

    @FXML
    private ListView<String> modelListView;

    @FXML
    private Tab modelsTab;

    @FXML
    private ProgressBar pdbEntriesProgress;

    @FXML
    private ListView<String> pdbEntryListView;

    @FXML
    private TextArea pdbFileTA;

    @FXML
    private TextField pdbSearchTF;

    @FXML
    private PieChart propertiesPC;

    @FXML
    private ScatterChart<Number, Number> ramachandranPlot;

    @FXML
    private Button redoButton;

    @FXML
    private PieChart residuePieChart;

    @FXML
    private CheckBox ribbonsCB;

    @FXML
    private Button saveButton;

    @FXML
    private PieChart secStrucPieChart;

    @FXML
    private ScrollPane sequenceScrollBar;

    @FXML
    private TextFlow sequenceTextFlow;

    @FXML
    private Tab statsTab;

    @FXML
    private Button undoButton;

    @FXML
    private Button zoomInButton;

    @FXML
    private Button zoomOutButton;

    public CheckBox getAtomsCB() {
        return atomsCB;
    }

    public Slider getAtomsSlider() {
        return atomsSlider;
    }

    public CheckBox getBondsCB() {
        return bondsCB;
    }

    public Slider getBondsSlider() {
        return bondsSlider;
    }

    public ListView<CheckBoxListViewItem> getChainListView() {
        return chainListView;
    }

    public Tab getChainsTab() {
        return chainsTab;
    }

    public ScrollPane getChartsScrollPane() {
        return chartsScrollPane;
    }

    public Menu getColorByMenu() {
        return colorByMenu;
    }

    public ChoiceBox<String> getColorSchemeChoiceBox() {
        return colorSchemeChoiceBox;
    }

    public Button getCopyButton() {
        return copyButton;
    }

    public Button getDeselectButton() {
        return deselectButton;
    }

    public Button getExplodeButton() {
        return explodeButton;
    }

    public MenuItem getExplodeMenu() {
        return explodeMenu;
    }

    public ProgressBar getGeneralProgress() {
        return generalProgress;
    }

    public Label getInfoLabel() {
        return infoLabel;
    }

    public Button getJiggleButton() {
        return jiggleButton;
    }

    public MenuItem getJiggleMenu() {
        return jiggleMenu;
    }

    public TitledPane getLegendTP() {
        return legendTP;
    }

    public WebView getLegendWV() {
        return legendWV;
    }

    public Button getLoadButton() {
        return loadButton;
    }

    public Pane getMainPane() {
        return mainPane;
    }

    public MenuItem getMenuAbout() {
        return menuAbout;
    }

    public MenuItem getMenuClose() {
        return menuClose;
    }

    public RadioMenuItem getMenuColAtom() {
        return menuColAtom;
    }

    public RadioMenuItem getMenuColMolecule() {
        return menuColMolecule;
    }

    public RadioMenuItem getMenuColResidue() {
        return menuColResidue;
    }

    public RadioMenuItem getMenuColSecStruc() {
        return menuColSecStruc;
    }

    public MenuItem getMenuCopy() {
        return menuCopy;
    }

    public CheckMenuItem getMenuDarkMode() {
        return menuDarkMode;
    }

    public MenuItem getMenuFullScreen() {
        return menuFullScreen;
    }

    public MenuItem getMenuHelp() {
        return menuHelp;
    }

    public MenuItem getMenuOpen() {
        return menuOpen;
    }

    public MenuItem getMenuRedo() {
        return menuRedo;
    }

    public MenuItem getMenuSave() {
        return menuSave;
    }

    public CheckMenuItem getMenuShowBalls() {
        return menuShowBalls;
    }

    public CheckMenuItem getMenuShowRibbons() {
        return menuShowRibbons;
    }

    public CheckMenuItem getMenuShowSticks() {
        return menuShowSticks;
    }

    public MenuItem getMenuUndo() {
        return menuUndo;
    }

    public ListView<String> getModelListView() {
        return modelListView;
    }

    public Tab getModelsTab() {
        return modelsTab;
    }

    public ProgressBar getPdbEntriesProgress() {
        return pdbEntriesProgress;
    }

    public ListView<String> getPdbEntryListView() {
        return pdbEntryListView;
    }

    public TextArea getPdbFileTA() {
        return pdbFileTA;
    }

    public TextField getPdbSearchTF() {
        return pdbSearchTF;
    }

    public PieChart getPropertiesPC() {
        return propertiesPC;
    }

    public ScatterChart<Number, Number> getRamachandranPlot() {
        return ramachandranPlot;
    }

    public Button getRedoButton() {
        return redoButton;
    }

    public PieChart getResiduePieChart() {
        return residuePieChart;
    }

    public CheckBox getRibbonsCB() {
        return ribbonsCB;
    }

    public Button getSaveButton() {
        return saveButton;
    }

    public PieChart getSecStrucPieChart() {
        return secStrucPieChart;
    }

    public ScrollPane getSequenceScrollBar() {
        return sequenceScrollBar;
    }

    public TextFlow getSequenceTextFlow() {
        return sequenceTextFlow;
    }

    public Tab getStatsTab() {
        return statsTab;
    }

    public Button getUndoButton() {
        return undoButton;
    }

    public Button getZoomInButton() {
        return zoomInButton;
    }

    public Button getZoomOutButton() {
        return zoomOutButton;
    }
}
