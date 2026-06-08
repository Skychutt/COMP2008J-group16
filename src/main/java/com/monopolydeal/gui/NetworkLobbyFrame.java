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
    private final Stage homeStage;
    private final Runnable homeCallback;

    public NetworkLobbyFrame(GameServer server, Stage homeStage, Runnable homeCallback) {
        this.server = server;
        this.homeStage = homeStage;
        this.homeCallback = homeCallback;

        stage = new Stage();
        stage.setTitle("LAN Game — Waiting Hall");
        stage.setResizable(true);
        stage.setOnCloseRequest(e -> {
            e.consume();
            cancelAndReturn();
        });

        VBox content = new VBox(16);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(24, 32, 20, 32));
        content.setStyle("-fx-background-color: " + UITheme.toCssHex(UITheme.PAGE_BG) + ";");

        Label title = new Label("Waiting for Players...");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setTextFill(Color.rgb(250, 241, 209));

        VBox ipBlock = buildJoinInfoBlock(server);

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
        btnCancel.setOnAction(e -> cancelAndReturn());

        middleRow.setMaxHeight(220);
        content.getChildren().addAll(title, ipBlock, lblStatus, middleRow, btnCancel);

        Scene scene = new Scene(content, 640, 580);
        stage.setScene(scene);
        stage.setMinWidth(560);
        stage.setMinHeight(500);

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
                    appendLog("[" + timestamp() + "] Port open. Room code: " + server.getRoomCode());
                    appendLog("[" + timestamp() + "] Host joining as "
                            + server.getPlayerNameAt(0) + "...");
                    NetworkGameFrame.openAsClient(
                            "localhost",
                            server.getPort(),
                            server.getRoomCode(),
                            server.getPlayerNameAt(0),
                            homeStage,
                            false,
                            homeCallback
                    );
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
        stage.toFront();
        stage.requestFocus();
    }

    private static VBox buildJoinInfoBlock(GameServer server) {
        VBox ipBlock = new VBox(10);
        ipBlock.setAlignment(Pos.CENTER_LEFT);
        ipBlock.setFillWidth(true);
        ipBlock.setMaxWidth(Double.MAX_VALUE);
        ipBlock.setStyle(
                "-fx-background-color: rgba(0,0,0,0.42);" +
                "-fx-border-color: " + UITheme.toCssHex(UITheme.ACCENT) + ";" +
                "-fx-border-width: 2px; -fx-border-radius: 8px; -fx-background-radius: 8px;" +
                "-fx-padding: 16 24 16 24;"
        );

        Label lblJoinTitle = new Label("Share these with players (Join a game):");
        lblJoinTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));
        lblJoinTitle.setTextFill(Color.rgb(255, 244, 210));
        ipBlock.getChildren().add(lblJoinTitle);

        ipBlock.getChildren().add(infoLine("Server IP:", LanAddressUtil.primaryAddress()));
        ipBlock.getChildren().add(infoLine("Port:", String.valueOf(server.getPort())));
        ipBlock.getChildren().add(infoLine("Room Code:", server.getRoomCode()));

        Label lblIpTitle = new Label("Server IP options:");
        lblIpTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        lblIpTitle.setTextFill(Color.rgb(248, 233, 191));
        lblIpTitle.setPadding(new Insets(6, 0, 0, 0));
        ipBlock.getChildren().add(lblIpTitle);

        List<String> ips = LanAddressUtil.joinAddressesForDisplay();
        for (String ip : ips) {
            String note = "localhost".equals(ip) ? "  (same computer only)" : "  (other computers on your Wi‑Fi/LAN)";
            Label lblIp = new Label("  •  " + ip + note);
            lblIp.setFont(Font.font("Consolas", FontWeight.BOLD, 17));
            lblIp.setTextFill(Color.rgb(255, 220, 120));
            lblIp.setWrapText(true);
            lblIp.setMaxWidth(560);
            ipBlock.getChildren().add(lblIp);
        }

        if (LanAddressUtil.localIpv4Addresses().isEmpty()) {
            Label hint = new Label(
                    "  If no LAN IP appears above, other PCs may not connect.\n"
                            + "  Same-PC test: use localhost. Check firewall / Wi‑Fi.");
            hint.setFont(UITheme.FONT_BODY);
            hint.setTextFill(Color.rgb(210, 220, 195));
            hint.setWrapText(true);
            ipBlock.getChildren().add(hint);
        }
        return ipBlock;
    }

    private static HBox infoLine(String label, String value) {
        Label lbl = new Label(label);
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        lbl.setTextFill(Color.rgb(220, 230, 210));
        lbl.setMinWidth(120);

        Label val = new Label(value);
        val.setFont(Font.font("Consolas", FontWeight.BOLD, 20));
        val.setTextFill(Color.rgb(255, 220, 120));

        HBox row = new HBox(8, lbl, val);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void updateSlot(int index, String connectedName) {
        if (index < 0 || index >= playerListBox.getChildren().size()) return;
        playerListBox.getChildren().set(index, buildSlot(index, connectedName));
    }

    private HBox buildSlot(int index, String connectedName) {
        String slotLabel = "Slot " + (index + 1) + ":  " + server.getPlayerNameAt(index);
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

    private void cancelAndReturn() {
        server.stop();
        stage.hide();
        if (homeCallback != null) {
            homeCallback.run();
        } else if (homeStage != null) {
            homeStage.show();
            homeStage.toFront();
        }
    }

    private static String timestamp() {
        java.time.LocalTime now = java.time.LocalTime.now();
        return String.format("%02d:%02d:%02d", now.getHour(), now.getMinute(), now.getSecond());
    }
}
