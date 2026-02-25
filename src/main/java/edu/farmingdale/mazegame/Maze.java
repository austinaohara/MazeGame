package edu.farmingdale.mazegame;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.io.InputStream;
import java.util.List;

public class Maze {

    // =========================================================
    // HARDCODED SPAWN AND END LOCATIONS
    // Change these values to move the start/end markers.
    // =========================================================

    // Maze 1
    private static final double MAZE1_SPAWN_X = 25;
    private static final double MAZE1_SPAWN_Y = 265;
    private static final double MAZE1_END_X = 580;//580
    private static final double MAZE1_END_Y = 250;//250

    // Maze 2
    private static final double MAZE2_SPAWN_X = 25;
    private static final double MAZE2_SPAWN_Y = 20;
    private static final double MAZE2_END_X = 440;
    private static final double MAZE2_END_Y = 310;

    // Maze 3
    private static final double MAZE3_SPAWN_X = 490;//490
    private static final double MAZE3_SPAWN_Y = 30;//30
    private static final double MAZE3_END_X = 715;
    private static final double MAZE3_END_Y = 820;

    // =========================================================



    private static final double AUTO_PIXELS_PER_SECOND = 120.0;

    private Canvas canvas;
    private GraphicsContext gc;
    private Image mazeImage;
    private Image robotImage;
    private Car car;
    private double playerX, playerY;
    private double endX, endY;
    private final double playerSize = 20;
    private final int stepSize = 4;
    private Pane pane;
    private AnimationTimer autoTimer;
    private boolean showEndPoint = false;

    public Maze(String mazeFileName, String playerFileName) {
        InputStream mazeStream = getClass().getResourceAsStream("/" + mazeFileName);
        if (mazeStream == null) {
            throw new RuntimeException(mazeFileName + " not found in resources!");
        }
        mazeImage = new Image(mazeStream);

        canvas = new Canvas(mazeImage.getWidth(), mazeImage.getHeight());
        gc = canvas.getGraphicsContext2D();
        pane = new Pane(canvas);

        if (mazeFileName.equals("maze.png")) {
            playerX = MAZE1_SPAWN_X;
            playerY = MAZE1_SPAWN_Y;
            endX = MAZE1_END_X;
            endY = MAZE1_END_Y;
        } else if (mazeFileName.equals("maze2.png")) {
            playerX = MAZE2_SPAWN_X;
            playerY = MAZE2_SPAWN_Y;
            endX = MAZE2_END_X;
            endY = MAZE2_END_Y;
        } else {
            playerX = MAZE3_SPAWN_X;
            playerY = MAZE3_SPAWN_Y;
            endX = MAZE3_END_X;
            endY = MAZE3_END_Y;
        }

        if (playerFileName != null) {
            InputStream rs = getClass().getResourceAsStream("/" + playerFileName);
            if (rs == null) {
                throw new RuntimeException(playerFileName + " not found in resources!");
            }
            robotImage = new Image(rs);
        } else {
            car = new Car(playerX, playerY, playerSize, playerSize / 2);
        }

        draw();
    }

    public Pane getPane() { return pane; }
    public Image getMazeImage() { return mazeImage; }
    public double getPlayerX() { return playerX; }
    public double getPlayerY() { return playerY; }
    public double getEndX() { return endX; }
    public double getEndY() { return endY; }
    public int getStepSize() { return stepSize; }
    public int getPlayerSize() { return (int) playerSize; }

    public void setShowEndMarker(boolean show) {
        this.showEndPoint = show;
        draw();
    }

    /**
     * Creates a standalone clone canvas showing the current maze + player state.
     */
    public Canvas createCloneCanvas() {
        Canvas clone = new Canvas(mazeImage.getWidth(), mazeImage.getHeight());
        drawOnto(clone.getGraphicsContext2D(), playerX, playerY);
        return clone;
    }

    /**
     * Runs BFS from the current player position and animates on the provided clone canvas.
     * Never modifies playerX/playerY or the original pane.
     */
    public void autoSolveOnCanvas(Canvas cloneCanvas, Runnable onDone) {
        stopAuto();

        GraphicsContext cgc = cloneCanvas.getGraphicsContext2D();
        MazeSolver solver = new MazeSolver(mazeImage, stepSize, (int) playerSize);
        int[] start = resolveAutoStart(playerX, playerY);
        List<int[]> path = solver.solve(start[0], start[1], (int) endX, (int) endY);

        if (path.isEmpty()) {
            System.out.println("Auto-solve: no path from (" + (int) playerX + "," + (int) playerY + ")");
            if (onDone != null) {
                onDone.run();
            }
            return;
        }

        startAutoAnimation(path, start[0], start[1], (x, y, dx, dy) -> {
            if (car != null) {
                car.setPosition(x, y);
                car.updateAngle(dx, dy);
            }
            drawOnto(cgc, x, y);
        }, onDone);
    }

    // -----------------------------------------------------------------------
    // Manual movement
    // -----------------------------------------------------------------------

    public void moveRobot(double dx, double dy) {
        moveRobotBy(dx * stepSize, dy * stepSize);
    }

    public void moveRobotBy(double dx, double dy) {
        double appliedDx = 0;
        double appliedDy = 0;

        if (dx != 0) {
            double nextX = playerX + dx;
            if (canMoveTo(nextX, playerY)) {
                playerX = nextX;
                appliedDx = dx;
            }
        }

        if (dy != 0) {
            double nextY = playerY + dy;
            if (canMoveTo(playerX, nextY)) {
                playerY = nextY;
                appliedDy = dy;
            }
        }

        if (appliedDx != 0 || appliedDy != 0) {
            if (car != null) {
                car.setPosition(playerX, playerY);
                car.updateAngle(appliedDx, appliedDy);
            }
            draw();
        }
    }

