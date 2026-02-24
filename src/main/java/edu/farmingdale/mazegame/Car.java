package edu.farmingdale.mazegame;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Car
{
    // State
    private double x, y;
    private final double width, height;

    // degrees
    private double angle = 0;

    // Colors
    private static final Color SHADOW        = Color.rgb(0, 0, 0, 0.18);
    private static final Color BODY_FILL     = Color.rgb(70, 40, 160);
    private static final Color BODY_STROKE   = Color.rgb(40, 20, 100);
    private static final Color ROOF_FILL     = Color.rgb(95, 55, 190);

    private static final Color GLASS_FILL    = Color.rgb(130, 210, 50, 0.78);
    private static final Color GLASS_STROKE  = Color.rgb(80, 130, 20);

    private static final Color HOOD_LINE     = Color.rgb(50, 25, 120);

    private static final Color HEAD_FILL     = Color.rgb(255, 240, 150);
    private static final Color HEAD_STROKE   = Color.rgb(180, 160, 80);

    private static final Color TAIL_FILL     = Color.rgb(220, 40, 40);
    private static final Color TAIL_STROKE   = Color.rgb(140, 20, 20);

    private static final Color TIRE_FILL     = Color.rgb(25, 25, 25);
    private static final Color RIM_FILL      = Color.rgb(200, 200, 210);
    private static final Color HUB_FILL      = Color.rgb(140, 130, 160);
    private static final Color SPOKE_STROKE  = Color.rgb(160, 150, 175);

    // Constructor
    public Car(double startX, double startY, double width, double height)
    {
        this.x = startX;
        this.y = startY;
        this.width = width;
        this.height = height;
    }

    // Rendering
    public void draw(GraphicsContext gc)
    {
        gc.save();
        gc.translate(x + width / 2, y + height / 2);
        gc.rotate(angle);

        final double w = width;
        final double h = height;

        drawShadow(gc, w, h);
        drawBody(gc, w, h);
        drawRoof(gc, w, h);
        drawWindows(gc, w, h);
        drawDetails(gc, w, h);
        drawLights(gc, w, h);
        drawWheels(gc, w, h);

        gc.restore();
    }

    private void drawShadow(GraphicsContext gc, double w, double h)
    {
        gc.setFill(SHADOW);
        gc.fillOval(-w * 0.45, h * 0.35, w * 0.9, h * 0.25);
    }

    private void drawBody(GraphicsContext gc, double w, double h)
    {
        double[] bodyX = {-w * 0.48,  w * 0.48,  w * 0.48,  w * 0.20, -w * 0.28, -w * 0.48};
        double[] bodyY = { h * 0.30,  h * 0.30, -h * 0.05, -h * 0.48, -h * 0.48, -h * 0.05};

        gc.setFill(BODY_FILL);
        gc.fillPolygon(bodyX, bodyY, bodyX.length);

        gc.setStroke(BODY_STROKE);
        gc.setLineWidth(1.2);
        gc.strokePolygon(bodyX, bodyY, bodyX.length);
    }

    private void drawRoof(GraphicsContext gc, double w, double h)
    {
        double[] roofX = {-w * 0.26,  w * 0.18,  w * 0.14, -w * 0.22};
        double[] roofY = {-h * 0.08, -h * 0.08, -h * 0.44, -h * 0.44};

        gc.setFill(ROOF_FILL);
        gc.fillPolygon(roofX, roofY, roofX.length);
    }

    private void drawWindows(GraphicsContext gc, double w, double h)
    {
        double[] windX = {-w * 0.24, -w * 0.10, -w * 0.14, -w * 0.26};
        double[] windY = {-h * 0.10, -h * 0.10, -h * 0.42, -h * 0.42};
        fillAndStrokePoly(gc, windX, windY, GLASS_FILL, GLASS_STROKE, 0.8);

        double[] rearX = { w * 0.04,  w * 0.17,  w * 0.13,  w * 0.02};
        double[] rearY = {-h * 0.10, -h * 0.10, -h * 0.42, -h * 0.42};
        fillAndStrokePoly(gc, rearX, rearY, GLASS_FILL, GLASS_STROKE, 0.8);
    }

    private void drawDetails(GraphicsContext gc, double w, double h)
    {
        gc.setStroke(HOOD_LINE);
        gc.setLineWidth(1.0);
        gc.strokeLine(-w * 0.48, -h * 0.05, -w * 0.28, -h * 0.05);
    }

    private void drawLights(GraphicsContext gc, double w, double h)
    {
        // Headlight
        gc.setFill(HEAD_FILL);
        gc.fillOval(-w * 0.50, -h * 0.03, w * 0.08, h * 0.10);
        gc.setStroke(HEAD_STROKE);
        gc.setLineWidth(0.8);
        gc.strokeOval(-w * 0.50, -h * 0.03, w * 0.08, h * 0.10);

        // Tail light
        gc.setFill(TAIL_FILL);
        gc.fillOval(w * 0.42, -h * 0.03, w * 0.07, h * 0.09);
        gc.setStroke(TAIL_STROKE);
        gc.setLineWidth(0.8);
        gc.strokeOval(w * 0.42, -h * 0.03, w * 0.07, h * 0.09);
    }

    private void drawWheels(GraphicsContext gc, double w, double h)
    {
        double r  = h * 0.42; // wheel radius
        double wa = h * 0.15; // axle offset from bottom

        drawWheel(gc,  w * 0.28, h * 0.30 - wa, r);  // rear
        drawWheel(gc, -w * 0.28, h * 0.30 - wa, r);  // front
    }

    private void drawWheel(GraphicsContext gc, double cx, double cy, double r)
    {
        gc.setFill(TIRE_FILL);
        gc.fillOval(cx - r, cy - r, r * 2, r * 2);

        gc.setFill(RIM_FILL);
        gc.fillOval(cx - r * 0.55, cy - r * 0.55, r * 1.1, r * 1.1);

        gc.setFill(HUB_FILL);
        gc.fillOval(cx - r * 0.22, cy - r * 0.22, r * 0.44, r * 0.44);

        gc.setStroke(SPOKE_STROKE);
        gc.setLineWidth(0.8);
        gc.strokeLine(cx - r * 0.5, cy, cx + r * 0.5, cy);
        gc.strokeLine(cx, cy - r * 0.5, cx, cy + r * 0.5);
    }

    private void fillAndStrokePoly(GraphicsContext gc, double[] xs, double[] ys, Color fill, Color stroke, double strokeWidth)
    {
        gc.setFill(fill);
        gc.fillPolygon(xs, ys, xs.length);
        gc.setStroke(stroke);
        gc.setLineWidth(strokeWidth);
        gc.strokePolygon(xs, ys, xs.length);
    }

    // Movement
    public void setPosition(double newX, double newY)
    {
        this.x = newX;
        this.y = newY;
    }

    public void updateAngle(double dx, double dy)
    {
        if      (dx > 0) angle = 0;
        else if (dx < 0) angle = 180;
        else if (dy > 0) angle = 90;
        else if (dy < 0) angle = -90;
    }
}
