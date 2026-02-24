package edu.farmingdale.mazegame;

import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.io.InputStream;

public class Maze {
    private static final String IMAGE_BASE_PATH = "/edu/farmingdale/mazegame/images/";

    // =========================================================
    // HARDCODED SPAWN AND END LOCATIONS
    // Change these values to move the start/end markers.
    // =========================================================

    // --- Maze 1 ---
    private static final double MAZE1_SPAWN_X = 25;
    private static final double MAZE1_SPAWN_Y = 265;
    private static final double MAZE1_END_X   = 580;
    private static final double MAZE1_END_Y   = 250;

    // --- Maze 2 ---
    private static final double MAZE2_SPAWN_X = 25;
    private static final double MAZE2_SPAWN_Y = 20;
    private static final double MAZE2_END_X   = 440;
    private static final double MAZE2_END_Y   = 310;

    // =========================================================

    private Canvas canvas;
    private GraphicsContext gc;
    private Image mazeImage;
    private Image robotImage;
    private double playerX, playerY;
    private double endX, endY;
    private final double playerSize = 20;
    private final int    stepSize   = 4;
    private Pane pane;
    private Timeline autoTimeline;

    public Maze(String mazeFileName, String playerFileName) {
        InputStream mazeStream = getClass().getResourceAsStream(IMAGE_BASE_PATH + mazeFileName);
        if (mazeStream == null) throw new RuntimeException(mazeFileName + " not found in resources!");
        mazeImage = new Image(mazeStream);

        canvas = new Canvas(mazeImage.getWidth(), mazeImage.getHeight());
        gc     = canvas.getGraphicsContext2D();
        pane   = new Pane(canvas);

        if (mazeFileName.equals("maze.png")) {
            playerX = MAZE1_SPAWN_X; playerY = MAZE1_SPAWN_Y;
            endX    = MAZE1_END_X;   endY    = MAZE1_END_Y;
        } else {
            playerX = MAZE2_SPAWN_X; playerY = MAZE2_SPAWN_Y;
            endX    = MAZE2_END_X;   endY    = MAZE2_END_Y;
        }

        if (playerFileName != null) {
            InputStream rs = getClass().getResourceAsStream(IMAGE_BASE_PATH + playerFileName);
            if (rs == null) throw new RuntimeException(playerFileName + " not found in resources!");
            robotImage = new Image(rs);
        }

        draw();
    }

    public Pane   getPane()        { return pane; }
    public Image  getMazeImage()   { return mazeImage; }
    public double getPlayerX()     { return playerX; }
    public double getPlayerY()     { return playerY; }
    public double getEndX()        { return endX; }
    public double getEndY()        { return endY; }
    public int    getStepSize()    { return stepSize; }
    public int    getPlayerSize()  { return (int) playerSize; }

    /**
     * Creates a standalone clone canvas showing the current maze + player state.
     * The auto-solve animation draws onto this clone only — the original maze
     * tab is completely untouched.
     */
    public Canvas createCloneCanvas() {
        Canvas clone = new Canvas(mazeImage.getWidth(), mazeImage.getHeight());
        drawOnto(clone.getGraphicsContext2D(), playerX, playerY);
        return clone;
    }

    // -----------------------------------------------------------------------
    // Manual movement
    // -----------------------------------------------------------------------

    public void moveRobot(double dx, double dy) {
        double nextX = playerX + dx * stepSize;
        double nextY = playerY + dy * stepSize;
        playerX = nextX;
        playerY = nextY;
        draw();
    }

    // -----------------------------------------------------------------------
    // Drawing
    // -----------------------------------------------------------------------

    private void draw() {
        drawOnto(gc, playerX, playerY);
    }

    /** Draws the maze image, end marker, and player at (px, py) onto any GraphicsContext. */
    private void drawOnto(GraphicsContext target, double px, double py) {
        target.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        target.drawImage(mazeImage, 0, 0);

        // End marker
        target.setFill(Color.LIMEGREEN);
        target.fillOval(endX, endY, playerSize, playerSize);
        target.setStroke(Color.DARKGREEN);
        target.setLineWidth(2);
        target.strokeOval(endX, endY, playerSize, playerSize);

        // Player
        if (robotImage != null) {
            target.drawImage(robotImage, px, py, playerSize, playerSize);
        }
    }
}
