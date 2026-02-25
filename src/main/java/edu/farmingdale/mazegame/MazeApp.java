package edu.farmingdale.mazegame;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class MazeApp extends Application {

    private static final double PLAYER_SPEED_PX_PER_SEC = 140.0;

    private Maze maze1;
    private Maze maze2;
    private Maze maze3;
    private boolean useCar = false;
    private TabPane tabPane;
    private Tab autoTab;
    private final java.util.Set<KeyCode> pressedKeys = java.util.EnumSet.noneOf(KeyCode.class);
    private AnimationTimer movementTimer;
    private long lastFrameNs;

    @Override
    public void start(Stage stage) {
        Label title = new Label("Select Player Type");
        Button robotBtn = new Button("Robot");
        Button carBtn = new Button("Car");

        HBox btnBox = new HBox(20, robotBtn, carBtn);
        btnBox.setAlignment(Pos.CENTER);

        VBox startPane = new VBox(20, title, btnBox);
        startPane.setAlignment(Pos.CENTER);

        Scene startScene = new Scene(startPane, 1000, 800);
        stage.setScene(startScene);
        stage.setTitle("Maze Game");
        stage.show();

        robotBtn.setOnAction(e -> {
            useCar = false;
            initMazes(stage);
        });
        carBtn.setOnAction(e -> {
            useCar = true;
            initMazes(stage);
        });
    }

    private void initMazes(Stage stage) {
        String playerFile = useCar ? null : "robot.png";

        maze1 = new Maze("maze.png", playerFile);
        maze2 = new Maze("maze2.png", playerFile);
        maze3 = new Maze("maze3.png", playerFile);

        Tab tab1 = new Tab("Maze 1", maze1.getPane());
        Tab tab2 = new Tab("Maze 2", maze2.getPane());
        Tab tab3 = new Tab("Maze 3", maze3.getPane());
        tab1.setClosable(false);
        tab2.setClosable(false);
        tab3.setClosable(false);

        autoTab = new Tab("Auto-Complete");
        autoTab.setClosable(false);
        autoTab.setDisable(true);

        tabPane = new TabPane(tab1, tab2, tab3, autoTab);

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
//            if (newTab == autoTab && oldTab != autoTab) {
//                Maze activeMaze = (oldTab == tab1) ? maze1 : maze2;
//                String mazeName = (oldTab == tab1) ? "Maze 1" : "Maze 2";
//                autoTab.setContent(buildAutoPane(activeMaze, mazeName));
//            }

            if (newTab == autoTab && oldTab != autoTab) {
                Maze activeMaze;
                String mazeName;
                if (oldTab == tab1) {
                    activeMaze = maze1;
                    mazeName = "Maze 1";
                }else if (oldTab == tab2) {
                    activeMaze = maze2;
                    mazeName = "Maze 2";
                }else{
                    activeMaze = maze3;
                    mazeName = "Maze 3";
                }
                autoTab.setContent(buildAutoPane(activeMaze, mazeName));
            }
        });

        autoTab.setDisable(false);
        tab2.setOnSelectionChanged(e -> {
            if (tab2.isSelected()) {
                autoTab.setDisable(false);
            }
        });

        Scene mazeScene = new Scene(tabPane, 1200, 1000);
        setupSmoothMovement(mazeScene, tab1, tab2,tab3);

        tabPane.getSelectionModel().select(tab1);
        stage.setScene(mazeScene);
        stage.requestFocus();
    }

    private void setupSmoothMovement(Scene scene, Tab tab1, Tab tab2, Tab tab3) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            KeyCode code = event.getCode();
            if (isArrow(code)) {
                pressedKeys.add(code);
                event.consume();
            }
        });

        scene.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            KeyCode code = event.getCode();
            if (isArrow(code)) {
                pressedKeys.remove(code);
                event.consume();
            }
        });

        if (movementTimer != null) {
            movementTimer.stop();
        }

        lastFrameNs = 0;
        movementTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastFrameNs == 0) {
                    lastFrameNs = now;
                    return;
                }

                double deltaSeconds = (now - lastFrameNs) / 1_000_000_000.0;
                lastFrameNs = now;
                if (deltaSeconds > 0.05) {
                    deltaSeconds = 0.05;
                }

                Tab selected = tabPane.getSelectionModel().getSelectedItem();
                if (selected != tab1 && selected != tab2 && selected != tab3) {
                    return;
                }

                double dirX = 0;
                double dirY = 0;
                if (pressedKeys.contains(KeyCode.LEFT)) {
                    dirX -= 1;
                }
                if (pressedKeys.contains(KeyCode.RIGHT)) {
                    dirX += 1;
                }
                if (pressedKeys.contains(KeyCode.UP)) {
                    dirY -= 1;
                }
                if (pressedKeys.contains(KeyCode.DOWN)) {
                    dirY += 1;
                }

                if (dirX == 0 && dirY == 0) {
                    return;
                }

                double len = Math.hypot(dirX, dirY);
                dirX /= len;
                dirY /= len;

