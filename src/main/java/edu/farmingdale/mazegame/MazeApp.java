package edu.farmingdale.mazegame;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MazeApp extends Application {

    private Maze maze1;
    private Maze maze2;
    private boolean useCar = false;
    private TabPane tabPane;
    private Tab autoTab;

    @Override
    public void start(Stage stage) {
        Label title = new Label("Select Player Type");
        Button robotBtn = new Button("Robot");
        Button carBtn   = new Button("Car");

        HBox btnBox = new HBox(20, robotBtn, carBtn);
        btnBox.setAlignment(Pos.CENTER);

        VBox startPane = new VBox(20, title, btnBox);
        startPane.setAlignment(Pos.CENTER);

        Scene startScene = new Scene(startPane, 800, 600);
        stage.setScene(startScene);
        stage.setTitle("Maze Game");
        stage.show();

        robotBtn.setOnAction(e -> { useCar = false; initMazes(stage); });
        carBtn.setOnAction(e   -> { useCar = true;  initMazes(stage); });
    }

    private void initMazes(Stage stage) {
        String playerFile = useCar ? null : "robot.png";

        maze1 = new Maze("maze.png",  playerFile);
        maze2 = new Maze("maze2.png", playerFile);

        // ── Maze tabs ────────────────────────────────────────────────────────
        Tab tab1 = new Tab("Maze 1", maze1.getPane());
        Tab tab2 = new Tab("Maze 2", maze2.getPane());
        tab1.setClosable(false);
        tab2.setClosable(false);

        // ── Auto-Complete tab — disabled until a maze tab was visited ────────
        autoTab = new Tab("🔍 Auto-Complete");
        autoTab.setClosable(false);
        autoTab.setDisable(true);   // locked until maze tab is selected first

        tabPane = new TabPane(tab1, tab2, autoTab);

        // When the user switches TO the auto tab, build its content fresh
        // using whichever maze was active before, starting from current position
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == autoTab && oldTab != autoTab) {
                // Determine which maze was active before switching
                Maze activeMaze = (oldTab == tab1) ? maze1 : maze2;
                String mazeName = (oldTab == tab1) ? "Maze 1" : "Maze 2";
            }
        });

        // Unlock auto tab immediately — tab1 is already selected on load
        autoTab.setDisable(false);
        tab2.setOnSelectionChanged(e -> { if (tab2.isSelected()) autoTab.setDisable(false); });

        // ── Scene-level arrow key filter ─────────────────────────────────────
        Scene mazeScene = new Scene(tabPane, 800, 600);
        mazeScene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            KeyCode code = event.getCode();
            if (code == KeyCode.UP || code == KeyCode.DOWN ||
                    code == KeyCode.LEFT || code == KeyCode.RIGHT) {
                event.consume();
                Tab sel = tabPane.getSelectionModel().getSelectedItem();
                if (sel == tab1 || sel == tab2) {
                    Maze current = (sel == tab1) ? maze1 : maze2;
                    switch (code) {
                        case UP    -> current.moveRobot(0, -1);
                        case DOWN  -> current.moveRobot(0,  1);
                        case LEFT  -> current.moveRobot(-1, 0);
                        case RIGHT -> current.moveRobot(1,  0);
                        default    -> {}
                    }
                }
            }
        });

        tabPane.getSelectionModel().select(tab1);
        stage.setScene(mazeScene);
        stage.requestFocus();
    }

    public static void main(String[] args) { launch(args); }
}