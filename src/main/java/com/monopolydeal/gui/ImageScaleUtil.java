package com.monopolydeal.gui;

import java.awt.AlphaComposite;
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
        return scaleExact(src, targetW, targetH, true);
    }

    public static BufferedImage scaleExact(Image source, int targetWidth, int targetHeight) {
        return scaleExact(source, targetWidth, targetHeight, false);
    }

    private static BufferedImage scaleExact(Image source, int targetWidth, int targetHeight, boolean transparent) {
        if (source == null || targetWidth <= 0 || targetHeight <= 0) {
            return null;
        }
        BufferedImage src = toBufferedImage(source);
        int type = transparent || hasAlpha(src) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage dest = new BufferedImage(targetWidth, targetHeight, type);
        Graphics2D g2 = dest.createGraphics();
        enableQuality(g2);
        if (type == BufferedImage.TYPE_INT_ARGB) {
            g2.setComposite(AlphaComposite.Clear);
            g2.fillRect(0, 0, targetWidth, targetHeight);
            g2.setComposite(AlphaComposite.SrcOver);
        }
        g2.drawImage(src, 0, 0, targetWidth, targetHeight, null);
        g2.dispose();
        return dest;
    }

    public static BufferedImage toBufferedImage(Image image) {
        if (image instanceof BufferedImage) {
            BufferedImage buffered = (BufferedImage) image;
            if (hasAlpha(buffered)) {
                return buffered;
            }
        }
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        if (w <= 0 || h <= 0) {
            w = 1;
            h = 1;
        }
        boolean argb = image instanceof BufferedImage && hasAlpha((BufferedImage) image);
        BufferedImage buffered = new BufferedImage(w, h, argb ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = buffered.createGraphics();
        enableQuality(g2);
        if (argb) {
            g2.setComposite(AlphaComposite.Clear);
            g2.fillRect(0, 0, w, h);
            g2.setComposite(AlphaComposite.SrcOver);
        }
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return buffered;
    }

    private static boolean hasAlpha(BufferedImage image) {
        return image != null && image.getColorModel().hasAlpha();
    }

    /** Preferred size to show the image at max bounds without stretching the layout box. */
    public static java.awt.Dimension sizeToFit(Image image, int maxWidth, int maxHeight) {
        if (image == null || maxWidth <= 0 || maxHeight <= 0) {
            return new java.awt.Dimension(maxWidth, maxHeight);
        }
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        if (w <= 0 || h <= 0) {
            return new java.awt.Dimension(maxWidth, maxHeight);
        }
        double scale = Math.min((double) maxWidth / w, (double) maxHeight / h);
        if (scale > 1.0) {
            scale = 1.0;
        }
        return new java.awt.Dimension(
                Math.max(1, (int) Math.round(w * scale)),
                Math.max(1, (int) Math.round(h * scale)));
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
