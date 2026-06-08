package com.monopolydeal.gui;

import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.enums.ActionType;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.PropertySet;
import com.monopolydeal.model.card.ActionCard;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.PropertyCard;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Middle property area showing every color lane and the current rent per lane.
 */
public class PropertyAreaPanel extends HBox {

    private static final int CARD_W = 54;
    private static final int CARD_H = 82;
    private static final int CARD_STEP_Y = CARD_H / 3;

    private final GamePanelHost mainFrame;
    private final List<LanePane> lanes = new ArrayList<>();
    private final List<FadeTransition> rentFlashes = new ArrayList<>();

    private Player currentPlayer;
    private boolean interactive;

    public PropertyAreaPanel(GamePanelHost mainFrame) {
        this.mainFrame = mainFrame;

        setSpacing(6);
        setPadding(new Insets(8, 8, 8, 8));
        setAlignment(Pos.TOP_CENTER);
        setStyle("-fx-background-color: transparent; -fx-border-width: 0;");

        for (PropertyType color : displayOrder()) {
            LanePane lane = new LanePane(color);
            lanes.add(lane);
            getChildren().add(lane);
        }
    }

    public void updatePropertyArea(Player player, boolean interactive, int rentMultiplier) {
        this.currentPlayer = player;
        this.interactive = interactive;

        stopRentFlash();

        for (LanePane lane : lanes) {
            PropertySet set = player == null ? null : player.getPropertyArea().getSet(lane.color);
            lane.update(set, Math.max(1, rentMultiplier));
        }
    }

    private void stopRentFlash() {
        for (FadeTransition flash : rentFlashes) {
            flash.stop();
        }
        rentFlashes.clear();
    }

    private void flashRentLabel(Label label) {
        FadeTransition flash = new FadeTransition(Duration.millis(420), label);
        flash.setFromValue(1.0);
        flash.setToValue(0.35);
        flash.setCycleCount(FadeTransition.INDEFINITE);
        flash.setAutoReverse(true);
        flash.play();
        rentFlashes.add(flash);
    }

