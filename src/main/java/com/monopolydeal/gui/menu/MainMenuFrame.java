package com.monopolydeal.gui.menu;

import com.monopolydeal.gui.MonopolyDealGUIApp;
import com.monopolydeal.gui.network.NetworkGameFrame;
import com.monopolydeal.gui.network.NetworkLobbyFrame;
import com.monopolydeal.gui.theme.ThemedConfirmDialog;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;

/**
 * Home screen wrapper. Owns the primary {@link Stage} and shows it with the main-menu scene.
 */
public class MainMenuFrame {

    private final Stage stage;
    private final MainMenuPanel menuPanel;

    public MainMenuFrame(Stage stage) {
        this.stage = stage;

        MainMenuFrame self = this;

        menuPanel = new MainMenuPanel(
                new LocalGameSetupPanel.SetupListener() {
                    @Override
                    public void onBack() {
                        menuPanel.showMainMenu();
                    }

                    @Override
                    public void onStart(int playerCount, List<String> playerNames) {
                        MonopolyDealGUIApp.startLocalGame(self, playerCount, playerNames);
                    }
                },
                new SinglePlayerSetupPanel.SetupListener() {
                    @Override
                    public void onBack() {
                        menuPanel.showMainMenu();
                    }

                    @Override
                    public void onStart(String playerName, int aiOpponents) {
                        MonopolyDealGUIApp.startAiGame(self, playerName, aiOpponents);
                    }
                }
        );

        wireButtons();

        Scene scene = new Scene(menuPanel, 1280, 720);
        stage.setTitle("Monopoly Deal");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(560);
        stage.setOnCloseRequest(e -> System.exit(0));
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void wireButtons() {
        if (menuPanel.getButtonLocalGame() != null) {
            menuPanel.getButtonLocalGame().setOnAction(e -> menuPanel.showLocalSetup());
        }
        if (menuPanel.getButtonSinglePlayer() != null) {
            menuPanel.getButtonSinglePlayer().setOnAction(e -> menuPanel.showSinglePlayerSetup());
        }
        if (menuPanel.getButtonOnline() != null) {
            menuPanel.getButtonOnline().setOnAction(e ->
                menuPanel.showLanSetup(new LanSetupPanel.LanSetupListener() {
                    @Override
                    public void onBack() {
                        menuPanel.showMainMenu();
                    }

                    @Override
                    public void onHost(int playerCount, String hostName, int port) {
                        com.monopolydeal.network.GameServer server =
                                new com.monopolydeal.network.GameServer(port, playerCount, hostName);
                        NetworkLobbyFrame lobby = new NetworkLobbyFrame(server, stage, MainMenuFrame.this::showHomeAgain);
                        lobby.show();
                    }

                    @Override
                    public void onJoin(String host, int port, String roomCode, String playerName) {
                        NetworkGameFrame.openAsClient(
                                host, port, roomCode, playerName, stage, true, MainMenuFrame.this::showHomeAgain);
                    }
                })
            );
        }
        if (menuPanel.getButtonRules() != null) {
            menuPanel.getButtonRules().setOnAction(e -> GameRulesDialog.show(stage));
        }
        if (menuPanel.getButtonExit() != null) {
            menuPanel.getButtonExit().setOnAction(e -> {
                boolean confirmed = ThemedConfirmDialog.show(
                        stage,
                        "Exit Game",
                        "Quit Monopoly Deal and close the application?",
                        "Exit",
                        "Cancel"
                );
                if (confirmed) {
                    Platform.exit();
                    System.exit(0);
                }
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    public Stage getStage() { return stage; }

    public void show() {
        stage.show();
    }

    /** Called when a game session ends so the player returns to the home screen. */
    public void showHomeAgain() {
        menuPanel.showMainMenu();
        stage.show();
        stage.toFront();
    }

    public static MainMenuFrame createAndShow(Stage stage) {
        MainMenuFrame frame = new MainMenuFrame(stage);
        frame.show();
        return frame;
    }
}