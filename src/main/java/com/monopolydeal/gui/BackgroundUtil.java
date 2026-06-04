package com.monopolydeal.gui;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.io.IOException;
import java.io.InputStream;

/**
 * Loads and paints the main menu background image from classpath resources.
 */
public final class BackgroundUtil {

    public static final String MAIN_BACKGROUND_PATH = "/Card_Library/Background_graph/MainBackground.png";

    private static final Image MAIN_BACKGROUND = loadImage(MAIN_BACKGROUND_PATH);

    private BackgroundUtil() {
    }

    public static Image getMainBackground() {
        return MAIN_BACKGROUND;
    }

    public static Image loadImage(String classpathPath) {
        try (InputStream in = BackgroundUtil.class.getResourceAsStream(classpathPath)) {
            if (in == null) {
                return null;
            }
            return ImageIO.read(in);
        } catch (IOException ignored) {
            return null;
        }
    }

    /**
     * Draws the image scaled to cover the target area (center crop).
     */
    public static void paintCover(Graphics2D g2, Image image, int panelW, int panelH) {
        if (image == null || panelW <= 0 || panelH <= 0) {
            return;
        }
        int imgW = image.getWidth(null);
        int imgH = image.getHeight(null);
        if (imgW <= 0 || imgH <= 0) {
            return;
        }

        double scale = Math.max((double) panelW / imgW, (double) panelH / imgH);
        int drawW = (int) Math.round(imgW * scale);
        int drawH = (int) Math.round(imgH * scale);
        int x = (panelW - drawW) / 2;
        int y = (panelH - drawH) / 2;

        g2.drawImage(image, x, y, drawW, drawH, null);
    }

    public static void enableQuality(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }
}
