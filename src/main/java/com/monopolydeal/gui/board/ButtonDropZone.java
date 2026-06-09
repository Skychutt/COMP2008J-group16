package com.monopolydeal.gui.board;

import com.monopolydeal.gui.image.ImageActionButton;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * JavaFX drop-target panel that paints a button image from {@code Card_Library/Button_graph/}.
 * Drag-and-drop event handlers (setOnDragOver / setOnDragDropped) are attached externally
 * by {@link PlayerPanel}.
 */
public class ButtonDropZone extends StackPane {

    private final ImageView imageView;
    private final javafx.scene.shape.Rectangle highlightRect;
    private final Label hoverLabel;

    private boolean highlight = false;

    public ButtonDropZone(String imageFileName, int width, int height) {
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);

        Image img = ImageActionButton.loadButtonImage(imageFileName);
        imageView = new ImageView(img);
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        imageView.setPreserveRatio(true);

        // Semi-transparent yellow overlay shown when a dragged card hovers over this zone
        highlightRect = new javafx.scene.shape.Rectangle(width, height,
                Color.rgb(255, 255, 180, 0.35));
        highlightRect.setVisible(false);

        // Hover text label
        hoverLabel = new Label();
        hoverLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        hoverLabel.setTextFill(Color.rgb(255, 248, 210));
        hoverLabel.setStyle(
            "-fx-background-color: rgba(0,0,0,0.63);" +
            "-fx-padding: 3 8 3 8;" +
            "-fx-background-radius: 3px;"
        );
        hoverLabel.setVisible(false);

        getChildren().addAll(imageView, highlightRect, hoverLabel);
        StackPane.setAlignment(hoverLabel, Pos.CENTER);
        setStyle("-fx-background-color: transparent;");
    }

    /** Shifts the image downward within the drop zone (positive = down). */
    public void setImageOffsetY(int offsetY) {
        imageView.setTranslateY(Math.max(0, offsetY));
    }

    public void setHighlight(boolean highlight) {
        if (this.highlight == highlight) return;
        this.highlight = highlight;
        highlightRect.setVisible(highlight);
    }

    public void setHoverText(String text) {
        if (text == null || text.isBlank()) {
            hoverLabel.setVisible(false);
        } else {
            hoverLabel.setText(text);
            hoverLabel.setVisible(true);
        }
    }
}
