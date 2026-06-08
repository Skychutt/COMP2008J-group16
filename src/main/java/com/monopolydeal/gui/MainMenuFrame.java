package com.monopolydeal.gui;

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
                    public void onHost(int playerCount, List<String> playerNames, int port) {
                        com.monopolydeal.network.GameServer server =
                                new com.monopolydeal.network.GameServer(port, playerCount, playerNames);
                        NetworkLobbyFrame lobby = new NetworkLobbyFrame(server, playerNames, stage);
                        lobby.show();
                    }

                    @Override
                    public void onJoin(String host, int port) {
                        NetworkGameFrame.openAsClient(host, port, stage);
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

    private void showComingSoon(String modeName) {
        ThemedDialog.showInfo(
                stage,
                modeName,
                modeName + " is not available yet.\nPlease use Local Game for now."
        );
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
