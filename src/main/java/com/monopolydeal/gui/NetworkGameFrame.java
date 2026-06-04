package com.monopolydeal.gui;

import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.model.GameManager;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.PropertyCard;
import com.monopolydeal.network.GameClient;
import com.monopolydeal.network.GameStateParser;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.util.List;

/**
 * LAN Battle Game Window
 *
 *  The hand is only visible to oneself, and other players only display the number of hands
 */
public class NetworkGameFrame extends JFrame {

    private final GameClient client;
    private volatile int myPlayerIndex = -1;
    private volatile boolean myTurn = false;
    private volatile boolean discardMode = false;
    private volatile int discardRemaining = 0;

    private final CardImageResolver imageResolver;
    private TopStatusPanel topStatusPanel;
    private NetworkPlayerPanel playerPanel;
    private NetworkOpponentsPanel opponentsPanel;
    private NetworkControlPanel controlPanel;

    private volatile GameStateParser.Snapshot snapshot;

    private final JFrame homeFrame;
    private boolean gameOverShown = false;

    private NetworkGameFrame(GameClient client, JFrame homeFrame) {
        this.client = client;
        this.homeFrame = homeFrame;
        this.imageResolver = new CardImageResolver();

        setTitle("Monopoly Deal — LAN");
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int w = Math.min(1400, Math.max(1180, screen.width - 40));
        int h = Math.min(920, Math.max(680, screen.height - 80));
        setSize(w, h);
        setMinimumSize(new Dimension(1180, 680));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        buildUI();
        wireClient();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                client.disconnect();
                returnHome();
            }
        });
    }

    /**
     * Host as player 0 connects to its own server
     */
    public static void openAsHost(GameManager gm, JFrame homeFrame) {
        openAsClient("localhost", com.monopolydeal.network.GameServer.DEFAULT_PORT, homeFrame);
    }

    /**
     * Connect to the specified server
     */
    public static void openAsClient(String host, int port, JFrame homeFrame) {
        GameClient client = new GameClient(host, port);
        NetworkGameFrame frame = new NetworkGameFrame(client, homeFrame);

        Thread connectThread = new Thread(() -> {
            try {
                client.connect();
                SwingUtilities.invokeLater(() -> {
                    frame.setVisible(true);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(homeFrame,
                            "Cannot connect to server:\n" + e.getMessage(),
                            "Connection Failed",
                            JOptionPane.ERROR_MESSAGE);
                    returnToHome(homeFrame);
                });
            }
        }, "NetworkConnect");
        connectThread.setDaemon(true);
        connectThread.start();
    }

    private void buildUI() {
        TableSurfacePanel table = new TableSurfacePanel();
        table.setLayout(new BorderLayout(14, 14));
        table.setBorder(javax.swing.BorderFactory.createEmptyBorder(20, 24, 20, 24));
        setContentPane(table);

        topStatusPanel = new TopStatusPanel();
        playerPanel = new NetworkPlayerPanel(this);
        opponentsPanel = new NetworkOpponentsPanel(this);
        controlPanel = new NetworkControlPanel(this);

        topStatusPanel.setCardDropHandler(this::handleCenterDrop);
        playerPanel.setBankDropHandler(this::handleBankDrop);
        playerPanel.setPropertyDropHandler(this::handlePropertyDrop);
        playerPanel.setEndTurnHandler(this::handleEndTurn);

        add(opponentsPanel, BorderLayout.NORTH);
        add(controlPanel, BorderLayout.WEST);
        add(topStatusPanel, BorderLayout.CENTER);
        add(playerPanel, BorderLayout.SOUTH);

        showWaitingOverlay("Connecting to server...");
    }

    private void showWaitingOverlay(String message) {
        topStatusPanel.showWaitingMessage(message);
    }


    private void wireClient() {
        client.setMessageListener(new GameClient.MessageListener() {

            @Override
            public void onConnected(int myIndex) {
                myPlayerIndex = myIndex;
                SwingUtilities.invokeLater(() ->
                        showWaitingOverlay("Connected! Waiting for all players..."));
            }

            @Override
            public void onGameStart(String message) {
                SwingUtilities.invokeLater(() ->
                        showWaitingOverlay("Game starting..."));
            }

            @Override
            public void onGameState(String stateJson) {
                GameStateParser.Snapshot snap = GameStateParser.parse(stateJson, myPlayerIndex);
                snapshot = snap;
                if (snap != null && !snap.gameOver) {
                    myTurn = (snap.turn == myPlayerIndex);
                } else if (snap != null && snap.gameOver) {
                    myTurn = false;
                }
                SwingUtilities.invokeLater(() -> refreshFromSnapshot(snap));
            }

            @Override
            public void onYourTurn(String message) {
                myTurn = true;
                discardMode = false;
                SwingUtilities.invokeLater(() -> {
                    controlPanel.logEvent("** YOUR TURN — " + message);
                    refreshTurnIndicator();
                });
            }

            @Override
            public void onWait(String message) {
                myTurn = false;
                SwingUtilities.invokeLater(() -> {
                    controlPanel.logEvent(message);
                    refreshTurnIndicator();
                });
            }

            @Override
            public void onEvent(String event) {
                SwingUtilities.invokeLater(() -> {
                    controlPanel.logEvent(event);
                    if (event.contains("must discard") && myTurn) {
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
                SwingUtilities.invokeLater(() -> {
                    if (!gameOverShown) {
                        gameOverShown = true;
                        JOptionPane.showMessageDialog(NetworkGameFrame.this,
                                winner, "Game Over!", JOptionPane.INFORMATION_MESSAGE);
                    }
                });
            }

            @Override
            public void onDisconnected(String reason) {
                SwingUtilities.invokeLater(() -> {
                    if (!gameOverShown) {
                        JOptionPane.showMessageDialog(NetworkGameFrame.this,
                                "Disconnected: " + reason, "Connection Lost",
                                JOptionPane.WARNING_MESSAGE);
                        dispose();
                    }
                });
            }
        });
    }

    /**
     * Refresh all UI panels
     */
    private void refreshFromSnapshot(GameStateParser.Snapshot snap) {
        if (snap == null) return;

        topStatusPanel.updateFromSnapshot(snap, imageResolver, myTurn, discardMode, discardRemaining);

        playerPanel.updateFromSnapshot(snap, myTurn, discardMode, discardRemaining);

        opponentsPanel.updateFromSnapshot(snap, myPlayerIndex);

        controlPanel.updateFromSnapshot(snap, myPlayerIndex);
    }

    private void refreshTurnIndicator() {
        playerPanel.setMyTurn(myTurn, discardMode, discardRemaining);
    }

    public void handleCenterDrop(int cardId) {
        if (!myTurn) return;
        if (discardMode) {
            client.sendDiscard(cardId);
        } else {
            GameStateParser.CardInfo card = findCardInHand(cardId);
            if (card != null && "PROPERTY".equals(card.cardType)) {
                if (card.needsChoice) {
                    PropertyCard dummy = makeDummyPropertyCard(card);
                    PropertyType chosen = PropertyColorChooser.prompt(this, dummy);
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
            PropertyType chosen = PropertyColorChooser.prompt(this, dummy);
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
        returnToHome(homeFrame);
    }

    private static void returnToHome(JFrame homeFrame) {
        if (homeFrame instanceof MainMenuFrame) {
            ((MainMenuFrame) homeFrame).showHomeAgain();
        } else if (homeFrame != null) {
            homeFrame.setVisible(true);
        }
    }

    public GameClient getClient() { return client; }
    public int getMyPlayerIndex() { return myPlayerIndex; }
    public boolean isMyTurn() { return myTurn; }
    public boolean isDiscardMode() { return discardMode; }
    public CardImageResolver getImageResolver() { return imageResolver; }
    public GameStateParser.Snapshot getSnapshot() { return snapshot; }
}