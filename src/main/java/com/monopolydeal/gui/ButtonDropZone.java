package com.monopolydeal.gui;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * Drop target panel that paints a button image from {@code Card_Library/Button-graph/}.
 */
public class ButtonDropZone extends JPanel {

    private static final String BUTTON_PREFIX = "Card_Library/Button-graph/";

    private final Image background;
    private boolean highlight;
    private String hoverText;
    private int imageOffsetY;

    public ButtonDropZone(String imageFileName, int width, int height) {
        setOpaque(false);
        setLayout(null);
        Dimension size = new Dimension(width, height);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        background = loadButtonImage(imageFileName);
    }

    /** Shifts the painted image downward within the drop zone (pixels). */
    public void setImageOffsetY(int imageOffsetY) {
        this.imageOffsetY = Math.max(0, imageOffsetY);
        repaint();
    }

    public void setHighlight(boolean highlight) {
        if (this.highlight != highlight) {
            this.highlight = highlight;
            repaint();
        }
    }

    public void setHoverText(String hoverText) {
        String next = hoverText == null || hoverText.isBlank() ? null : hoverText;
        if ((this.hoverText == null && next == null)
                || (this.hoverText != null && this.hoverText.equals(next))) {
            return;
        }
        this.hoverText = next;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        ImageScaleUtil.enableQuality(g2);

        if (background != null && getWidth() > 0 && getHeight() > 0) {
            int padTop = imageOffsetY;
            int padBottom = 4;
            int availH = Math.max(1, getHeight() - padTop - padBottom);
            BufferedImage scaled = ImageScaleUtil.scaleToFit(background, getWidth(), availH);
            int x = (getWidth() - scaled.getWidth()) / 2;
            int y = padTop + (availH - scaled.getHeight()) / 2;
            g2.drawImage(scaled, x, y, null);
            if (highlight) {
                g2.setColor(new Color(255, 255, 180, 90));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        } else {
            g2.setColor(UITheme.BANK_ZONE);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

        if (highlight && hoverText != null) {
            Font font = UITheme.FONT_SUBTITLE.deriveFont(Font.BOLD, 13f);
            g2.setFont(font);
            int textW = g2.getFontMetrics().stringWidth(hoverText);
            int textX = Math.max(4, (getWidth() - textW) / 2);
            int textY = getHeight() / 2 + 5;
            g2.setColor(new Color(0, 0, 0, 160));
            g2.drawString(hoverText, textX + 1, textY + 1);
            g2.setColor(new Color(255, 248, 210));
            g2.drawString(hoverText, textX, textY);
        }
        g2.dispose();
    }

    private static Image loadButtonImage(String fileName) {
        String path = BUTTON_PREFIX + fileName;
        try (InputStream in = ButtonDropZone.class.getClassLoader().getResourceAsStream(path)) {
            if (in != null) {
                return ImageIO.read(in);
            }
        } catch (IOException ignored) {
        }
        return null;
    }
}
