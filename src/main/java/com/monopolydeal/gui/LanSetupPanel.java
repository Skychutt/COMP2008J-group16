package com.monopolydeal.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * LAN Online Setup Panel
 */
public class LanSetupPanel extends VBox {

    public interface LanSetupListener {
        void onBack();
        void onHost(int playerCount, List<String> playerNames, int port);
        void onJoin(String host, int port);
    }

    private static final int DEFAULT_PORT = 12345;

    private final LanSetupListener listener;

    private final RadioButton rbHost;
    private final RadioButton rbJoin;

    // Host panel widgets
    private int selectedCount = 2;
    private final Label lblCount;
    private final VBox namesContainer;
    private final List<TextField> nameFields = new ArrayList<>();
    private final TextField txtHostPort;

    // Join panel widgets
    private final TextField txtJoinIp;
    private final TextField txtJoinPort;

    // Dynamic panels shown/hidden based on mode
    private final VBox hostPane;
    private final VBox joinPane;

    private final Button btnAction;

    public LanSetupPanel(LanSetupListener listener) {
        this.listener = listener;

        setSpacing(16);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(28, 48, 28, 48));
        setStyle(
            "-fx-background-color: rgba(255,255,255,0.85);" +
            "-fx-border-color: " + UITheme.toCssHex(UITheme.BORDER) + ";" +
            "-fx-border-width: 2px; -fx-border-radius: 8px; -fx-background-radius: 8px;"
        );
        setMaxWidth(420);

        Label title = new Label("LAN Multiplayer");
        title.setFont(UITheme.FONT_MENU_SUBTITLE);
        title.setTextFill(Color.BLACK);

        // ── Mode selection ──
        rbHost = new RadioButton("Host a game");
        rbJoin = new RadioButton("Join a game");
        rbHost.setFont(UITheme.FONT_BODY);
        rbJoin.setFont(UITheme.FONT_BODY);
        rbHost.setTextFill(Color.BLACK);
        rbJoin.setTextFill(Color.BLACK);

        ToggleGroup group = new ToggleGroup();
        rbHost.setToggleGroup(group);
        rbJoin.setToggleGroup(group);
        rbHost.setSelected(true);

        HBox modeRow = new HBox(24, rbHost, rbJoin);
        modeRow.setAlignment(Pos.CENTER);

        // ── Host panel ──
        hostPane = new VBox(8);
        hostPane.setAlignment(Pos.CENTER);

        HBox countRow = new HBox(8);
        countRow.setAlignment(Pos.CENTER);
        Button btnDec = new Button("-");
        Button btnInc = new Button("+");
        btnDec.setFont(UITheme.FONT_BODY);
        btnInc.setFont(UITheme.FONT_BODY);
        btnDec.setPrefSize(32, 28);
        btnInc.setPrefSize(32, 28);

        lblCount = new Label("2 Players");
        lblCount.setFont(UITheme.FONT_BODY);
        lblCount.setTextFill(Color.BLACK);
        lblCount.setMinWidth(80);
        lblCount.setAlignment(Pos.CENTER);

        Label lblPlayers = new Label("Players:");
        lblPlayers.setFont(UITheme.FONT_BODY);
        lblPlayers.setTextFill(Color.BLACK);

        countRow.getChildren().addAll(lblPlayers, btnDec, lblCount, btnInc);
        hostPane.getChildren().add(countRow);

        namesContainer = new VBox(6);
        namesContainer.setAlignment(Pos.CENTER);
        hostPane.getChildren().add(namesContainer);

        HBox portHostRow = new HBox(8);
        portHostRow.setAlignment(Pos.CENTER);
        Label lblHostPort = new Label("Port:");
        lblHostPort.setFont(UITheme.FONT_BODY);
        lblHostPort.setTextFill(Color.BLACK);
        txtHostPort = new TextField(String.valueOf(DEFAULT_PORT));
        txtHostPort.setFont(UITheme.FONT_BODY);
        txtHostPort.setPrefColumnCount(8);
        portHostRow.getChildren().addAll(lblHostPort, txtHostPort);
        hostPane.getChildren().add(portHostRow);

        // ── Join panel ──
        joinPane = new VBox(10);
        joinPane.setAlignment(Pos.CENTER);
        joinPane.setVisible(false);
        joinPane.setManaged(false);

