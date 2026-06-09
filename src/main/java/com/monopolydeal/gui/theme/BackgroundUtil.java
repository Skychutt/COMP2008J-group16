package com.monopolydeal.gui.theme;

import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.io.InputStream;

/**
 * Loads the main-menu background image and provides JavaFX Background helpers.
 */
public final class BackgroundUtil {

    public static final String MAIN_BACKGROUND_PATH = "/Card_Library/Background_graph/MainBackground.png";

    private static final Image MAIN_BACKGROUND = loadImage(MAIN_BACKGROUND_PATH);

    private BackgroundUtil() {}

    public static Image getMainBackground() {
        return MAIN_BACKGROUND;
    }

    /**
     * Load a JavaFX Image from an absolute classpath path (leading slash).
     */
    public static Image loadImage(String classpathPath) {
        try (InputStream in = BackgroundUtil.class.getResourceAsStream(classpathPath)) {
            if (in != null) {
                return new Image(in);
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    /**
     * Apply a cover-style background (CSS background-size: cover equivalent) to a Region.
     * The image scales to fill the region while preserving aspect ratio.
     */
    public static void applyCoverBackground(Region region, Image image) {
        if (image == null) {
            region.setBackground(Background.EMPTY);
            return;
        }
        BackgroundImage bg = new BackgroundImage(
                image,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                // cover = scale to fill, cropping if needed
                new BackgroundSize(100, 100, true, true, false, true)
        );
        region.setBackground(new Background(bg));
    }
}