//                Maze current = (selected == tab1) ? maze1 : maze2;
                Maze current;
                if (selected == tab1) {
                    current = maze1;
                }else if (selected == tab2) {
                    current = maze2;
                }else{
                    current = maze3;
                }
                double distance = PLAYER_SPEED_PX_PER_SEC * deltaSeconds;
                current.moveRobotBy(dirX * distance, dirY * distance);
            }
        };

        movementTimer.start();
    }

    private boolean isArrow(KeyCode code) {
        return code == KeyCode.UP || code == KeyCode.DOWN
                || code == KeyCode.LEFT || code == KeyCode.RIGHT;
    }

    private Pane buildAutoPane(Maze maze, String mazeName) {
        Label heading = new Label("Auto-Complete - " + mazeName);
        heading.setFont(Font.font("Arial", FontWeight.BOLD, 15));

        Label info = new Label("Solving from current player position -> green marker (end)");
        info.setStyle("-fx-text-fill:#555; -fx-font-size:12px;");

        Label statusLabel = new Label("Press Solve to start.");
        statusLabel.setStyle("-fx-font-size:13px; -fx-text-fill:#333;");

        Button solveBtn = new Button("Solve");
        Button stopBtn = new Button("Stop");

        solveBtn.setStyle("-fx-background-color:#27ae60; -fx-text-fill:white; -fx-font-size:13px;");
        stopBtn.setStyle("-fx-background-color:#e74c3c; -fx-text-fill:white; -fx-font-size:13px;");
        stopBtn.setDisable(true);

        javafx.scene.canvas.Canvas cloneCanvas = maze.createCloneCanvas();
        Pane clonePane = new Pane(cloneCanvas);

        solveBtn.setOnAction(e -> {
            solveBtn.setDisable(true);
            stopBtn.setDisable(false);
            statusLabel.setText("Solving...");
            statusLabel.setTextFill(Color.DARKORANGE);

            maze.autoSolveOnCanvas(cloneCanvas, () -> {
                solveBtn.setDisable(false);
                stopBtn.setDisable(true);
                statusLabel.setText("Done!");
                statusLabel.setTextFill(Color.GREEN);
            });
        });

        stopBtn.setOnAction(e -> {
            maze.stopAuto();
            solveBtn.setDisable(false);
            stopBtn.setDisable(true);
            statusLabel.setText("Stopped. Press Solve to resume from current position.");
            statusLabel.setTextFill(Color.GRAY);
        });

        HBox btnRow = new HBox(12, solveBtn, stopBtn, statusLabel);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        ScrollPane scroll = new ScrollPane(clonePane);
        scroll.setStyle("-fx-border-color:#ccc; -fx-border-width:1;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox root = new VBox(12, heading, info, btnRow, scroll);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color:#f5f7fa;");
        return root;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
