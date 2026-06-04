package com.monopolydeal.gui;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * JButton that paints a transparent PNG from {@code Card_Library/Button-graph/} with no fill behind it.
 */
public class ImageActionButton extends JButton {

    private static final String BUTTON_PREFIX = "Card_Library/Button_graph/";

    public enum VisualState {
        ENABLED,
        WAITING,
        DISABLED
    }

    private final Image background;
    private int imageOffsetX;
    private VisualState visualState = VisualState.ENABLED;

    public ImageActionButton(String imageFileName, int maxWidth, int maxHeight) {
        this(imageFileName, maxWidth, maxHeight, false);
    }

    /** @param fixedSlot if true, use maxWidth x maxHeight slot and center the image (aligns with drop zones). */
    public ImageActionButton(String imageFileName, int maxWidth, int maxHeight, boolean fixedSlot) {
        super();
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setMargin(new Insets(0, 0, 0, 0));
        setBorder(null);
        background = loadButtonImage(imageFileName);
        Dimension size = fixedSlot
                ? new Dimension(maxWidth, maxHeight)
                : ImageScaleUtil.sizeToFit(background, maxWidth, maxHeight);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
    }

    public void setImageOffsetX(int imageOffsetX) {
        this.imageOffsetX = imageOffsetX;
        repaint();
    }

    public void setVisualState(VisualState visualState) {
        if (this.visualState != visualState) {
            this.visualState = visualState;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (background == null || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        ImageScaleUtil.enableQuality(g2);

        BufferedImage scaled = ImageScaleUtil.scaleToFit(background, getWidth(), getHeight());
        if (scaled == null) {
            g2.dispose();
            return;
        }

        int x = (getWidth() - scaled.getWidth()) / 2 + imageOffsetX;
        int y = (getHeight() - scaled.getHeight()) / 2;

        float alpha = 1.0f;
        if (visualState == VisualState.DISABLED) {
            alpha = 0.45f;
        } else if (visualState == VisualState.WAITING) {
            alpha = 0.82f;
        }
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.drawImage(scaled, x, y, null);
        g2.dispose();
    }

    @Override
    public boolean isOpaque() {
        return false;
    }

    private static Image loadButtonImage(String fileName) {
        String path = BUTTON_PREFIX + fileName;
        try (InputStream in = ImageActionButton.class.getClassLoader().getResourceAsStream(path)) {
            if (in != null) {
                return ImageIO.read(in);
            }
        } catch (IOException ignored) {
        }
        return null;
    }
}
