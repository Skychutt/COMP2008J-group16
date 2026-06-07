package com.monopolydeal.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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

    private int selectedCount = 2;
    private final Label lblCount;
    private final VBox namesContainer;
    private final List<TextField> nameFields = new ArrayList<>();
    private final TextField txtHostPort;

    private final TextField txtJoinIp;
    private final TextField txtJoinPort;

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

        namesContainer = new VBox(8);
        namesContainer.setAlignment(Pos.CENTER);
        hostPane.getChildren().add(namesContainer);

        HBox portHostRow = new HBox(10);
        portHostRow.setAlignment(Pos.CENTER);
        Label lblHostPort = new Label("Port:");
        UITheme.styleSetupLabel(lblHostPort);
        txtHostPort = new TextField(String.valueOf(DEFAULT_PORT));
        UITheme.styleSetupField(txtHostPort);
        txtHostPort.setPrefColumnCount(8);
        portHostRow.getChildren().addAll(lblHostPort, txtHostPort);
        hostPane.getChildren().add(portHostRow);

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

        joinPane.getChildren().addAll(ipRow, portJoinRow);

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

        rebuildNameFields();
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
        rebuildNameFields();
    }

    private void rebuildNameFields() {
        namesContainer.getChildren().clear();
        nameFields.clear();
        for (int i = 0; i < selectedCount; i++) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER);
            Label lbl = new Label("Player " + (i + 1) + ":");
            UITheme.styleSetupLabel(lbl);
            lbl.setMinWidth(78);
            TextField tf = new TextField("Player " + (i + 1));
            UITheme.styleSetupField(tf);
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
            if (ip.isEmpty()) {
                ip = "localhost";
            }
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
