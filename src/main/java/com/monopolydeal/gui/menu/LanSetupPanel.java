package com.monopolydeal.gui.menu;

import com.monopolydeal.gui.theme.ThemedDialog;
import com.monopolydeal.gui.theme.UITheme;

import com.monopolydeal.network.GameServer;

import javafx.geometry.Insets;
import javafx.stage.Window;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * LAN online setup: host creates a room; joiners enter IP, port, room code, and their name.
 */
public class LanSetupPanel extends VBox {

    public interface LanSetupListener {
        void onBack();
        void onHost(int playerCount, String hostName, int port);
        void onJoin(String host, int port, String roomCode, String playerName);
    }

    public static final int DEFAULT_PORT = GameServer.DEFAULT_PORT;

    private final LanSetupListener listener;

    private final RadioButton rbHost;
    private final RadioButton rbJoin;

    private int selectedCount = 2;
    private final Label lblCount;
    private final TextField txtHostName;
    private final TextField txtHostPort;

    private final TextField txtJoinIp;
    private final TextField txtJoinPort;
    private final TextField txtRoomCode;
    private final TextField txtJoinName;

    private final VBox hostPane;
    private final VBox joinPane;
    private final Button btnAction;

    public LanSetupPanel(LanSetupListener listener) {
        this.listener = listener;

        UITheme.applySetupPanelRoot(this);
        setSpacing(16);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(24, 32, 24, 32));
        setMaxWidth(560);
        setFillWidth(false);

        getChildren().add(UITheme.createSetupTitleRow("LAN Multiplayer"));

        rbHost = new RadioButton("Host a game");
        rbJoin = new RadioButton("Join a game");
        ToggleGroup group = new ToggleGroup();
        rbHost.setToggleGroup(group);
        rbJoin.setToggleGroup(group);
        rbHost.setSelected(true);
        UITheme.styleSetupRadio(rbHost);
        UITheme.styleSetupRadio(rbJoin);

        HBox modeRow = new HBox(28, rbHost, rbJoin);
        modeRow.setAlignment(Pos.CENTER);
        getChildren().add(modeRow);

        hostPane = new VBox(12);
        hostPane.setAlignment(Pos.CENTER);

        HBox countRow = new HBox(10);
        countRow.setAlignment(Pos.CENTER);
        Button btnDec = new Button("-");
        Button btnInc = new Button("+");
        UITheme.styleSetupStepperButton(btnDec);
        UITheme.styleSetupStepperButton(btnInc);
        btnDec.setPrefSize(36, 32);
        btnInc.setPrefSize(36, 32);

        lblCount = new Label("2 Players");
        UITheme.styleSetupAccentLabel(lblCount);
        lblCount.setMinWidth(96);
        lblCount.setAlignment(Pos.CENTER);

        Label lblPlayers = new Label("Players:");
        UITheme.styleSetupLabel(lblPlayers);
        countRow.getChildren().addAll(lblPlayers, btnDec, lblCount, btnInc);
        hostPane.getChildren().add(countRow);

        HBox hostNameRow = new HBox(10);
        hostNameRow.setAlignment(Pos.CENTER);
        Label lblHostName = new Label("Your name:");
        UITheme.styleSetupLabel(lblHostName);
        txtHostName = new TextField("Host");
        UITheme.styleSetupField(txtHostName);
        txtHostName.setPrefColumnCount(16);
        hostNameRow.getChildren().addAll(lblHostName, txtHostName);
        hostPane.getChildren().add(hostNameRow);

        HBox portHostRow = new HBox(10);
        portHostRow.setAlignment(Pos.CENTER);
        Label lblHostPort = new Label("Port:");
        UITheme.styleSetupLabel(lblHostPort);
        txtHostPort = new TextField(String.valueOf(DEFAULT_PORT));
        UITheme.styleSetupField(txtHostPort);
        txtHostPort.setPrefColumnCount(8);
        portHostRow.getChildren().addAll(lblHostPort, txtHostPort);
        hostPane.getChildren().add(portHostRow);

        Label hostHint = new Label(
                "After you host, a waiting window will show your\n"
                        + "Room Code and IP — share those with other players.");
        hostHint.setWrapText(true);
        hostHint.setMaxWidth(420);
        hostHint.setAlignment(Pos.CENTER);
        UITheme.styleSetupLabel(hostHint);
        hostPane.getChildren().add(hostHint);

        joinPane = new VBox(12);
        joinPane.setAlignment(Pos.CENTER);
        joinPane.setVisible(false);
        joinPane.setManaged(false);