    private boolean canAcceptPropertyDrop(String rawCardId, PropertyType targetColor) {
        if (currentPlayer == null || !interactive || rawCardId == null) {
            return false;
        }
        try {
            int cardId = Integer.parseInt(rawCardId.trim());
            return mainFrame.canPlacePropertyInColor(cardId, targetColor);
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static List<PropertyType> displayOrder() {
        List<PropertyType> order = new ArrayList<>();
        order.add(PropertyType.BROWN);
        order.add(PropertyType.LIGHTBLUE);
        order.add(PropertyType.PURPLE);
        order.add(PropertyType.ORANGE);
        order.add(PropertyType.RED);
        order.add(PropertyType.YELLOW);
        order.add(PropertyType.GREEN);
        order.add(PropertyType.BLUE);
        order.add(PropertyType.BLACK);
        order.add(PropertyType.LIGHTGREEN);
        return order;
    }

    private static String displayName(PropertyType color) {
        switch (color) {
            case LIGHTBLUE:
                return "Light Blue";
            case PURPLE:
                return "Pink";
            case BLUE:
                return "Dark Blue";
            case BLACK:
                return "Railroad";
            case LIGHTGREEN:
                return "Utility";
            default:
                String raw = color.name().toLowerCase().replace('_', ' ');
                return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
        }
    }

    private static Color textColor(PropertyType color) {
        switch (color) {
            case BROWN:
                return Color.rgb(124, 80, 42);
            case LIGHTBLUE:
                return Color.rgb(74, 142, 212);
            case PURPLE:
                return Color.rgb(201, 74, 145);
            case ORANGE:
                return Color.rgb(219, 124, 37);
            case RED:
                return Color.rgb(204, 67, 67);
            case YELLOW:
                return Color.rgb(200, 156, 42);
            case GREEN:
                return Color.rgb(45, 135, 72);
            case BLUE:
                return Color.rgb(36, 72, 149);
            case BLACK:
                return Color.rgb(64, 64, 64);
            case LIGHTGREEN:
                return Color.rgb(64, 145, 118);
            default:
                return UITheme.TEXT_MAIN;
        }
    }

    private final class LanePane extends VBox {
        private final PropertyType color;
        private final Label lblColor;
        private final Pane cardsPane;
        private final StackPane cardsFrame;
        private final Label lblRent;

        private LanePane(PropertyType color) {
            this.color = color;

            setSpacing(4);
            setAlignment(Pos.TOP_CENTER);
            setPrefWidth(76);
            setMinWidth(76);
            setPadding(new Insets(5, 4, 5, 4));
            setStyle(laneStyle(false));

            lblColor = new Label(titleText(null));
            lblColor.setFont(Font.font("Segoe UI", 9));
            lblColor.setTextFill(textColor(color));
            lblColor.setWrapText(true);
            lblColor.setAlignment(Pos.CENTER);
            lblColor.setMaxWidth(Double.MAX_VALUE);

            cardsPane = new Pane();
            cardsPane.setPrefSize(CARD_W + 8, CARD_H + (CARD_STEP_Y * 2));
            cardsPane.setMinSize(CARD_W + 8, CARD_H + (CARD_STEP_Y * 2));

            cardsFrame = new StackPane(cardsPane);
            cardsFrame.setPrefSize(CARD_W + 12, CARD_H + (CARD_STEP_Y * 2) + 8);
            cardsFrame.setStyle(frameStyle(false));

            lblRent = new Label("0M");
            lblRent.setFont(UITheme.FONT_SUBTITLE);
            lblRent.setTextFill(UITheme.TEXT_MAIN);

            wireDropTarget(cardsFrame);
            getChildren().addAll(lblColor, cardsFrame, lblRent);
        }

        private void update(PropertySet set, int rentMultiplier) {
            cardsPane.getChildren().clear();
            boolean complete = set != null && set.isComplete();
            lblColor.setText(titleText(set));
            setStyle(laneStyle(complete));
            cardsFrame.setStyle(frameStyle(complete));
            lblRent.setTextFill(complete ? Color.rgb(170, 120, 24) : UITheme.TEXT_MAIN);

            int rent = 0;
            if (set != null) {
                rent = totalRent(set) * rentMultiplier;
                List<PropertyCard> cards = set.getCards();
                for (int i = 0; i < cards.size(); i++) {
                    PropertyCard card = cards.get(i);
                    ImageView iv = new ImageView(mainFrame.getImageResolver().getCardIcon(card, CARD_W, CARD_H));
                    iv.setFitWidth(CARD_W);
                    iv.setFitHeight(CARD_H);
                    iv.setPreserveRatio(false);
                    iv.setLayoutX(2);
                    iv.setLayoutY(i * CARD_STEP_Y);
                    cardsPane.getChildren().add(iv);
                }
            }

            lblRent.setText(rent + "M");
            lblRent.setOpacity(1.0);
            if (rentMultiplier > 1 && rent > 0) {
                flashRentLabel(lblRent);
            }
        }

        private int totalRent(PropertySet set) {
            int total = set.getRent();
            for (PropertyCard propertyCard : set.getCards()) {
                for (Card upgrade : propertyCard.getUpgrades()) {
                    if (!(upgrade instanceof ActionCard)) {
                        continue;
                    }
                    ActionType type = ((ActionCard) upgrade).getType();
                    if (type == ActionType.HOUSE) {
                        total += 3;
                    } else if (type == ActionType.HOTEL) {
                        total += 4;
                    }
                }
            }
            return total;
        }

        private String titleText(PropertySet set) {
            int have = set == null ? 0 : set.getCards().size();
            return displayName(color) + " " + have + "/" + requiredCount(color);
        }

        private void wireDropTarget(StackPane cardsFrame) {
            cardsFrame.setOnDragOver(e -> {
                if (e.getDragboard().hasString()
                        && canAcceptPropertyDrop(e.getDragboard().getString(), color)) {
                    e.acceptTransferModes(TransferMode.COPY);
                    setStyle(
                            "-fx-background-color: rgba(255,252,240,0.96);" +
                            "-fx-border-color: " + UITheme.toCssHex(UITheme.BANK_ZONE_BORDER) + ";" +
                            "-fx-border-width: 1px; -fx-border-radius: 6px; -fx-background-radius: 6px;" +
                            "-fx-padding: 5 4 5 4;" +
                            "-fx-effect: dropshadow(gaussian, rgba(96,150,80,0.28), 8, 0.2, 0, 0);"
                    );
                }
                e.consume();
            });

            cardsFrame.setOnDragExited(e -> {
                restoreLaneStyle();
                e.consume();
            });

            cardsFrame.setOnDragDropped(e -> {
                restoreLaneStyle();
                boolean success = false;
                if (e.getDragboard().hasString() && canAcceptPropertyDrop(e.getDragboard().getString(), color)) {
                    try {
                        int cardId = Integer.parseInt(e.getDragboard().getString().trim());
                        mainFrame.placePropertyByIdToColor(cardId, color);
                        success = true;
                    } catch (NumberFormatException ignored) {
                    }
                }
                e.setDropCompleted(success);
                e.consume();
            });
        }

        private void restoreLaneStyle() {
            PropertySet set = currentPlayer == null ? null : currentPlayer.getPropertyArea().getSet(color);
            boolean complete = set != null && set.isComplete();
            setStyle(laneStyle(complete));
            cardsFrame.setStyle(frameStyle(complete));
        }

        private String laneStyle(boolean complete) {
            String border = complete ? "rgba(244,210,92,1.0)" : UITheme.toCssHex(UITheme.BORDER);
            String background = complete ? "rgba(255,249,232,0.94)" : "rgba(252,247,232,0.90)";
            String effect = complete
                    ? "-fx-effect: dropshadow(gaussian, rgba(236,198,62,0.36), 10, 0.28, 0, 0);"
                    : "-fx-effect: none;";
            return "-fx-background-color: " + background + ";" +
                    "-fx-border-color: " + border + ";" +
                    "-fx-border-width: 1px;" +
                    "-fx-border-radius: 6px; -fx-background-radius: 6px;" +
                    "-fx-padding: 5 4 5 4;" +
                    effect;
        }

        private String frameStyle(boolean complete) {
            String border = complete ? "rgba(235,194,58,1.0)" : UITheme.toCssHex(UITheme.BORDER);
            String background = complete ? "rgba(255,248,224,0.90)" : "rgba(255,255,255,0.55)";
            String effect = complete
                    ? "-fx-effect: dropshadow(gaussian, rgba(236,198,62,0.28), 8, 0.24, 0, 0);"
                    : "-fx-effect: none;";
            return "-fx-background-color: " + background + ";" +
                    "-fx-border-color: " + border + ";" +
                    "-fx-border-width: 1px;" +
                    "-fx-border-radius: 4px; -fx-background-radius: 4px;" +
                    effect;
        }
    }

    private static int requiredCount(PropertyType color) {
        switch (color) {
            case BROWN:
            case BLUE:
            case LIGHTGREEN:
                return 2;
            case LIGHTBLUE:
            case PURPLE:
            case ORANGE:
            case RED:
            case YELLOW:
            case GREEN:
                return 3;
            case BLACK:
                return 4;
            default:
                return 0;
        }
    }
}
