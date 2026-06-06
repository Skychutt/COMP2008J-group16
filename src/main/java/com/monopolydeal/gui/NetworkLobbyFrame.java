package com.monopolydeal.gui;

import com.monopolydeal.network.GameServer;
import com.monopolydeal.network.LanAddressUtil;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * LAN host waiting lobby — modelled after ENG-19's NetworkLobbyController.
 *
 * Improvements over the original:
 *  - Shows ALL local IPv4 addresses (via LanAddressUtil) so teammates on
 *    different network adapters can find the right address.
 *  - Displays each player's name as they connect, not just a generic count.
 *  - Cleaner, wider layout with a split IP-list / log area.
 */
public class NetworkLobbyFrame {

    private final Stage stage;
    private final Label lblStatus;
    private final VBox playerListBox;
    private final TextArea txtLog;
    private final GameServer server;
    private final List<String> playerNames;

    public NetworkLobbyFrame(GameServer server, List<String> playerNames, Stage homeStage) {
        this.server      = server;
        this.playerNames = playerNames;

        stage = new Stage();
        stage.setTitle("LAN Game — Waiting Hall");
        stage.setResizable(true);
        stage.setOnCloseRequest(e -> {
            e.consume();
            cancelAndReturn(homeStage);
        });

        VBox content = new VBox(16);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(24, 32, 20, 32));
        content.setStyle("-fx-background-color: " + UITheme.toCssHex(UITheme.PAGE_BG) + ";");

        // ── Title ──
        Label title = new Label("Waiting for Players...");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setTextFill(Color.rgb(250, 241, 209));

        // ── IP address block ──
        List<String> ips = LanAddressUtil.localIpv4Addresses();
        VBox ipBlock = new VBox(4);
        ipBlock.setAlignment(Pos.CENTER);
        ipBlock.setStyle(
            "-fx-background-color: rgba(0,0,0,0.28);" +
            "-fx-border-color: " + UITheme.toCssHex(UITheme.ACCENT_DARK) + ";" +
            "-fx-border-width: 1px; -fx-border-radius: 6px; -fx-background-radius: 6px;" +
            "-fx-padding: 10 20 10 20;"
        );

        Label lblIpHeader = new Label("Share one of these addresses with your teammates:");
        lblIpHeader.setFont(UITheme.FONT_BODY);
        lblIpHeader.setTextFill(Color.rgb(210, 220, 195));
        ipBlock.getChildren().add(lblIpHeader);

