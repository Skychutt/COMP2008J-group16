package com.monopolydeal.gui;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Game rules dialog: official quick-start rule cards + English rules summary.
 */
public final class GameRulesDialog {

    private static final String RULES_LIBRARY = "Card_Library/RulesCard/";
    private static final int CARD_MAX_W = 680;

    private static final String[] RULE_CARD_FILES = {
            "quick-start-#1-rules-cards.jpg",
            "quick-start-#2-rules-cards.jpg"
    };
    private static final String[] RULE_CARD_CAPTIONS = {"Rule card 1", "Rule card 2"};

    private static final String RULES_TEXT =
            "RULES INTRODUCTION\n\n"
            + "OBJECTIVE\n"
            + "Be the first player to collect 3 complete property sets of different colors.\n\n"
            + "SETUP\n"
            + "Each player starts with 5 cards. The deck has 110 cards.\n\n"
            + "TURN STRUCTURE\n"
            + "1) Draw Phase: Draw 2 cards (draw 5 if hand is empty at turn start).\n"
            + "2) Play Phase: Play up to 3 cards from your hand.\n"
            + "3) End Phase: Discard down to 7 cards if needed, then pass the turn.\n\n"
            + "PAYMENTS\n"
            + "When paying rent or fees, use cards from bank or property area only.\n"
            + "Hand cards cannot be used to pay. Overpayment is not refunded.\n\n"
            + "DEFENSE\n"
            + "Just Say No cancels a hostile action or rent. Does not use an action.\n\n"
            + "BUILDINGS\n"
            + "House and Hotel may only be placed on completed property sets.\n"
            + "They increase rent on that set.\n\n"
            + "ACTION CARDS\n"
            + "Action cards can be played for their effect, or banked as money.\n\n"
            + "See the quick-start rule cards above for the official card reference.";

    private GameRulesDialog() {
    }

    public static void show(Window owner) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle("Game Rules");

        VBox content = buildScrollableContent();

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle(
                "-fx-background-color: " + UITheme.toCssRgba(UITheme.PANEL_BG) + ";" +
                "-fx-border-color: " + UITheme.toCssHex(UITheme.BORDER) + ";" +
                "-fx-border-width: 1px; -fx-border-radius: 4px; -fx-background-radius: 4px;"
        );
        content.prefWidthProperty().bind(scroll.widthProperty());

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + UITheme.toCssRgba(UITheme.PANEL_BG) + ";");
        root.setPadding(new Insets(16));
        root.setCenter(scroll);
        root.setBottom(buildCloseBar(dialog));

        Scene scene = new Scene(root, 760, 740);
        dialog.setScene(scene);
        dialog.setMinWidth(720);
        dialog.setMinHeight(640);
        dialog.showAndWait();
    }

    private static VBox buildScrollableContent() {
        Label cardsHeading = new Label("Official Quick-Start Rule Cards");
        cardsHeading.setFont(UITheme.FONT_SUBTITLE);
        cardsHeading.setTextFill(Color.BLACK);

        VBox cardsColumn = new VBox(18);
        cardsColumn.setAlignment(Pos.TOP_CENTER);
        cardsColumn.setFillWidth(true);
        for (int i = 0; i < RULE_CARD_FILES.length; i++) {
            cardsColumn.getChildren().add(buildRuleCardTile(RULE_CARD_FILES[i], RULE_CARD_CAPTIONS[i]));
        }

        Label rulesHeading = new Label("Rules Introduction");
        rulesHeading.setFont(UITheme.FONT_SUBTITLE);
        rulesHeading.setTextFill(UITheme.TEXT_SUB);

        Text rulesBody = new Text(RULES_TEXT);
        rulesBody.setFont(UITheme.FONT_BODY);
        rulesBody.setFill(UITheme.TEXT_MAIN);
        rulesBody.wrappingWidthProperty().bind(
                Bindings.subtract(cardsColumn.widthProperty(), 24));

        VBox content = new VBox(14, cardsHeading, cardsColumn, rulesHeading, rulesBody);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(4, 12, 16, 12));
        return content;
    }

    private static VBox buildRuleCardTile(String fileName, String caption) {
        Image image = loadRulesCardImage(fileName);
        if (image == null) {
            Label placeholder = new Label("<image not found>\n" + fileName);
            placeholder.setMinWidth(CARD_MAX_W);
            placeholder.setAlignment(Pos.CENTER);
            Label captionLabel = new Label(caption);
            captionLabel.setFont(UITheme.FONT_BODY);
            captionLabel.setTextFill(Color.BLACK);
            VBox tile = new VBox(6, placeholder, captionLabel);
            tile.setAlignment(Pos.CENTER);
            tile.setStyle(UITheme.softPanelStyle());
            tile.setPadding(new Insets(8));
            return tile;
        }

        ImageView imgView = new ImageView(image);
        imgView.setPreserveRatio(true);
        imgView.setSmooth(true);
        double scale = Math.min(1.0, (double) CARD_MAX_W / image.getWidth());
        imgView.setFitWidth(image.getWidth() * scale);
        imgView.setFitHeight(image.getHeight() * scale);

        Label captionLabel = new Label(caption);
        captionLabel.setFont(UITheme.FONT_BODY);
        captionLabel.setTextFill(Color.BLACK);

        VBox tile = new VBox(6, imgView, captionLabel);
        tile.setAlignment(Pos.CENTER);
        tile.setStyle(UITheme.softPanelStyle());
        tile.setPadding(new Insets(8));
        tile.setMaxWidth(CARD_MAX_W + 16);
        return tile;
    }

    private static HBox buildCloseBar(Stage dialog) {
        Button close = new Button("Close");
        UITheme.styleMenuButton(close);
        close.setOnAction(e -> dialog.close());

        HBox bar = new HBox(close);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(10, 0, 0, 0));
        return bar;
    }

    private static Image loadRulesCardImage(String fileName) {
        String resourcePath = RULES_LIBRARY + fileName;
        try (InputStream in = Thread.currentThread()
                .getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (in != null) {
                BufferedImage bi = ImageIO.read(in);
                if (bi != null) {
                    return ImageScaleUtil.toFXImage(bi);
                }
            }
        } catch (IOException ignored) {
        }

        Path localPath = Paths.get("src", "main", "resources", "Card_Library", "RulesCard", fileName);
        if (Files.exists(localPath)) {
            try (InputStream in = new FileInputStream(localPath.toFile())) {
                BufferedImage bi = ImageIO.read(in);
                if (bi != null) {
                    return ImageScaleUtil.toFXImage(bi);
                }
            } catch (IOException ignored) {
            }
        }

        Path rootPath = Paths.get("Card_Library", "RulesCard", fileName);
        if (Files.exists(rootPath)) {
            try (InputStream in = new FileInputStream(rootPath.toFile())) {
                BufferedImage bi = ImageIO.read(in);
                if (bi != null) {
                    return ImageScaleUtil.toFXImage(bi);
                }
            } catch (IOException ignored) {
            }
        }
        return null;
    }
}
