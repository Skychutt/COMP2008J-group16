package com.monopolydeal.gui;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * JButton that paints an image from {@code Card_Library/Button-graph/}.
 */
public class ImageActionButton extends JButton {

    private static final String BUTTON_PREFIX = "Card_Library/Button-graph/";

    public enum VisualState {
        ENABLED,
        WAITING,
        DISABLED
    }

    private final Image background;
    private VisualState visualState = VisualState.ENABLED;

    public ImageActionButton(String imageFileName, int width, int height) {
        super();
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        Dimension size = new Dimension(width, height);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        background = loadButtonImage(imageFileName);
    }

    public void setVisualState(VisualState visualState) {
        if (this.visualState != visualState) {
            this.visualState = visualState;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        ImageScaleUtil.enableQuality(g2);

        if (background != null && getWidth() > 0 && getHeight() > 0) {
            BufferedImage scaled = ImageScaleUtil.scaleToFit(background, getWidth(), getHeight());
            int x = (getWidth() - scaled.getWidth()) / 2;
            int y = (getHeight() - scaled.getHeight()) / 2;

            float alpha = 1.0f;
            if (visualState == VisualState.DISABLED) {
                alpha = 0.45f;
            } else if (visualState == VisualState.WAITING) {
                alpha = 0.88f;
            }
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.drawImage(scaled, x, y, null);

            if (visualState == VisualState.WAITING) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
                g2.setColor(new java.awt.Color(255, 230, 120));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        } else {
            g2.setColor(UITheme.BANK_ZONE);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        g2.dispose();
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