    // -----------------------------------------------------------------------
    // Auto-solve: BFS from current player position
    // -----------------------------------------------------------------------

    public void autoSolve(Runnable onDone) {
        stopAuto();

        MazeSolver solver = new MazeSolver(mazeImage, stepSize, (int) playerSize);
        int[] start = resolveAutoStart(playerX, playerY);
        List<int[]> path = solver.solve(start[0], start[1], (int) endX, (int) endY);

        if (path.isEmpty()) {
            System.out.println("Auto-solve: no path found from current position (" + (int) playerX + "," + (int) playerY + ")");
            if (onDone != null) {
                onDone.run();
            }
            return;
        }

        startAutoAnimation(path, start[0], start[1], (x, y, dx, dy) -> {
            playerX = x;
            playerY = y;
            if (car != null) {
                car.setPosition(playerX, playerY);
                car.updateAngle(dx, dy);
            }
            draw();
        }, onDone);
    }

    /** Stops any running auto-solve animation. */
    public void stopAuto() {
        if (autoTimer != null) {
            autoTimer.stop();
            autoTimer = null;
        }
    }

    private interface AutoStepRenderer {
        void render(double x, double y, double dx, double dy);
    }

    private void startAutoAnimation(List<int[]> path, double startX, double startY,
                                    AutoStepRenderer renderer, Runnable onDone) {
        double[] x = {startX};
        double[] y = {startY};
        int[] idx = {0};
        long[] lastNs = {0L};

        autoTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (idx[0] >= path.size()) {
                    stopAuto();
                    if (onDone != null) {
                        onDone.run();
                    }
                    return;
                }

                if (lastNs[0] == 0L) {
                    lastNs[0] = now;
                    return;
                }

                double dt = (now - lastNs[0]) / 1_000_000_000.0;
                lastNs[0] = now;
                if (dt > 0.05) {
                    dt = 0.05;
                }

                double remaining = AUTO_PIXELS_PER_SECOND * dt;
                double prevX = x[0];
                double prevY = y[0];

                while (remaining > 0 && idx[0] < path.size()) {
                    int[] target = path.get(idx[0]);
                    double dx = target[0] - x[0];
                    double dy = target[1] - y[0];
                    double dist = Math.hypot(dx, dy);

                    if (dist < 1e-6) {
                        idx[0]++;
                        continue;
                    }

                    if (remaining >= dist) {
                        x[0] = target[0];
                        y[0] = target[1];
                        remaining -= dist;
                        idx[0]++;
                    } else {
                        x[0] += (dx / dist) * remaining;
                        y[0] += (dy / dist) * remaining;
                        remaining = 0;
                    }
                }

                renderer.render(x[0], y[0], x[0] - prevX, y[0] - prevY);

                if (idx[0] >= path.size()) {
                    stopAuto();
                    if (onDone != null) {
                        onDone.run();
                    }
                }
            }
        };

        autoTimer.start();
    }

    private int[] resolveAutoStart(double rawX, double rawY) {
        int baseX = ((int) Math.round(rawX) / stepSize) * stepSize;
        int baseY = ((int) Math.round(rawY) / stepSize) * stepSize;

        if (canMoveTo(baseX, baseY)) {
            return new int[]{baseX, baseY};
        }

        int maxRadius = stepSize * 8;
        for (int r = stepSize; r <= maxRadius; r += stepSize) {
            for (int dx = -r; dx <= r; dx += stepSize) {
                int topY = baseY - r;
                int bottomY = baseY + r;
                int x = baseX + dx;
                if (canMoveTo(x, topY)) return new int[]{x, topY};
                if (canMoveTo(x, bottomY)) return new int[]{x, bottomY};
            }
            for (int dy = -r + stepSize; dy <= r - stepSize; dy += stepSize) {
                int leftX = baseX - r;
                int rightX = baseX + r;
                int y = baseY + dy;
                if (canMoveTo(leftX, y)) return new int[]{leftX, y};
                if (canMoveTo(rightX, y)) return new int[]{rightX, y};
            }
        }

        return new int[]{(int) Math.round(rawX), (int) Math.round(rawY)};
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

        if (showEndPoint) {
            target.setFill(Color.LIMEGREEN);
            target.fillOval(endX, endY, playerSize, playerSize);
            target.setStroke(Color.DARKGREEN);
            target.setLineWidth(2);
            target.strokeOval(endX, endY, playerSize, playerSize);
        }

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
                y + playerSize >= mazeImage.getHeight()) {
            return false;
        }

        javafx.scene.image.PixelReader reader = mazeImage.getPixelReader();
        double[][] points = {
                {x, y},
                {x + playerSize - 1, y},
                {x, y + playerSize - 1},
                {x + playerSize - 1, y + playerSize - 1},
                {x + playerSize / 2, y + playerSize / 2}
        };

        for (double[] pt : points) {
            if (!isWalkable(reader, (int) pt[0], (int) pt[1])) {
                return false;
            }
        }
        return true;
    }

    private boolean isWalkable(javafx.scene.image.PixelReader reader, int px, int py) {
        try {
            Color color = reader.getColor(px, py);
            boolean isWhite = color.getRed() > 0.85 && color.getGreen() > 0.85 && color.getBlue() > 0.85;
            boolean isOrange = color.getRed() > 0.6 && color.getGreen() > 0.1 && color.getGreen() < 0.8 && color.getBlue() < 0.15;
            boolean isPurple = color.getRed() > 0.3 && color.getRed() < 0.8 && color.getGreen() < 0.2 && color.getBlue() > 0.3;
            return isWhite || isOrange || isPurple;
        } catch (Exception e) {
            return false;
        }
    }
}
