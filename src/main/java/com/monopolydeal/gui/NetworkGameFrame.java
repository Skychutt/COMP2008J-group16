package com.monopolydeal.gui;

import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.model.card.PropertyCard;
import com.monopolydeal.network.GameClient;
import com.monopolydeal.network.GameStateParser;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * LAN online game main window
 *
 * Each player can only see their own hand, while other players only display the number of hands
 */
public class NetworkGameFrame {

    private final Stage stage;
    private final GameClient client;
    private final Stage homeStage;

    private volatile int myPlayerIndex = -1;
    private volatile boolean myTurn = false;
    private volatile boolean discardMode = false;
    private volatile int discardRemaining = 0;

    private final CardImageResolver imageResolver;
    private TopStatusPanel topStatusPanel;
    private NetworkPlayerPanel playerPanel;
    private NetworkControlPanel controlPanel;
    private GameBoardPane board;

    private volatile GameStateParser.Snapshot snapshot;
    private boolean gameOverShown = false;

    private NetworkGameFrame(GameClient client, Stage homeStage) {
        this.client    = client;
        this.homeStage = homeStage;
        this.imageResolver = new CardImageResolver();

        stage = new Stage();
        stage.setTitle("Monopoly Deal — LAN");

        javafx.geometry.Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double w = Math.min(1400, Math.max(1180, screen.getWidth() - 40));
        double h = Math.min(920,  Math.max(680,  screen.getHeight() - 80));

        stage.setMinWidth(1100);
        stage.setMinHeight(660);
        stage.setOnCloseRequest(e -> {
            client.disconnect();
            returnHome();
        });

        buildUI(w, h);
        wireClient();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Factory methods
    // ─────────────────────────────────────────────────────────────────────────

    /** Host (Player 0) connects to its own server as a client */
    public static void openAsHost(Stage homeStage) {
        openAsClient("localhost", com.monopolydeal.network.GameServer.DEFAULT_PORT, homeStage);
    }

    /** Connect to the specified server */
    public static void openAsClient(String host, int port, Stage homeStage) {
        GameClient client = new GameClient(host, port);
        NetworkGameFrame frame = new NetworkGameFrame(client, homeStage);

        Thread connectThread = new Thread(() -> {
            try {
                client.connect();
                Platform.runLater(() -> frame.stage.show());
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Connection Failed");
                    alert.setHeaderText(null);
                    alert.setContentText("Cannot connect to server:\n" + e.getMessage());
                    if (homeStage != null) alert.initOwner(homeStage);
                    alert.showAndWait();
                    returnToHome(homeStage);
                });
            }
        }, "NetworkConnect");
        connectThread.setDaemon(true);
        connectThread.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI construction
    // ─────────────────────────────────────────────────────────────────────────