        if (ips.isEmpty()) {
            Label fallback = new Label("localhost   (no network adapters detected)");
            fallback.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));
            fallback.setTextFill(UITheme.ACCENT);
            ipBlock.getChildren().add(fallback);
        } else {
            for (String ip : ips) {
                Label lblIp = new Label(ip + "   :   " + server.getPort());
                lblIp.setFont(Font.font("Monospaced", FontWeight.BOLD, 15));
                lblIp.setTextFill(UITheme.ACCENT);
                ipBlock.getChildren().add(lblIp);
            }
        }

        // ── Player count status ──
        lblStatus = new Label("Connected: 0 / " + server.getPlayerCount());
        lblStatus.setFont(UITheme.FONT_SUBTITLE);
        lblStatus.setTextFill(Color.rgb(210, 230, 200));

        // ── Player roster ──
        Label lblRosterTitle = new Label("Players");
        lblRosterTitle.setFont(UITheme.FONT_SUBTITLE);
        lblRosterTitle.setTextFill(Color.rgb(248, 233, 191));

        playerListBox = new VBox(6);
        playerListBox.setStyle(
            "-fx-background-color: rgba(0,0,0,0.22);" +
            "-fx-border-color: rgba(180,150,70,0.5);" +
            "-fx-border-width: 1px; -fx-border-radius: 5px; -fx-background-radius: 5px;" +
            "-fx-padding: 8 12 8 12;"
        );
        // Pre-populate slots
        for (int i = 0; i < server.getPlayerCount(); i++) {
            playerListBox.getChildren().add(buildSlot(i, null));
        }

        VBox rosterBox = new VBox(6, lblRosterTitle, playerListBox);

        // ── Activity log ──
        Label lblLogTitle = new Label("Activity Log");
        lblLogTitle.setFont(UITheme.FONT_SUBTITLE);
        lblLogTitle.setTextFill(Color.rgb(248, 233, 191));

        txtLog = new TextArea();
        txtLog.setEditable(false);
        txtLog.setPrefRowCount(7);
        txtLog.setWrapText(true);
        txtLog.setStyle(
            "-fx-control-inner-background: " + UITheme.toCssHex(UITheme.LOG_BG) + ";" +
            "-fx-text-fill: " + UITheme.toCssHex(UITheme.LOG_TEXT) + ";" +
            "-fx-font-size: 12;"
        );
        VBox.setVgrow(txtLog, Priority.ALWAYS);

        VBox logBox = new VBox(6, lblLogTitle, txtLog);
        VBox.setVgrow(logBox, Priority.ALWAYS);

        // ── Side-by-side roster + log ──
        HBox middleRow = new HBox(16, rosterBox, logBox);
        middleRow.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(logBox, Priority.ALWAYS);

        // ── Cancel button ──
        Button btnCancel = new Button("Cancel");
        UITheme.styleMenuButton(btnCancel);
        btnCancel.setPrefWidth(140);
        btnCancel.setOnAction(e -> cancelAndReturn(homeStage));

        content.getChildren().addAll(title, ipBlock, lblStatus, middleRow, btnCancel);
        VBox.setVgrow(middleRow, Priority.ALWAYS);

        Scene scene = new Scene(content, 560, 480);
        stage.setScene(scene);
        stage.setMinWidth(480);
        stage.setMinHeight(400);

        // ── Server callbacks ──
        server.setStatusListener(new GameServer.ServerStatusListener() {
            @Override
            public void onPlayerJoined(int count, int total, String playerName) {
                Platform.runLater(() -> {
                    lblStatus.setText("Connected: " + count + " / " + total);
                    // Mark this slot as filled
                    updateSlot(count - 1, playerName);
                    appendLog("[" + timestamp() + "] " + playerName + " connected  (" + count + "/" + total + ")");
                });
            }

            @Override
            public void onGameStarted() {
                Platform.runLater(() -> {
                    appendLog("[" + timestamp() + "] All players ready — game starting!");
                    javafx.animation.PauseTransition pause =
                            new javafx.animation.PauseTransition(javafx.util.Duration.millis(600));
                    pause.setOnFinished(ev -> stage.hide());
                    pause.play();
                });
            }

            @Override
            public void onPlayerDisconnected(int playerIndex, String playerName) {
                Platform.runLater(() -> {
                    appendLog("[" + timestamp() + "] " + playerName + " disconnected.");
                    updateSlot(playerIndex, null);
                    lblStatus.setText("Connected: — / " + server.getPlayerCount());
                });
            }
        });

        appendLog("[" + timestamp() + "] Server starting on port " + server.getPort() + "...");

        // ── Server thread ──
        Thread serverThread = new Thread(() -> {
            try {
                server.bind();

                CountDownLatch hostReady = new CountDownLatch(1);
                Platform.runLater(() -> {
                    appendLog("[" + timestamp() + "] Port open. Host joining as Player 1 ("
                            + playerNames.get(0) + ")...");
                    NetworkGameFrame.openAsClient("localhost", server.getPort(), homeStage);
                    hostReady.countDown();
                });
                hostReady.await();

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

    private void updateSlot(int index, String connectedName) {
        if (index < 0 || index >= playerListBox.getChildren().size()) return;
        playerListBox.getChildren().set(index, buildSlot(index, connectedName));
    }

    private HBox buildSlot(int index, String connectedName) {
        String slotLabel = "Slot " + (index + 1) + ":  " + playerNames.get(index);
        boolean filled = connectedName != null;

        Label dot = new Label(filled ? "●" : "○");
        dot.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        dot.setTextFill(filled ? Color.rgb(80, 200, 100) : Color.rgb(160, 160, 160));
        dot.setMinWidth(20);

        Label lbl = new Label(slotLabel);
        lbl.setFont(UITheme.FONT_BODY);
        lbl.setTextFill(filled ? Color.rgb(220, 240, 200) : Color.rgb(160, 160, 160));

        Label status = new Label(filled ? "Connected" : "Waiting...");
        status.setFont(UITheme.FONT_SMALL);
        status.setTextFill(filled ? Color.rgb(80, 200, 100) : Color.rgb(160, 160, 160));

        HBox row = new HBox(8, dot, lbl, status);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

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

    private static String timestamp() {
        java.time.LocalTime now = java.time.LocalTime.now();
        return String.format("%02d:%02d:%02d", now.getHour(), now.getMinute(), now.getSecond());
    }
}
