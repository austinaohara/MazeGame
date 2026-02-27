package edu.farmingdale.mazegame;

import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Maze {

    // =========================================================
    // HARDCODED SPAWN AND END LOCATIONS
    // Change these values to move the start/end markers.
    // =========================================================

    // Maze 1
    private static final double MAZE1_SPAWN_X = 25;
    private static final double MAZE1_SPAWN_Y = 265;
    private static final double MAZE1_END_X = 580;
    private static final double MAZE1_END_Y = 250;

    // Maze 2
    private static final double MAZE2_SPAWN_X = 25;
    private static final double MAZE2_SPAWN_Y = 20;
    private static final double MAZE2_END_X = 440;
    private static final double MAZE2_END_Y = 310;

    // Maze 3
    private static final double MAZE3_SPAWN_X = 490;
    private static final double MAZE3_SPAWN_Y = 30;
    private static final double MAZE3_END_X = 715;
    private static final double MAZE3_END_Y = 820;

    // =========================================================

    private static final double AUTO_PIXELS_PER_SECOND = 120.0;
    private static final String CHALLENGER_FILE = "challenger.png";
    private static final double DEFAULT_PLAYER_SIZE = 20;
    private static final double CHALLENGER_PLAYER_SIZE = 12;
    private static final double DEFAULT_MOVE_SPEED = 140.0;
    private static final double CHALLENGER_MOVE_SPEED = 55.0;
    private static final double DEFAULT_DISPLAY_SCALE = 1.0;
    private static final double CHALLENGER_DISPLAY_SCALE = 0.68;
    // Set these to real values in challenger.png pixel coordinates.
    // Use -1 to fall back to auto-detecting blue/red circle centers.
    private static final double CHALLENGER_SPAWN_X = -1;
    private static final double CHALLENGER_SPAWN_Y = -1;
    private static final double CHALLENGER_END_X = -1;
    private static final double CHALLENGER_END_Y = -1;

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final Image mazeImage;
    private Image robotImage;
    private Car car;
    private double playerX;
    private double playerY;
    private double endX;
    private double endY;
    private final double playerSize;
    private final double displayScale;
    private final int stepSize = 4;
    private final Pane pane;
    private final boolean challengerLevel;
    private AnimationTimer autoTimer;
    private boolean showEndPoint = false;
    private boolean wonShown = false;

    public Maze(String mazeFileName, String playerFileName) {
        InputStream mazeStream = getClass().getResourceAsStream("/" + mazeFileName);
        if (mazeStream == null) {
            throw new RuntimeException(mazeFileName + " not found in resources!");
        }
        mazeImage = new Image(mazeStream);

        challengerLevel = CHALLENGER_FILE.equals(mazeFileName);
        playerSize = challengerLevel ? CHALLENGER_PLAYER_SIZE : DEFAULT_PLAYER_SIZE;
        displayScale = challengerLevel ? CHALLENGER_DISPLAY_SCALE : DEFAULT_DISPLAY_SCALE;

        canvas = new Canvas(mazeImage.getWidth() * displayScale, mazeImage.getHeight() * displayScale);
        gc = canvas.getGraphicsContext2D();

        StackPane centeredPane = new StackPane(canvas);
        centeredPane.setAlignment(Pos.CENTER);
        pane = centeredPane;

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
        } else if (mazeFileName.equals("maze3.png")) {
            playerX = MAZE3_SPAWN_X;
            playerY = MAZE3_SPAWN_Y;
            endX = MAZE3_END_X;
            endY = MAZE3_END_Y;
        } else if (challengerLevel) {
            setChallengerSpawnAndEnd();
        } else {
            throw new IllegalArgumentException("Unsupported maze file: " + mazeFileName);
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
    public double getMoveSpeedPxPerSecond() { return challengerLevel ? CHALLENGER_MOVE_SPEED : DEFAULT_MOVE_SPEED; }
    public boolean isCompleted() { return wonShown; }

    public void setShowEndMarker(boolean show) {
        this.showEndPoint = show;
        draw();
    }

    /**
     * Creates a standalone clone canvas showing the current maze + player state.
     */
    public Canvas createCloneCanvas() {
        Canvas clone = new Canvas(mazeImage.getWidth() * displayScale, mazeImage.getHeight() * displayScale);
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
        if (challengerLevel) {
            moveRobotByChallenger(dx, dy);
            return;
        }

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
            showWinIfReached();
        }
    }

    private void moveRobotByChallenger(double dx, double dy) {
        double startX = playerX;
        double startY = playerY;
        double remainingX = Math.abs(dx);
        double remainingY = Math.abs(dy);
        double dirX = Math.signum(dx);
        double dirY = Math.signum(dy);

        while (remainingX > 0.0001 || remainingY > 0.0001) {
            double stepX = remainingX > 0 ? dirX * Math.min(1.0, remainingX) : 0;
            double stepY = remainingY > 0 ? dirY * Math.min(1.0, remainingY) : 0;
            boolean movedThisStep = false;

            if (stepX != 0 && canMoveTo(playerX + stepX, playerY)) {
                playerX += stepX;
                movedThisStep = true;
            }
            if (stepY != 0 && canMoveTo(playerX, playerY + stepY)) {
                playerY += stepY;
                movedThisStep = true;
            }

            remainingX = Math.max(0, remainingX - Math.abs(stepX));
            remainingY = Math.max(0, remainingY - Math.abs(stepY));

            if (!movedThisStep) {
                break;
            }
        }

        // If the player got into an invalid pixel seam, relocate to the nearest safe tile.
        if (!canMoveTo(playerX, playerY)) {
            double[] corrected = findNearestWalkablePoint(playerX, playerY);
            playerX = corrected[0];
            playerY = corrected[1];
        }

        double appliedDx = playerX - startX;
        double appliedDy = playerY - startY;
        if (appliedDx != 0 || appliedDy != 0) {
            if (car != null) {
                car.setPosition(playerX, playerY);
                car.updateAngle(appliedDx, appliedDy);
            }
            draw();
            showWinIfReached();
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
            showWinIfReached();
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

    private void setChallengerSpawnAndEnd() {
        boolean hasSpawnOverride = CHALLENGER_SPAWN_X >= 0 && CHALLENGER_SPAWN_Y >= 0;
        boolean hasEndOverride = CHALLENGER_END_X >= 0 && CHALLENGER_END_Y >= 0;
        if (hasSpawnOverride && hasEndOverride) {
            playerX = clampToBounds(CHALLENGER_SPAWN_X, mazeImage.getWidth() - playerSize);
            playerY = clampToBounds(CHALLENGER_SPAWN_Y, mazeImage.getHeight() - playerSize);
            endX = clampToBounds(CHALLENGER_END_X, mazeImage.getWidth() - playerSize);
            endY = clampToBounds(CHALLENGER_END_Y, mazeImage.getHeight() - playerSize);
            return;
        }

        PixelReader reader = mazeImage.getPixelReader();
        List<double[]> blueCenters = findMarkerCenters(reader, true);
        List<double[]> redCenters = findMarkerCenters(reader, false);
        redCenters.sort(Comparator.comparingDouble(center -> center[1]));

        if (hasSpawnOverride) {
            playerX = clampToBounds(CHALLENGER_SPAWN_X, mazeImage.getWidth() - playerSize);
            playerY = clampToBounds(CHALLENGER_SPAWN_Y, mazeImage.getHeight() - playerSize);
        } else if (!blueCenters.isEmpty()) {
            double[] blueCenter = blueCenters.get(0);
            playerX = clampToBounds(blueCenter[0] - playerSize / 2, mazeImage.getWidth() - playerSize);
            playerY = clampToBounds(blueCenter[1] - playerSize / 2, mazeImage.getHeight() - playerSize);
        } else if (!redCenters.isEmpty()) {
            // If only red markers exist, top red is spawn.
            double[] topRed = redCenters.get(0);
            playerX = clampToBounds(topRed[0] - playerSize / 2, mazeImage.getWidth() - playerSize);
            playerY = clampToBounds(topRed[1] - playerSize / 2, mazeImage.getHeight() - playerSize);
        } else {
            playerX = 20;
            playerY = 20;
        }

        if (hasEndOverride) {
            endX = clampToBounds(CHALLENGER_END_X, mazeImage.getWidth() - playerSize);
            endY = clampToBounds(CHALLENGER_END_Y, mazeImage.getHeight() - playerSize);
        } else {
            // Challenger exit is always the nearest walkable point at the bottom-right area.
            double[] bottomRightExit = findBottomRightWalkablePoint();
            endX = bottomRightExit[0];
            endY = bottomRightExit[1];
        }

        double[] correctedSpawn = findNearestWalkablePoint(playerX, playerY);
        playerX = correctedSpawn[0];
        playerY = correctedSpawn[1];
    }

    private double[] findBottomRightWalkablePoint() {
        int maxX = (int) (mazeImage.getWidth() - playerSize - 1);
        int maxY = (int) (mazeImage.getHeight() - playerSize - 1);
        int snappedX = (maxX / stepSize) * stepSize;
        int snappedY = (maxY / stepSize) * stepSize;

        if (canMoveTo(snappedX, snappedY)) {
            return new double[]{snappedX, snappedY};
        }

        int maxRadius = Math.max((int) mazeImage.getWidth(), (int) mazeImage.getHeight());
        for (int r = stepSize; r <= maxRadius; r += stepSize) {
            for (int dx = 0; dx <= r; dx += stepSize) {
                int x = snappedX - dx;
                int y = snappedY - (r - dx);
                if (x >= 0 && y >= 0 && canMoveTo(x, y)) {
                    return new double[]{x, y};
                }
            }
        }

        return new double[]{Math.max(0, snappedX), Math.max(0, snappedY)};
    }

    private double[] findNearestWalkablePoint(double rawX, double rawY) {
        int baseX = ((int) Math.round(rawX) / stepSize) * stepSize;
        int baseY = ((int) Math.round(rawY) / stepSize) * stepSize;

        if (canMoveTo(baseX, baseY)) {
            return new double[]{baseX, baseY};
        }

        int maxRadius = Math.max((int) mazeImage.getWidth(), (int) mazeImage.getHeight());
        for (int r = stepSize; r <= maxRadius; r += stepSize) {
            for (int dx = -r; dx <= r; dx += stepSize) {
                int topY = baseY - r;
                int bottomY = baseY + r;
                int x = baseX + dx;
                if (x >= 0 && topY >= 0 && x < mazeImage.getWidth() && topY < mazeImage.getHeight() && canMoveTo(x, topY)) {
                    return new double[]{x, topY};
                }
                if (x >= 0 && bottomY >= 0 && x < mazeImage.getWidth() && bottomY < mazeImage.getHeight() && canMoveTo(x, bottomY)) {
                    return new double[]{x, bottomY};
                }
            }
            for (int dy = -r + stepSize; dy <= r - stepSize; dy += stepSize) {
                int leftX = baseX - r;
                int rightX = baseX + r;
                int y = baseY + dy;
                if (leftX >= 0 && y >= 0 && leftX < mazeImage.getWidth() && y < mazeImage.getHeight() && canMoveTo(leftX, y)) {
                    return new double[]{leftX, y};
                }
                if (rightX >= 0 && y >= 0 && rightX < mazeImage.getWidth() && y < mazeImage.getHeight() && canMoveTo(rightX, y)) {
                    return new double[]{rightX, y};
                }
            }
        }

        return new double[]{Math.max(0, baseX), Math.max(0, baseY)};
    }

    private List<double[]> findMarkerCenters(PixelReader reader, boolean blueMarker) {
        int width = (int) mazeImage.getWidth();
        int height = (int) mazeImage.getHeight();
        boolean[][] visited = new boolean[height][width];
        List<double[]> centers = new ArrayList<>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (visited[y][x]) {
                    continue;
                }
                Color c = reader.getColor(x, y);
                boolean marker = blueMarker ? isBlueMarker(c) : isRedMarker(c);
                if (marker) {
                    double[] center = floodFillMarkerCenter(reader, visited, x, y, blueMarker);
                    if (center != null) {
                        centers.add(center);
                    }
                } else {
                    visited[y][x] = true;
                }
            }
        }

        return centers;
    }

    private double[] floodFillMarkerCenter(PixelReader reader, boolean[][] visited, int startX, int startY, boolean blueMarker) {
        int width = (int) mazeImage.getWidth();
        int height = (int) mazeImage.getHeight();
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startX, startY});
        visited[startY][startX] = true;

        double sumX = 0;
        double sumY = 0;
        int count = 0;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int cx = cur[0];
            int cy = cur[1];
            sumX += cx;
            sumY += cy;
            count++;

            for (int[] d : dirs) {
                int nx = cx + d[0];
                int ny = cy + d[1];
                if (nx < 0 || ny < 0 || nx >= width || ny >= height || visited[ny][nx]) {
                    continue;
                }
                Color neighbor = reader.getColor(nx, ny);
                boolean marker = blueMarker ? isBlueMarker(neighbor) : isRedMarker(neighbor);
                if (marker) {
                    visited[ny][nx] = true;
                    queue.add(new int[]{nx, ny});
                } else {
                    visited[ny][nx] = true;
                }
            }
        }

        if (count == 0) {
            return null;
        }
        return new double[]{sumX / count, sumY / count};
    }

    private double clampToBounds(double value, double max) {
        return Math.max(0, Math.min(value, max));
    }

    private boolean isBlueMarker(Color color) {
        return color.getBlue() > 0.6 && color.getRed() < 0.35 && color.getGreen() < 0.55;
    }

    private boolean isRedMarker(Color color) {
        return color.getRed() > 0.6 && color.getGreen() < 0.35 && color.getBlue() < 0.35;
    }

    private void showWinIfReached() {
        if (wonShown || !isAtEnd()) {
            return;
        }

        wonShown = true;
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Maze Complete");
        alert.setHeaderText(null);
        alert.setContentText("You won!");
        alert.show();
    }

    private boolean isAtEnd() {
        double playerCenterX = playerX + (playerSize / 2.0);
        double playerCenterY = playerY + (playerSize / 2.0);
        double endCenterX = endX + (playerSize / 2.0);
        double endCenterY = endY + (playerSize / 2.0);
        double distance = Math.hypot(playerCenterX - endCenterX, playerCenterY - endCenterY);
        return distance <= Math.max(6, playerSize * 0.7);
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
        target.save();
        target.scale(displayScale, displayScale);
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
        target.restore();
    }

    // -----------------------------------------------------------------------
    // Collision
    // -----------------------------------------------------------------------

    private boolean canMoveTo(double x, double y) {
        double hitboxInset = challengerLevel ? 2.0 : 0.0;
        double left = x + hitboxInset;
        double top = y + hitboxInset;
        double right = x + playerSize - 1 - hitboxInset;
        double bottom = y + playerSize - 1 - hitboxInset;

        if (left < 0 || top < 0 ||
                right >= mazeImage.getWidth() ||
                bottom >= mazeImage.getHeight()) {
            return false;
        }

        PixelReader reader = mazeImage.getPixelReader();
        double[][] points = {
                {left, top},
                {right, top},
                {left, bottom},
                {right, bottom},
                {(left + right) / 2.0, (top + bottom) / 2.0}
        };

        for (double[] pt : points) {
            if (!isWalkable(reader, (int) pt[0], (int) pt[1])) {
                return false;
            }
        }
        return true;
    }

    private boolean isWalkable(PixelReader reader, int px, int py) {
        try {
            Color color = reader.getColor(px, py);
            boolean isWhite = color.getRed() > 0.85 && color.getGreen() > 0.85 && color.getBlue() > 0.85;
            boolean isOrange = color.getRed() > 0.6 && color.getGreen() > 0.1 && color.getGreen() < 0.8 && color.getBlue() < 0.15;
            boolean isPurple = color.getRed() > 0.3 && color.getRed() < 0.8 && color.getGreen() < 0.2 && color.getBlue() > 0.3;
            boolean isChallengerMarker = challengerLevel && (isBlueMarker(color) || isRedMarker(color));
            boolean isChallengerPathShade = challengerLevel && color.getBrightness() > 0.78;
            return isWhite || isOrange || isPurple || isChallengerMarker || isChallengerPathShade;
        } catch (Exception e) {
            return false;
        }
    }
}