        HBox ipRow = new HBox(8);
        ipRow.setAlignment(Pos.CENTER);
        Label lblIp = new Label("Server IP:");
        lblIp.setFont(UITheme.FONT_BODY);
        lblIp.setTextFill(Color.BLACK);
        txtJoinIp = new TextField("192.168.1.100");
        txtJoinIp.setFont(UITheme.FONT_BODY);
        txtJoinIp.setPrefColumnCount(16);
        ipRow.getChildren().addAll(lblIp, txtJoinIp);

        HBox portJoinRow = new HBox(8);
        portJoinRow.setAlignment(Pos.CENTER);
        Label lblJoinPort = new Label("Port:");
        lblJoinPort.setFont(UITheme.FONT_BODY);
        lblJoinPort.setTextFill(Color.BLACK);
        txtJoinPort = new TextField(String.valueOf(DEFAULT_PORT));
        txtJoinPort.setFont(UITheme.FONT_BODY);
        txtJoinPort.setPrefColumnCount(8);
        portJoinRow.getChildren().addAll(lblJoinPort, txtJoinPort);

        joinPane.getChildren().addAll(ipRow, portJoinRow);

        // ── Buttons ──
        Button btnBack = new Button("Back");
        btnAction      = new Button("Host Game");
        UITheme.styleMenuButton(btnBack);
        UITheme.styleMenuButton(btnAction);
        btnBack.setPrefWidth(140);
        btnAction.setPrefWidth(140);

        HBox btnRow = new HBox(16, btnBack, btnAction);
        btnRow.setAlignment(Pos.CENTER);

        getChildren().addAll(title, modeRow, hostPane, joinPane, btnRow);

        // ── Event wiring ──
        rbHost.setOnAction(e -> switchMode(true));
        rbJoin.setOnAction(e -> switchMode(false));
        btnDec.setOnAction(e -> adjustCount(-1));
        btnInc.setOnAction(e -> adjustCount(+1));
        btnBack.setOnAction(e -> listener.onBack());
        btnAction.setOnAction(e -> onActionClicked());

        rebuildNameFields();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void switchMode(boolean hostMode) {
        hostPane.setVisible(hostMode);
        hostPane.setManaged(hostMode);
        joinPane.setVisible(!hostMode);
        joinPane.setManaged(!hostMode);
        btnAction.setText(hostMode ? "Host Game" : "Join Game");
    }

    private void adjustCount(int delta) {
        selectedCount = Math.max(2, Math.min(5, selectedCount + delta));
        lblCount.setText(selectedCount + " Players");
        rebuildNameFields();
    }

    private void rebuildNameFields() {
        namesContainer.getChildren().clear();
        nameFields.clear();
        for (int i = 0; i < selectedCount; i++) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER);
            Label lbl = new Label("Player " + (i + 1) + ":");
            lbl.setFont(UITheme.FONT_BODY);
            lbl.setTextFill(Color.BLACK);
            lbl.setMinWidth(70);
            TextField tf = new TextField("Player " + (i + 1));
            tf.setFont(UITheme.FONT_BODY);
            tf.setPrefColumnCount(16);
            nameFields.add(tf);
            row.getChildren().addAll(lbl, tf);
            namesContainer.getChildren().add(row);
        }
    }

    private void onActionClicked() {
        if (rbHost.isSelected()) {
            int port = parsePort(txtHostPort.getText(), DEFAULT_PORT);
            List<String> names = new ArrayList<>();
            for (int i = 0; i < nameFields.size(); i++) {
                String txt = nameFields.get(i).getText().trim();
                names.add(txt.isEmpty() ? "Player " + (i + 1) : txt);
            }
            listener.onHost(selectedCount, names, port);
        } else {
            String ip = txtJoinIp.getText().trim();
            if (ip.isEmpty()) ip = "localhost";
            int port = parsePort(txtJoinPort.getText(), DEFAULT_PORT);
            listener.onJoin(ip, port);
        }
    }

    public void resetToDefaults() {
        rbHost.setSelected(true);
        switchMode(true);
        selectedCount = 2;
        lblCount.setText("2 Players");
        rebuildNameFields();
    }

    private static int parsePort(String text, int fallback) {
        try {
            int v = Integer.parseInt(text.trim());
            return (v >= 1024 && v <= 65535) ? v : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
