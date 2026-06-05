package com.monopolydeal.gui;

import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * JavaFX Button that displays a transparent PNG from {@code Card_Library/Button_graph/}.
 * Visual states (ENABLED / WAITING / DISABLED) are conveyed through opacity.
 */
public class ImageActionButton extends Button {

    private static final String BUTTON_PREFIX = "Card_Library/Button_graph/";

    public enum VisualState { ENABLED, WAITING, DISABLED }

    private final ImageView imageView;
    private VisualState visualState = VisualState.ENABLED;

    public ImageActionButton(String imageFileName, int maxWidth, int maxHeight) {
        this(imageFileName, maxWidth, maxHeight, false);
    }

    /**
     * @param fixedSlot when true the button occupies exactly maxWidth × maxHeight and
     *                  the image is scaled to fit inside; when false the button is sized
     *                  to the image's natural fit within the given max bounds.
     */
    public ImageActionButton(String imageFileName, int maxWidth, int maxHeight, boolean fixedSlot) {
        super();
        Image img = loadButtonImage(imageFileName);
        imageView = new ImageView(img);
        imageView.setPreserveRatio(true);

        if (fixedSlot || img == null) {
            imageView.setFitWidth(maxWidth);
            imageView.setFitHeight(maxHeight);
            setPrefSize(maxWidth, maxHeight);
            setMinSize(maxWidth, maxHeight);
            setMaxSize(maxWidth, maxHeight);
        } else {
            // Scale to fit within maxWidth × maxHeight preserving ratio
            double imgW = img.getWidth();
            double imgH = img.getHeight();
            double scale = Math.min(maxWidth / imgW, maxHeight / imgH);
            if (scale > 1.0) scale = 1.0;
            int fw = Math.max(1, (int) Math.round(imgW * scale));
            int fh = Math.max(1, (int) Math.round(imgH * scale));
            imageView.setFitWidth(fw);
            imageView.setFitHeight(fh);
            setPrefSize(fw, fh);
            setMinSize(fw, fh);
            setMaxSize(fw, fh);
        }

        setGraphic(imageView);
        setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-border-width: 0; -fx-cursor: hand;");
        setFocusTraversable(false);
        applyOpacity();
    }

    /** Horizontal pixel shift applied to the image inside the button slot. */
    public void setImageOffsetX(int offsetX) {
        imageView.setTranslateX(offsetX);
    }

    public void setVisualState(VisualState state) {
        if (this.visualState != state) {
            this.visualState = state;
            applyOpacity();
        }
    }

    private void applyOpacity() {
        switch (visualState) {
            case DISABLED:
                imageView.setOpacity(0.45);
                setDisable(true);
                break;
            case WAITING:
                imageView.setOpacity(0.82);
                setDisable(false);
                break;
            default:
                imageView.setOpacity(1.0);
                setDisable(false);
                break;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Image loading — classpath first, then project-root fallback
    // ─────────────────────────────────────────────────────────────────────────

    static Image loadButtonImage(String fileName) {
        String path = BUTTON_PREFIX + fileName;
        try (InputStream in = ImageActionButton.class.getClassLoader().getResourceAsStream(path)) {
            if (in != null) {
                BufferedImage bi = ImageIO.read(in);
                if (bi != null) return ImageScaleUtil.toFXImage(bi);
            }
        } catch (IOException ignored) {}

        java.nio.file.Path local = java.nio.file.Paths.get(path);
        if (java.nio.file.Files.exists(local)) {
            try (InputStream in = new java.io.FileInputStream(local.toFile())) {
                BufferedImage bi = ImageIO.read(in);
                if (bi != null) return ImageScaleUtil.toFXImage(bi);
            } catch (IOException ignored) {}
        }
        return null;
    }
}
