module edu.farmingdale.mazegame {
    requires javafx.controls;
    requires javafx.fxml;


    opens edu.farmingdale.mazegame to javafx.fxml;
    exports edu.farmingdale.mazegame;
}