module com.example.pdbexplorer {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.json;
    requires javafx.web;

    exports pdbexplorer;
    opens pdbexplorer.window to javafx.fxml;
    opens pdbexplorer.model to javafx.fxml;
    opens pdbexplorer to javafx.fxml;
}