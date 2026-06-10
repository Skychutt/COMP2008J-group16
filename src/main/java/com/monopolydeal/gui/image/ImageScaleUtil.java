package com.monopolydeal.gui.image;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * High-quality BufferedImage scaling utilities.
 * JavaFX images are obtained via {@link #toFXImage(BufferedImage)}.
 */
public final class ImageScaleUtil {

    private ImageScaleUtil() {}

    /** Scale to fit within max bounds while preserving aspect ratio (never upscales). */
    public static BufferedImage scaleToFit(java.awt.Image source, int maxWidth, int maxHeight) {
        if (source == null || maxWidth <= 0 || maxHeight <= 0) {
            return null;
        }
        BufferedImage src = toBufferedImage(source);
        int srcW = src.getWidth();
        int srcH = src.getHeight();
        if (srcW <= 0 || srcH <= 0) return null;

        double scale = Math.min((double) maxWidth / srcW, (double) maxHeight / srcH);
        if (scale > 1.0) scale = 1.0;
        int tw = Math.max(1, (int) Math.round(srcW * scale));
        int th = Math.max(1, (int) Math.round(srcH * scale));
        return scaleExact(src, tw, th, true);
    }

    /** Scale to exact target dimensions (may change aspect ratio). */
    public static BufferedImage scaleExact(java.awt.Image source, int targetWidth, int targetHeight) {
        return scaleExact(source, targetWidth, targetHeight, false);
    }

    private static BufferedImage scaleExact(java.awt.Image source, int tw, int th, boolean transparent) {
        if (source == null || tw <= 0 || th <= 0) return null;
        BufferedImage src = toBufferedImage(source);
        int type = (transparent || hasAlpha(src))
                ? BufferedImage.TYPE_INT_ARGB
                : BufferedImage.TYPE_INT_RGB;
        BufferedImage dest = new BufferedImage(tw, th, type);
        Graphics2D g2 = dest.createGraphics();
        enableQuality(g2);
        if (type == BufferedImage.TYPE_INT_ARGB) {
            g2.setComposite(AlphaComposite.Clear);
            g2.fillRect(0, 0, tw, th);
            g2.setComposite(AlphaComposite.SrcOver);
        }
        g2.drawImage(src, 0, 0, tw, th, null);
        g2.dispose();
        return dest;
    }

    /** Rotate 180 degrees. */
    public static BufferedImage rotate180(BufferedImage source) {
        if (source == null) return null;
        int w = source.getWidth();
        int h = source.getHeight();
        int type = hasAlpha(source) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage dest = new BufferedImage(w, h, type);
        Graphics2D g2 = dest.createGraphics();
        enableQuality(g2);
        if (type == BufferedImage.TYPE_INT_ARGB) {
            g2.setComposite(AlphaComposite.Clear);
            g2.fillRect(0, 0, w, h);
            g2.setComposite(AlphaComposite.SrcOver);
        }
        g2.drawImage(source, w, h, 0, 0, 0, 0, w, h, null);
        g2.dispose();
        return dest;
    }

    /** Convert any AWT Image to BufferedImage. */
    public static BufferedImage toBufferedImage(java.awt.Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        }
        int w = Math.max(1, image.getWidth(null));
        int h = Math.max(1, image.getHeight(null));
        BufferedImage buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = buffered.createGraphics();
        enableQuality(g2);
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return buffered;
    }

    /**
     * Convert a BufferedImage to a JavaFX Image using SwingFXUtils.
     * Returns null if source is null.
     */
    public static Image toFXImage(BufferedImage buffered) {
        if (buffered == null) return null;
        return SwingFXUtils.toFXImage(buffered, null);
    }

    /** Preferred Dimension to display an image at max bounds without stretching. */
    public static java.awt.Dimension sizeToFit(java.awt.Image image, int maxWidth, int maxHeight) {
        if (image == null || maxWidth <= 0 || maxHeight <= 0) {
            return new java.awt.Dimension(maxWidth, maxHeight);
        }
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        if (w <= 0 || h <= 0) return new java.awt.Dimension(maxWidth, maxHeight);
        double scale = Math.min((double) maxWidth / w, (double) maxHeight / h);
        if (scale > 1.0) scale = 1.0;
        return new java.awt.Dimension(
                Math.max(1, (int) Math.round(w * scale)),
                Math.max(1, (int) Math.round(h * scale)));
    }

    public static void enableQuality(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private static boolean hasAlpha(BufferedImage image) {
        return image != null && image.getColorModel().hasAlpha();
    }
}
