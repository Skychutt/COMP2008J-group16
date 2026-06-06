package com.monopolydeal.gui;

import com.monopolydeal.network.GameServer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * LAN game waiting hall
 *
 * After creating a room, the host displays the local IP/port and the current number of connected players, and automatically starts the game after waiting for all players to join
 */
public class NetworkLobbyFrame {

    private final Stage stage;
    private final Label lblStatus;
    private final TextArea txtLog;
    private final GameServer server;
    private Thread serverThread;

    public NetworkLobbyFrame(GameServer server, List<String> playerNames, Stage homeStage) {
        this.server = server;

        stage = new Stage();
        stage.setTitle("LAN Game — Waiting for Players");
        stage.setResizable(false);
        stage.setOnCloseRequest(e -> {
            e.consume();
            cancelAndReturn(homeStage);
        });

        VBox content = new VBox(14);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(24, 32, 24, 32));
        content.setStyle("-fx-background-color: " + UITheme.toCssHex(UITheme.PAGE_BG) + ";");

        Label title = new Label("Waiting for Players...");
        title.setFont(UITheme.FONT_TITLE);
        title.setTextFill(UITheme.TEXT_MAIN);

        String ipText = getLocalIp() + "   Port: " + server.getPort();
        Label lblIpInfo = new Label("Your IP:  " + ipText);
        lblIpInfo.setFont(javafx.scene.text.Font.font("Monospaced", javafx.scene.text.FontWeight.BOLD, 14));
        lblIpInfo.setTextFill(UITheme.ACCENT_DARK);

        lblStatus = new Label("Players connected: 0 / " + server.getPlayerCount());
        lblStatus.setFont(UITheme.FONT_SUBTITLE);
        lblStatus.setTextFill(UITheme.TEXT_MAIN);

        txtLog = new TextArea();
        txtLog.setEditable(false);
        txtLog.setPrefRowCount(8);
        txtLog.setPrefColumnCount(36);
        txtLog.setWrapText(true);
        txtLog.setStyle(
                "-fx-background-color: " + UITheme.toCssHex(UITheme.LOG_BG) + ";" +
                        "-fx-text-fill: " + UITheme.toCssHex(UITheme.LOG_TEXT) + ";"
        );

        Button btnCancel = new Button("Cancel");
        UITheme.styleMenuButton(btnCancel);
        btnCancel.setPrefWidth(140);
        btnCancel.setOnAction(e -> cancelAndReturn(homeStage));

        content.getChildren().addAll(title, lblIpInfo, lblStatus, txtLog, btnCancel);

        Scene scene = new Scene(content, 480, 400);
        stage.setScene(scene);

        // Register server status callbacks
        server.setStatusListener(new GameServer.ServerStatusListener() {
            @Override
            public void onPlayerJoined(int count, int total) {
                Platform.runLater(() -> {
                    lblStatus.setText("Players connected: " + count + " / " + total);
                    appendLog("Player " + count + " connected!");
                });
            }

            @Override
            public void onGameStarted() {
                Platform.runLater(() -> {
                    appendLog("All players connected. Game starting!");
                    // Short delay then hide lobby
                    javafx.animation.PauseTransition pause =
                            new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
                    pause.setOnFinished(ev -> stage.hide());
                    pause.play();
                });
            }

            @Override
            public void onPlayerDisconnected(int idx) {
                Platform.runLater(() ->
                        appendLog("Player " + (idx + 1) + " disconnected."));
            }
        });

        appendLog("Server starting on port " + server.getPort() + "...");

        // Switch back to FX thread to create NetworkGameFrame (Host connects as Player 0)
        serverThread = new Thread(() -> {
            try {
                server.bind();

                CountDownLatch clientReady = new CountDownLatch(1);

                Platform.runLater(() -> {
                    appendLog("Port ready. Host connecting as Player 1...");
                    NetworkGameFrame.openAsClient("localhost", server.getPort(), homeStage);
                    clientReady.countDown();
                });

                clientReady.await();

                server.acceptRemainingPlayers();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                Platform.runLater(() -> appendLog("Server error: " + e.getMessage()));
            }
        }, "GameServerThread");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    public void show() {
        stage.show();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void appendLog(String text) {
        txtLog.appendText(text + "\n");
    }

    private void cancelAndReturn(Stage homeStage) {
        server.stop();
        stage.hide();
        if (homeStage != null) {
            homeStage.show();
            homeStage.toFront();
        }
    }

    private static String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "localhost";
        }
    }
}