        HBox ipRow = new HBox(10);
        ipRow.setAlignment(Pos.CENTER);
        Label lblIp = new Label("Server IP:");
        UITheme.styleSetupLabel(lblIp);
        txtJoinIp = new TextField("192.168.1.100");
        UITheme.styleSetupField(txtJoinIp);
        txtJoinIp.setPrefColumnCount(16);
        ipRow.getChildren().addAll(lblIp, txtJoinIp);

        HBox portJoinRow = new HBox(10);
        portJoinRow.setAlignment(Pos.CENTER);
        Label lblJoinPort = new Label("Port:");
        UITheme.styleSetupLabel(lblJoinPort);
        txtJoinPort = new TextField(String.valueOf(DEFAULT_PORT));
        UITheme.styleSetupField(txtJoinPort);
        txtJoinPort.setPrefColumnCount(8);
        portJoinRow.getChildren().addAll(lblJoinPort, txtJoinPort);

        HBox codeRow = new HBox(10);
        codeRow.setAlignment(Pos.CENTER);
        Label lblCode = new Label("Room code:");
        UITheme.styleSetupLabel(lblCode);
        txtRoomCode = new TextField();
        UITheme.styleSetupField(txtRoomCode);
        txtRoomCode.setPrefColumnCount(10);
        txtRoomCode.setPromptText("6-digit code from host");
        codeRow.getChildren().addAll(lblCode, txtRoomCode);

        HBox joinNameRow = new HBox(10);
        joinNameRow.setAlignment(Pos.CENTER);
        Label lblJoinName = new Label("Your name:");
        UITheme.styleSetupLabel(lblJoinName);
        txtJoinName = new TextField("Player");
        UITheme.styleSetupField(txtJoinName);
        txtJoinName.setPrefColumnCount(16);
        joinNameRow.getChildren().addAll(lblJoinName, txtJoinName);

        Label joinHint = new Label(
                "Ask the host for three things from their waiting screen:\n"
                        + "Server IP, Port, and 6-digit Room Code.\n"
                        + "Same computer? Use IP: localhost");
        joinHint.setWrapText(true);
        joinHint.setMaxWidth(420);
        joinHint.setAlignment(Pos.CENTER);
        UITheme.styleSetupLabel(joinHint);

        joinPane.getChildren().addAll(joinHint, ipRow, portJoinRow, codeRow, joinNameRow);

        Button btnBack = new Button("Back");
        btnAction = new Button("Host Game");
        UITheme.styleMenuButton(btnBack);
        UITheme.styleMenuButton(btnAction);
        btnBack.setPrefWidth(150);
        btnAction.setPrefWidth(150);

        HBox btnRow = new HBox(18, btnBack, btnAction);
        btnRow.setAlignment(Pos.CENTER);
        btnRow.setPadding(new Insets(8, 0, 0, 0));

        getChildren().addAll(hostPane, joinPane, btnRow);

        rbHost.setOnAction(e -> switchMode(true));
        rbJoin.setOnAction(e -> switchMode(false));
        btnDec.setOnAction(e -> adjustCount(-1));
        btnInc.setOnAction(e -> adjustCount(+1));
        btnBack.setOnAction(e -> listener.onBack());
        btnAction.setOnAction(e -> onActionClicked());
    }

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
    }

    private void onActionClicked() {
        if (rbHost.isSelected()) {
            int port = parsePort(txtHostPort.getText(), DEFAULT_PORT);
            String hostName = txtHostName.getText().trim();
            if (hostName.isEmpty()) {
                hostName = "Host";
            }
            listener.onHost(selectedCount, hostName, port);
        } else {
            String ip = txtJoinIp.getText().trim();
            if (ip.isEmpty()) {
                ip = "localhost";
            }
            String roomCode = txtRoomCode.getText().trim();
            String playerName = txtJoinName.getText().trim();
            if (roomCode.isEmpty()) {
                showSetupWarning("Room Code Required",
                        "Enter the 6-digit Room Code from the host's waiting window.\n\n"
                                + "You also need the host's Server IP and Port (shown there too).");
                return;
            }
            if (playerName.isEmpty()) {
                showSetupWarning("Name Required", "Enter your player name.");
                return;
            }
            int port = parsePort(txtJoinPort.getText(), DEFAULT_PORT);
            listener.onJoin(ip, port, roomCode, playerName);
        }
    }

    public void resetToDefaults() {
        rbHost.setSelected(true);
        switchMode(true);
        selectedCount = 2;
        lblCount.setText("2 Players");
    }

    private void showSetupWarning(String title, String message) {
        Window owner = getScene() != null ? getScene().getWindow() : null;
        ThemedDialog.showWarning(owner, title, message);
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
