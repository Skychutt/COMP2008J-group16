package com.monopolydeal.gui;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;

/**
 * Paints a green card-table background with a wood frame and subtle perspective.
 */
public class TableSurfacePanel extends JPanel {

    public TableSurfacePanel() {
        setOpaque(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int frame = Math.max(14, Math.min(w, h) / 28);

        // Wood outer frame
        g2.setColor(UITheme.WOOD_OUTER);
        g2.fillRoundRect(0, 0, w, h, 34, 34);

        // Wood inner edge
        g2.setColor(UITheme.WOOD_INNER);
        g2.fillRoundRect(frame / 2, frame / 2, w - frame, h - frame, 28, 28);

        // Felt area with light perspective (wider at bottom).
        int leftTop = frame + frame / 2;
        int rightTop = w - leftTop;
        int leftBottom = frame / 2;
        int rightBottom = w - leftBottom;
        int topY = frame + 2;
        int bottomY = h - frame - 2;

        Polygon felt = new Polygon();
        felt.addPoint(leftTop, topY);
        felt.addPoint(rightTop, topY);
        felt.addPoint(rightBottom, bottomY);
        felt.addPoint(leftBottom, bottomY);

        GradientPaint feltPaint = new GradientPaint(
                0, topY, UITheme.TABLE_FELT_TOP,
                0, bottomY, UITheme.TABLE_FELT_BOTTOM
        );
        g2.setPaint(feltPaint);
        g2.fillPolygon(felt);

        // Soft vignette to strengthen table depth.
        g2.setColor(new Color(0, 0, 0, 35));
        g2.fillRoundRect(frame + 4, topY + 4, w - (frame + 4) * 2, h - (frame + 12) * 2, 26, 26);

        g2.dispose();
    }
}
