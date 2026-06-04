package com.monopolydeal.gui;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * High-quality image scaling for Swing (avoids blurry {@link Image#getScaledInstance} results).
 */
public final class ImageScaleUtil {

    private ImageScaleUtil() {
    }

    /**
     * Scales to fit within max width/height while keeping aspect ratio.
     */
    public static BufferedImage scaleToFit(Image source, int maxWidth, int maxHeight) {
        if (source == null || maxWidth <= 0 || maxHeight <= 0) {
            return null;
        }
        BufferedImage src = toBufferedImage(source);
        int srcW = src.getWidth();
        int srcH = src.getHeight();
        if (srcW <= 0 || srcH <= 0) {
            return null;
        }

        double scale = Math.min((double) maxWidth / srcW, (double) maxHeight / srcH);
        if (scale > 1.0) {
            scale = 1.0;
        }
        int targetW = Math.max(1, (int) Math.round(srcW * scale));
        int targetH = Math.max(1, (int) Math.round(srcH * scale));
        return scaleExact(src, targetW, targetH);
    }

    public static BufferedImage scaleExact(Image source, int targetWidth, int targetHeight) {
        if (source == null || targetWidth <= 0 || targetHeight <= 0) {
            return null;
        }
        BufferedImage src = toBufferedImage(source);
        BufferedImage dest = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = dest.createGraphics();
        enableQuality(g2);
        g2.drawImage(src, 0, 0, targetWidth, targetHeight, null);
        g2.dispose();
        return dest;
    }

    public static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage) image;
        }
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        if (w <= 0 || h <= 0) {
            w = 1;
            h = 1;
        }
        BufferedImage buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = buffered.createGraphics();
        enableQuality(g2);
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return buffered;
    }

    public static void enableQuality(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    public static BufferedImage rotate180(BufferedImage source) {
        if (source == null) {
            return null;
        }
        int w = source.getWidth();
        int h = source.getHeight();
        BufferedImage dest = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = dest.createGraphics();
        enableQuality(g2);
        g2.drawImage(source, w, 0, 0, h, 0, 0, w, h, null);
        g2.dispose();
        return dest;
    }
}
