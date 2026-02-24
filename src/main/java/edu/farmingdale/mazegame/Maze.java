package edu.farmingdale.mazegame;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.io.InputStream;
import java.util.List;

public class Maze {

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
    private Car car;
    private double playerX, playerY;
    private double endX, endY;
    private final double playerSize = 20;
    private final int    stepSize   = 4;
    private Pane pane;
    private Timeline autoTimeline;
    private boolean showEndPoint = false;

    public Maze(String mazeFileName, String playerFileName)
    {
        InputStream mazeStream = getClass().getResourceAsStream("/" + mazeFileName);
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
            InputStream rs = getClass().getResourceAsStream("/" + playerFileName);
            if (rs == null) throw new RuntimeException(playerFileName + " not found in resources!");
            robotImage = new Image(rs);
        } else {
            car = new Car(playerX, playerY, playerSize, playerSize / 2);
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
    public void setShowEndMarker(boolean show)
    {
        this.showEndPoint = show;
        draw();
    }

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

    /**
     * Runs BFS from the current player position and animates on the provided
     * clone canvas. Never modifies playerX/playerY or the original pane.
     */
    public void autoSolveOnCanvas(Canvas cloneCanvas, Runnable onDone) {
        stopAuto();

        GraphicsContext cgc = cloneCanvas.getGraphicsContext2D();

        MazeSolver solver = new MazeSolver(mazeImage, stepSize, (int) playerSize);
        List<int[]> path  = solver.solve((int) playerX, (int) playerY, (int) endX, (int) endY);

        if (path.isEmpty()) {
            System.out.println("Auto-solve: no path from (" + (int)playerX + "," + (int)playerY + ")");
            if (onDone != null) onDone.run();
            return;
        }

        double[] cx = {playerX};
        double[] cy = {playerY};
        int[] idx   = {0};

        autoTimeline = new Timeline(new KeyFrame(Duration.millis(60), e -> {
            if (idx[0] >= path.size()) {
                autoTimeline.stop();
                autoTimeline = null;
                if (onDone != null) onDone.run();
                return;
            }
            int[] pos  = path.get(idx[0]++);
            double px  = cx[0], py = cy[0];
            cx[0] = pos[0];
            cy[0] = pos[1];
            if (car != null) {
                car.setPosition(cx[0], cy[0]);
                car.updateAngle(cx[0] - px, cy[0] - py);
            }
            drawOnto(cgc, cx[0], cy[0]);
        }));
        autoTimeline.setCycleCount(Timeline.INDEFINITE);
        autoTimeline.play();
    }

    // -----------------------------------------------------------------------
    // Manual movement
    // -----------------------------------------------------------------------

    public void moveRobot(double dx, double dy) {
        double nextX = playerX + dx * stepSize;
        double nextY = playerY + dy * stepSize;
        if (canMoveTo(nextX, nextY)) {
            playerX = nextX;
            playerY = nextY;
            if (car != null) { car.setPosition(playerX, playerY); car.updateAngle(dx, dy); }
            draw();
        }
    }

    // -----------------------------------------------------------------------
    // Auto-solve: BFS from CURRENT player position, slow animation
    // -----------------------------------------------------------------------

    /**
     * Finds the shortest path from wherever the player currently is to the
     * hardcoded end position, then animates the player along it slowly.
     * @param onDone called when animation finishes (may be null)
     */
    public void autoSolve(Runnable onDone) {
        stopAuto();

        MazeSolver solver = new MazeSolver(mazeImage, stepSize, (int) playerSize);
        List<int[]> path  = solver.solve((int) playerX, (int) playerY, (int) endX, (int) endY);

        if (path.isEmpty()) {
            System.out.println("Auto-solve: no path found from current position (" + (int)playerX + "," + (int)playerY + ")");
            if (onDone != null) onDone.run();
            return;
        }

        int[] idx = {0};
        // 60 ms per step — smooth but clearly watchable
        autoTimeline = new Timeline(new KeyFrame(Duration.millis(60), e -> {
            if (idx[0] >= path.size()) {
                autoTimeline.stop();
                autoTimeline = null;
                if (onDone != null) onDone.run();
                return;
            }
            int[] pos  = path.get(idx[0]++);
            double prevX = playerX, prevY = playerY;
            playerX = pos[0];
            playerY = pos[1];
            if (car != null) {
                car.setPosition(playerX, playerY);
                car.updateAngle(playerX - prevX, playerY - prevY);
            }
            draw();
        }));
        autoTimeline.setCycleCount(Timeline.INDEFINITE);
        autoTimeline.play();
    }

    /** Stops any running auto-solve animation. */
    public void stopAuto() {
        if (autoTimeline != null) {
            autoTimeline.stop();
            autoTimeline = null;
        }
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
        if(showEndPoint)
        {
            target.setFill(Color.LIMEGREEN);
            target.fillOval(endX, endY, playerSize, playerSize);
            target.setStroke(Color.DARKGREEN);
            target.setLineWidth(2);
            target.strokeOval(endX, endY, playerSize, playerSize);
        }

        // Player
        if (robotImage != null) {
            target.drawImage(robotImage, px, py, playerSize, playerSize);
        } else if (car != null) {
            car.draw(target);
        }
    }

    // -----------------------------------------------------------------------
    // Collision
    // -----------------------------------------------------------------------

    private boolean canMoveTo(double x, double y) {
        if (x < 0 || y < 0 ||
                x + playerSize >= mazeImage.getWidth() ||
                y + playerSize >= mazeImage.getHeight()) return false;
        javafx.scene.image.PixelReader reader = mazeImage.getPixelReader();
        double[][] points = {
                {x,                  y},
                {x + playerSize - 1, y},
                {x,                  y + playerSize - 1},
                {x + playerSize - 1, y + playerSize - 1},
                {x + playerSize / 2, y + playerSize / 2}
        };
        for (double[] pt : points) {
            if (!isWalkable(reader, (int) pt[0], (int) pt[1])) return false;
        }
        return true;
    }

    private boolean isWalkable(javafx.scene.image.PixelReader reader, int px, int py) {
        try {
            Color color = reader.getColor(px, py);
            boolean isWhite  = color.getRed() > 0.85 && color.getGreen() > 0.85 && color.getBlue() > 0.85;
            boolean isOrange = color.getRed() > 0.6 && color.getGreen() > 0.1 && color.getGreen() < 0.8 && color.getBlue() < 0.15;
            boolean isPurple = color.getRed() > 0.3 && color.getRed() < 0.8 && color.getGreen() < 0.2 && color.getBlue() > 0.3;
            return isWhite || isOrange || isPurple;
        } catch (Exception e) {
            return false;
        }
    }
}