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

    private Maze maze1;
    private Maze maze2;
    private Maze maze3;
    private Maze maze4;
    private boolean useCar = false;
    private TabPane tabPane;
    private Tab autoTab;
    private final java.util.Set<KeyCode> pressedKeys = java.util.EnumSet.noneOf(KeyCode.class);
    private AnimationTimer movementTimer;
    private AnimationTimer uiTimer;
    private long lastFrameNs;

    private LevelTimer level1Timer;
    private LevelTimer level2Timer;
    private LevelTimer level3Timer;
    private LevelTimer challengerTimer;

    private static class LevelTimer {
        private final Label label;
        private final Button startButton;
        private final Button stopButton;
        private final Maze maze;
        private long startNs;
        private long elapsedNs;
        private boolean running;
        private boolean started;

        private LevelTimer(Label label, Button startButton, Button stopButton, Maze maze) {
            this.label = label;
            this.startButton = startButton;
            this.stopButton = stopButton;
            this.maze = maze;
            this.startNs = 0L;
            this.elapsedNs = 0L;
            this.running = false;
            this.started = false;
            updateLabel(0L);

            this.startButton.setOnAction(e -> start());
            this.stopButton.setOnAction(e -> stop());
            this.stopButton.setDisable(true);
        }

        private void start() {
            if (started || maze.isCompleted()) {
                return;
            }
            started = true;
            running = true;
            startNs = System.nanoTime();
            startButton.setDisable(true);
            stopButton.setDisable(false);
        }

        private void stop() {
            if (!running) {
                return;
            }
            elapsedNs += (System.nanoTime() - startNs);
            running = false;
            stopButton.setDisable(true);
            updateLabel(elapsedNs);
        }

        private void tick(long nowNs) {
            if (!started) {
                updateLabel(elapsedNs);
                return;
            }
            if (running) {
                if (maze.isCompleted()) {
                    elapsedNs += (nowNs - startNs);
                    running = false;
                    stopButton.setDisable(true);
                } else {
                    updateLabel(elapsedNs + (nowNs - startNs));
                    return;
                }
            }
            updateLabel(elapsedNs);
        }

        private void updateLabel(long totalNs) {
            long totalMs = totalNs / 1_000_000L;
            long minutes = totalMs / 60_000L;
            long seconds = (totalMs % 60_000L) / 1000L;
            long tenths = (totalMs % 1000L) / 100L;
            label.setText(String.format("Time: %02d:%02d.%d", minutes, seconds, tenths));
        }
    }

    @Override
    public void start(Stage stage) {
        Label title = new Label("Select Player Type");
        Button robotBtn = new Button("Robot");
        Button carBtn = new Button("Car");

        String baseBtnStyle = "-fx-background-color:#e8edf2; -fx-text-fill:#111; -fx-font-size:14px;";
        String hoverBtnStyle = "-fx-background-color:#cfe3ff; -fx-text-fill:#111; -fx-font-size:14px;";
        robotBtn.setStyle(baseBtnStyle);
        carBtn.setStyle(baseBtnStyle);
        robotBtn.setOnMouseEntered(e -> robotBtn.setStyle(hoverBtnStyle));
        robotBtn.setOnMouseExited(e -> robotBtn.setStyle(baseBtnStyle));
        carBtn.setOnMouseEntered(e -> carBtn.setStyle(hoverBtnStyle));
        carBtn.setOnMouseExited(e -> carBtn.setStyle(baseBtnStyle));

        // Prevent initial focus ring/highlight on startup.
        robotBtn.setFocusTraversable(false);
        carBtn.setFocusTraversable(false);

        HBox btnBox = new HBox(20, robotBtn, carBtn);
        btnBox.setAlignment(Pos.CENTER);

        VBox startPane = new VBox(20, title, btnBox);
        startPane.setAlignment(Pos.CENTER);

        Scene startScene = new Scene(startPane, 1000, 800);
        stage.setScene(startScene);
        stage.setTitle("Maze Game");
        stage.show();
        startPane.requestFocus();

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
        maze4 = new Maze("challenger.png", playerFile);

        Label timer1Label = createTimerLabel();
        Label timer2Label = createTimerLabel();
        Label timer3Label = createTimerLabel();
        Label timer4Label = createTimerLabel();
        Button timer1Start = createTimerStartButton();
        Button timer2Start = createTimerStartButton();
        Button timer3Start = createTimerStartButton();
        Button timer4Start = createTimerStartButton();
        Button timer1Stop = createTimerStopButton();
        Button timer2Stop = createTimerStopButton();
        Button timer3Stop = createTimerStopButton();
        Button timer4Stop = createTimerStopButton();

        level1Timer = new LevelTimer(timer1Label, timer1Start, timer1Stop, maze1);
        level2Timer = new LevelTimer(timer2Label, timer2Start, timer2Stop, maze2);
        level3Timer = new LevelTimer(timer3Label, timer3Start, timer3Stop, maze3);
        challengerTimer = new LevelTimer(timer4Label, timer4Start, timer4Stop, maze4);

        Tab tab1 = new Tab("Maze 1", buildLevelPane(maze1.getPane(), buildTimerRow(timer1Label, timer1Start, timer1Stop)));
        Tab tab2 = new Tab("Maze 2", buildLevelPane(maze2.getPane(), buildTimerRow(timer2Label, timer2Start, timer2Stop)));
        Tab tab3 = new Tab("Maze 3", buildLevelPane(maze3.getPane(), buildTimerRow(timer3Label, timer3Start, timer3Stop)));
        Tab tab4 = new Tab("Challenger", buildLevelPane(maze4.getPane(), buildTimerRow(timer4Label, timer4Start, timer4Stop)));
        tab1.setClosable(false);
        tab2.setClosable(false);
        tab3.setClosable(false);
        tab4.setClosable(false);
        tab4.setStyle("-fx-background-color: #e53935;");
        Label challengerLabel = new Label("Challenger");
        challengerLabel.setTextFill(Color.BLACK);
        challengerLabel.setStyle("-fx-font-weight: bold;");
        tab4.setText("");
        tab4.setGraphic(challengerLabel);

        autoTab = new Tab("Auto-Complete");
        autoTab.setClosable(false);
        autoTab.setDisable(true);

        tabPane = new TabPane(tab1, tab2, tab3, tab4, autoTab);

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == autoTab && oldTab != autoTab) {
                Maze activeMaze;
                String mazeName;
                if (oldTab == tab1) {
                    activeMaze = maze1;
                    mazeName = "Maze 1";
                } else if (oldTab == tab2) {
                    activeMaze = maze2;
                    mazeName = "Maze 2";
                } else if (oldTab == tab3) {
                    activeMaze = maze3;
                    mazeName = "Maze 3";
                } else {
                    activeMaze = maze4;
                    mazeName = "Challenger";
                }
                autoTab.setContent(buildAutoPane(activeMaze, mazeName));
            }
        });

        autoTab.setDisable(false);

        Scene mazeScene = new Scene(tabPane, 1200, 1000);
        setupSmoothMovement(mazeScene, tab1, tab2, tab3, tab4);
        setupTimers();

        tabPane.getSelectionModel().select(tab1);
        stage.setScene(mazeScene);
        stage.requestFocus();
    }

    private Label createTimerLabel() {
        Label label = new Label();
        label.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#1e2a38;");
        return label;
    }

    private Button createTimerStartButton() {
        Button button = new Button("Start");
        button.setStyle("-fx-background-color:#2d89ef; -fx-text-fill:white; -fx-font-size:12px;");
        return button;
    }

    private Button createTimerStopButton() {
        Button button = new Button("Stop");
        button.setStyle("-fx-background-color:#e74c3c; -fx-text-fill:white; -fx-font-size:12px;");
        return button;
    }

    private HBox buildTimerRow(Label timerLabel, Button startButton, Button stopButton) {
        HBox row = new HBox(10, timerLabel, startButton, stopButton);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Pane buildLevelPane(Pane mazePane, HBox timerRow) {
        VBox root = new VBox(8);
        root.setPadding(new Insets(12));
        VBox.setVgrow(mazePane, Priority.ALWAYS);
        root.getChildren().addAll(timerRow, mazePane);
        return root;
    }

    private void setupTimers() {
        if (uiTimer != null) {
            uiTimer.stop();
        }

        uiTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                level1Timer.tick(now);
                level2Timer.tick(now);
                level3Timer.tick(now);
                challengerTimer.tick(now);
            }
        };
        uiTimer.start();
    }

    private void setupSmoothMovement(Scene scene, Tab tab1, Tab tab2, Tab tab3, Tab tab4) {
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
                if (selected != tab1 && selected != tab2 && selected != tab3 && selected != tab4) {
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

                Maze current;
                if (selected == tab1) {
                    current = maze1;
                } else if (selected == tab2) {
                    current = maze2;
                } else if (selected == tab3) {
                    current = maze3;
                } else {
                    current = maze4;
                }
                double distance = current.getMoveSpeedPxPerSecond() * deltaSeconds;
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

        Label info = new Label("Solving from current player position to this level's exit.");
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