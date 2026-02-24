package edu.farmingdale.mazegame;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Car
{
    private double x, y;
    private double width, height;
    private double angle; // degrees: 0=right, 90=down, 180=left, -90=up

    public Car(double startX, double startY, double width, double height)
    {
        this.x = startX;
        this.y = startY;
        this.width = width;
        this.height = height;
        this.angle = 0;
    }

    public void draw(GraphicsContext gc)
    {
        gc.save();
        gc.translate(x + width / 2, y + height / 2);
        gc.rotate(angle);

        double w = width;
        double h = height;

        // ── Shadow / ground line
        gc.setFill(Color.rgb(0, 0, 0, 0.18));
        gc.fillOval(-w * 0.45, h * 0.35, w * 0.9, h * 0.25);

        double[] bodyX = {-w * 0.48, w * 0.48, w * 0.48, w * 0.20, -w * 0.28, -w * 0.48};
        double[] bodyY = {h * 0.30, h * 0.30, -h * 0.05, -h * 0.48, -h * 0.48, -h * 0.05};

        gc.setFill(Color.rgb(70, 40, 160));   // deep violet-blue
        gc.fillPolygon(bodyX, bodyY, bodyX.length);

        gc.setStroke(Color.rgb(40, 20, 100));
        gc.setLineWidth(1.2);
        gc.strokePolygon(bodyX, bodyY, bodyX.length);

        double[] roofX = {-w * 0.26, w * 0.18, w * 0.14, -w * 0.22};
        double[] roofY = {-h * 0.08, -h * 0.08, -h * 0.44, -h * 0.44};

        gc.setFill(Color.rgb(95, 55, 190));
        gc.fillPolygon(roofX, roofY, roofX.length);

        double[] windX = {-w * 0.24, -w * 0.10, -w * 0.14, -w * 0.26};
        double[] windY = {-h * 0.10, -h * 0.10, -h * 0.42, -h * 0.42};

        gc.setFill(Color.rgb(130, 210, 50, 0.78));
        gc.fillPolygon(windX, windY, windX.length);
        gc.setStroke(Color.rgb(80, 130, 20));
        gc.setLineWidth(0.8);
        gc.strokePolygon(windX, windY, windX.length);

        double[] rearWinX = {w * 0.04, w * 0.17, w * 0.13, w * 0.02};
        double[] rearWinY = {-h * 0.10, -h * 0.10, -h * 0.42, -h * 0.42};

        gc.setFill(Color.rgb(130, 210, 50, 0.78));
        gc.fillPolygon(rearWinX, rearWinY, rearWinX.length);
        gc.setStroke(Color.rgb(80, 130, 20));
        gc.strokePolygon(rearWinX, rearWinY, rearWinX.length);

        gc.setStroke(Color.rgb(50, 25, 120));
        gc.setLineWidth(1.0);
        gc.strokeLine(-w * 0.48, -h * 0.05, -w * 0.28, -h * 0.05);

        gc.setFill(Color.rgb(255, 240, 150));
        gc.fillOval(-w * 0.50, -h * 0.03, w * 0.08, h * 0.10);
        gc.setStroke(Color.rgb(180, 160, 80));
        gc.setLineWidth(0.8);
        gc.strokeOval(-w * 0.50, -h * 0.03, w * 0.08, h * 0.10);

        gc.setFill(Color.rgb(220, 40, 40));
        gc.fillOval(w * 0.42, -h * 0.03, w * 0.07, h * 0.09);
        gc.setStroke(Color.rgb(140, 20, 20));
        gc.strokeOval(w * 0.42, -h * 0.03, w * 0.07, h * 0.09);

        double wr = h * 0.42;   // wheel radius
        double wa = h * 0.15;   // axle offset from bottom

        double rwx = w * 0.28;
        double rwy = h * 0.30 - wa;
        drawWheel(gc, rwx, rwy, wr);

        double fwx = -w * 0.28;
        double fwy = h * 0.30 - wa;
        drawWheel(gc, fwx, fwy, wr);

        gc.restore();
    }

    private void drawWheel(GraphicsContext gc, double cx, double cy, double r)
    {
        // Tire
        gc.setFill(Color.rgb(25, 25, 25));
        gc.fillOval(cx - r, cy - r, r * 2, r * 2);

        // Rim
        gc.setFill(Color.rgb(200, 200, 210));
        gc.fillOval(cx - r * 0.55, cy - r * 0.55, r * 1.1, r * 1.1);

        // Hub cap
        gc.setFill(Color.rgb(140, 130, 160));
        gc.fillOval(cx - r * 0.22, cy - r * 0.22, r * 0.44, r * 0.44);

        // Spoke lines
        gc.setStroke(Color.rgb(160, 150, 175));
        gc.setLineWidth(0.8);
        gc.strokeLine(cx - r * 0.5, cy, cx + r * 0.5, cy);
        gc.strokeLine(cx, cy - r * 0.5, cx, cy + r * 0.5);
    }

    /** Update position directly (called from Maze after collision check). */
    public void setPosition(double newX, double newY)
    {
        this.x = newX;
        this.y = newY;
    }

    /** Update heading based on movement direction. */
    public void updateAngle(double dx, double dy)
    {
        if      (dx > 0) angle = 0;
        else if (dx < 0) angle = 180;
        else if (dy > 0) angle = 90;
        else if (dy < 0) angle = -90;
    }

    // Legacy move() kept for compatibility
    public void move(double dx, double dy)
    {
        x += dx;
        y += dy;
        updateAngle(dx, dy);
    }

    public double getX()      { return x; }
    public double getY()      { return y; }
    public double getWidth()  { return width; }
    public double getHeight() { return height; }
}