    private void buildUI(double w, double h) {
        topStatusPanel = new TopStatusPanel();
        playerPanel    = new NetworkPlayerPanel(this);
        controlPanel   = new NetworkControlPanel(this);

        topStatusPanel.setCardDropHandler(this::handleCenterDrop);
        playerPanel.setBankDropHandler(this::handleBankDrop);
        playerPanel.setPropertyDropHandler(this::handlePropertyDrop);
        playerPanel.setEndTurnHandler(this::handleEndTurn);

        // Use the same circular oval-table board as local mode
        board = new GameBoardPane(topStatusPanel, playerPanel, controlPanel);

        Scene scene = new Scene(board, w, h);
        stage.setScene(scene);

        topStatusPanel.showWaitingMessage("Connecting to server...");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Client wiring
    // ─────────────────────────────────────────────────────────────────────────

    private void wireClient() {
        client.setMessageListener(new GameClient.MessageListener() {

            @Override
            public void onConnected(int myIndex) {
                myPlayerIndex = myIndex;
                Platform.runLater(() ->
                        topStatusPanel.showWaitingMessage("Connected! Waiting for all players..."));
            }

            @Override
            public void onGameStart(String message) {
                Platform.runLater(() ->
                        topStatusPanel.showWaitingMessage("Game starting..."));
            }

            @Override
            public void onGameState(String stateJson) {
                GameStateParser.Snapshot snap = GameStateParser.parse(stateJson, myPlayerIndex);
                snapshot = snap;
                if (snap != null) {
                    myTurn = !snap.gameOver && (snap.turn == myPlayerIndex);
                }
                Platform.runLater(() -> refreshFromSnapshot(snap));
            }

            @Override
            public void onYourTurn(String message) {
                myTurn = true;
                discardMode = false;
                Platform.runLater(() -> {
                    controlPanel.logEvent("** YOUR TURN — " + message);
                    refreshTurnIndicator();
                });
            }

            @Override
            public void onWait(String message) {
                myTurn = false;
                Platform.runLater(() -> {
                    controlPanel.logEvent(message);
                    refreshTurnIndicator();
                });
            }

            @Override
            public void onEvent(String event) {
                Platform.runLater(() -> {
                    controlPanel.logEvent(event);
                    if (event != null && event.contains("must discard") && myTurn) {
                        discardMode = true;
                        try {
                            discardRemaining = Integer.parseInt(
                                    event.replaceAll(".*must discard (\\d+).*", "$1"));
                        } catch (NumberFormatException ignored) {
                            discardRemaining = 1;
                        }
                        refreshTurnIndicator();
                    }
                });
            }

            @Override
            public void onGameOver(String winner) {
                Platform.runLater(() -> {
                    if (!gameOverShown) {
                        gameOverShown = true;
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Game Over!");
                        alert.setHeaderText(null);
                        alert.setContentText(winner);
                        alert.initOwner(stage);
                        alert.showAndWait();
                    }
                });
            }

            @Override
            public void onDisconnected(String reason) {
                Platform.runLater(() -> {
                    if (!gameOverShown) {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Connection Lost");
                        alert.setHeaderText(null);
                        alert.setContentText("Disconnected: " + reason);
                        alert.initOwner(stage);
                        alert.showAndWait();
                        stage.close();
                    }
                });
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI refresh
    // ─────────────────────────────────────────────────────────────────────────

    private void refreshFromSnapshot(GameStateParser.Snapshot snap) {
        if (snap == null) return;
        topStatusPanel.updateFromSnapshot(snap, imageResolver, myTurn, discardMode, discardRemaining);
        playerPanel.updateFromSnapshot(snap, myTurn, discardMode, discardRemaining);
        board.updateFromSnapshot(snap, myPlayerIndex, imageResolver);
        controlPanel.updateFromSnapshot(snap, myPlayerIndex);
    }

    private void refreshTurnIndicator() {
        playerPanel.setMyTurn(myTurn, discardMode, discardRemaining);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Card action handlers (called from child panels)
    // ─────────────────────────────────────────────────────────────────────────

    public void handleCenterDrop(int cardId) {
        if (!myTurn) return;
        if (discardMode) {
            client.sendDiscard(cardId);
        } else {
            GameStateParser.CardInfo card = findCardInHand(cardId);
            if (card != null && "PROPERTY".equals(card.cardType)) {
                if (card.needsChoice) {
                    PropertyCard dummy = makeDummyPropertyCard(card);
                    PropertyType chosen = PropertyColorChooser.prompt(stage, dummy);
                    if (chosen == null) return;
                    client.sendPlaceProperty(cardId, chosen.name());
                } else {
                    client.sendPlaceProperty(cardId, null);
                }
            } else {
                client.sendPlayCard(cardId);
            }
        }
    }

    public void handleBankDrop(int cardId) {
        if (!myTurn || discardMode) return;
        client.sendBankCard(cardId);
    }

    public void handlePropertyDrop(int cardId) {
        if (!myTurn || discardMode) return;
        GameStateParser.CardInfo card = findCardInHand(cardId);
        if (card == null || !"PROPERTY".equals(card.cardType)) return;
        if (card.needsChoice) {
            PropertyCard dummy = makeDummyPropertyCard(card);
            PropertyType chosen = PropertyColorChooser.prompt(stage, dummy);
            if (chosen == null) return;
            client.sendPlaceProperty(cardId, chosen.name());
        } else {
            client.sendPlaceProperty(cardId, null);
        }
    }

    public void handleEndTurn() {
        if (!myTurn || discardMode) return;
        client.sendEndTurn();
        myTurn = false;
        refreshTurnIndicator();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private GameStateParser.CardInfo findCardInHand(int cardId) {
        if (snapshot == null || snapshot.myHand == null) return null;
        for (GameStateParser.CardInfo c : snapshot.myHand) {
            if (c.id == cardId) return c;
        }
        return null;
    }

    private PropertyCard makeDummyPropertyCard(GameStateParser.CardInfo info) {
        PropertyType color;
        try {
            color = PropertyType.valueOf(info.color);
        } catch (Exception e) {
            color = PropertyType.RAINBOW;
        }
        return new PropertyCard(info.name, info.value, color, true);
    }

    private void returnHome() {
        stage.close();
        returnToHome(homeStage);
    }

    private static void returnToHome(Stage homeStage) {
        if (homeStage != null) {
            homeStage.show();
            homeStage.toFront();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Getters (used by child panels)
    // ─────────────────────────────────────────────────────────────────────────

    public GameClient getClient()                { return client; }
    public int getMyPlayerIndex()                { return myPlayerIndex; }
    public boolean isMyTurn()                    { return myTurn; }
    public boolean isDiscardMode()               { return discardMode; }
    public CardImageResolver getImageResolver()  { return imageResolver; }
    public GameStateParser.Snapshot getSnapshot(){ return snapshot; }
    public Stage getStage()                      { return stage; }
}
