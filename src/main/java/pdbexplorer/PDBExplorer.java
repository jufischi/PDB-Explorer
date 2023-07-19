package pdbexplorer;

import pdbexplorer.model.protein.PDBComplex;
import pdbexplorer.window.WindowPresenter;
import pdbexplorer.window.WindowView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main application of the PDBExplorer. Run this class to run the program.
 */
public class PDBExplorer extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        WindowView view = new WindowView();
        PDBComplex model = new PDBComplex();
        WindowPresenter presenter = new WindowPresenter(stage, view, model);

        stage.setScene(new Scene(view.getRoot()));
        stage.setTitle("PDB Explorer");
        stage.show();
    }
}
