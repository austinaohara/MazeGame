package edu.farmingdale.mazegame;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

import java.util.*;

/**
 * BFS maze solver that operates directly on maze image pixel data.
 * Returns an ordered list of {x, y} waypoints from start to end.
 */
public class MazeSolver {

    private final Image mazeImage;
    private final int stepSize;
    private final int playerSize;

    public MazeSolver(Image mazeImage, int stepSize, int playerSize) {
        this.mazeImage  = mazeImage;
        this.stepSize   = stepSize;
        this.playerSize = playerSize;
    }

    /**
     * BFS from (startX, startY) to (endX, endY).
     * Returns the shortest path as a list of {x, y} int arrays,
     * or empty if no path exists.
     */
    public List<int[]> solve(int startX, int startY, int endX, int endY) {
        startX = snap(startX);
        startY = snap(startY);
        endX   = snap(endX);
        endY   = snap(endY);

        if (!isWalkable(startX, startY)) {
            System.out.println("BFS: start not walkable (" + startX + "," + startY + ")");
            return Collections.emptyList();
        }
        if (!isWalkable(endX, endY)) {
            System.out.println("BFS: end not walkable (" + endX + "," + endY + ")");
            return Collections.emptyList();
        }

        Map<String, int[]> cameFrom = new HashMap<>();
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{startX, startY});
        cameFrom.put(key(startX, startY), null);

        int[][] dirs = {{stepSize, 0}, {-stepSize, 0}, {0, stepSize}, {0, -stepSize}};
        int[] goal = null;

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            if (cur[0] == endX && cur[1] == endY) { goal = cur; break; }
            for (int[] d : dirs) {
                int nx = cur[0] + d[0], ny = cur[1] + d[1];
                String k = key(nx, ny);
                if (!cameFrom.containsKey(k) && isWalkable(nx, ny)) {
                    cameFrom.put(k, new int[]{cur[0], cur[1]});
                    queue.add(new int[]{nx, ny});
                }
            }
        }

        if (goal == null) {
            System.out.println("BFS: no path found.");
            return Collections.emptyList();
        }

        LinkedList<int[]> path = new LinkedList<>();
        int[] cur = goal;
        while (cur != null) {
            path.addFirst(cur);
            cur = cameFrom.get(key(cur[0], cur[1]));
        }
        return path;
    }

    private int snap(int v) { return (v / stepSize) * stepSize; }
    private String key(int x, int y) { return x + "," + y; }

    private boolean isWalkable(int x, int y) {
        if (x < 0 || y < 0
                || x + playerSize >= (int) mazeImage.getWidth()
                || y + playerSize >= (int) mazeImage.getHeight()) return false;
        PixelReader reader = mazeImage.getPixelReader();
        int[][] pts = {
                {x,                  y},
                {x + playerSize - 1, y},
                {x,                  y + playerSize - 1},
                {x + playerSize - 1, y + playerSize - 1},
                {x + playerSize / 2, y + playerSize / 2}
        };
        for (int[] pt : pts) {
            try {
                Color c = reader.getColor(pt[0], pt[1]);
                boolean isWhite  = c.getRed() > 0.85 && c.getGreen() > 0.85 && c.getBlue() > 0.85;
                boolean isOrange = c.getRed() > 0.6 && c.getGreen() > 0.1 && c.getGreen() < 0.8 && c.getBlue() < 0.15;
                boolean isPurple = c.getRed() > 0.3 && c.getRed() < 0.8 && c.getGreen() < 0.2 && c.getBlue() > 0.3;
                boolean isBlue = c.getBlue() > 0.6 && c.getRed() < 0.35 && c.getGreen() < 0.55;
                boolean isRed = c.getRed() > 0.6 && c.getGreen() < 0.35 && c.getBlue() < 0.35;
                boolean isBrightPath = c.getBrightness() > 0.78;
                if (!(isWhite || isOrange || isPurple || isBlue || isRed || isBrightPath)) return false;
            } catch (Exception e) { return false; }
        }
        return true;
    }
}
