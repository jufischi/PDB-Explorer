package pdbexplorer.window;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.util.Objects;

/**
 * This class implements the View part of the ModelViewPresenter programming pattern.
 */
public class WindowView {
    private final Parent root;
    private final pdbexplorer.window.WindowController controller;

    /**
     * Constructor that reads in the SceneGraph from a .fxml file.
     */
    public WindowView() throws IOException {
        try (var ins = Objects.requireNonNull(getClass().getResource("Window.fxml")).openStream()) {
            var fxmlLoader = new FXMLLoader();
            fxmlLoader.load(ins);

            controller = fxmlLoader.getController();
            root = fxmlLoader.getRoot();
        }
    }

    /**
     * Getter for the root of the SceneGraph
     *
     * @return root of the SceneGraph
     */
    public Parent getRoot() {
        return root;
    }

    /**
     * Getter for the controller of the UI
     *
     * @return controller of the UI
     */
    public WindowController getController() {
        return controller;
    }
}
