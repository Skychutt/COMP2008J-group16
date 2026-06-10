package com.monopolydeal.gui.board;

import com.monopolydeal.gui.theme.UITheme;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

/**
 * In-game table backdrop: green felt inside a wooden frame, drawn on a Canvas.
 * A {@link BorderPane} overlaid on top provides the layout slots used by {@link GameFrame}.
 */
public class TableSurfacePanel extends StackPane {

    private final Canvas canvas;
    private final BorderPane contentPane;

    public TableSurfacePanel() {
        canvas = new Canvas();
        contentPane = new BorderPane();
        contentPane.setBackground(javafx.scene.layout.Background.EMPTY);
        contentPane.setPadding(new javafx.geometry.Insets(20, 24, 20, 24));

        // The canvas fills the StackPane; content sits on top
        getChildren().addAll(canvas, contentPane);

        // Bind canvas size to this pane so the felt redraws on resize
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        widthProperty().addListener((obs, ov, nv)  -> drawFelt());
        heightProperty().addListener((obs, ov, nv) -> drawFelt());
    }

    /** The BorderPane used to position NORTH / SOUTH / EAST / WEST / CENTER children. */
    public BorderPane getContentPane() {
        return contentPane;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Drawing
    // ─────────────────────────────────────────────────────────────────────────

    private void drawFelt() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        int frame = (int) Math.max(14, Math.min(w, h) / 28);

        // Wood outer
        gc.setFill(UITheme.WOOD_OUTER);
        gc.fillRoundRect(0, 0, w, h, 34, 34);

        // Wood inner
        gc.setFill(UITheme.WOOD_INNER);
        gc.fillRoundRect(frame / 2.0, frame / 2.0, w - frame, h - frame, 28, 28);

        // Felt trapezoid
        double leftTop    = frame + frame / 2.0;
        double rightTop   = w - leftTop;
        double leftBottom = frame / 2.0;
        double rightBottom = w - leftBottom;
        double topY    = frame + 2;
        double bottomY = h - frame - 2;

        double[] xs = { leftTop, rightTop, rightBottom, leftBottom };
        double[] ys = { topY,    topY,     bottomY,     bottomY    };

        LinearGradient feltGradient = new LinearGradient(
                0, topY, 0, bottomY,
                false, CycleMethod.NO_CYCLE,
                new Stop(0, UITheme.TABLE_FELT_TOP),
                new Stop(1, UITheme.TABLE_FELT_BOTTOM)
        );
        gc.setFill(feltGradient);
        gc.fillPolygon(xs, ys, 4);

        // Inner shadow overlay
        gc.setFill(Color.rgb(0, 0, 0, 0.137)); // ~35/255
        gc.fillRoundRect(
                frame + 4, topY + 4,
                w - (frame + 4) * 2,
                h - (frame + 12) * 2,
                26, 26
        );
    }
}
