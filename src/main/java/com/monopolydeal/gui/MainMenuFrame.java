package com.monopolydeal.gui;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.List;

/**
 * Application home screen shown on launch.
 */
public class MainMenuFrame extends JFrame {

    private final MainMenuPanel menuPanel;

    public MainMenuFrame() {
        setTitle("Monopoly Deal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int width = Math.min(1280, Math.max(960, screen.width - 80));
        int height = Math.min(720, Math.max(600, screen.height - 100));
        setSize(width, height);
        setMinimumSize(new Dimension(900, 560));
        setLocationRelativeTo(null);

        menuPanel = new MainMenuPanel(new LocalGameSetupPanel.SetupListener() {
            @Override
            public void onBack() {
                menuPanel.showMainMenu();
            }

            @Override
            public void onStart(int playerCount, List<String> playerNames) {
                setVisible(false);
                MonopolyDealGUIApp.startLocalGame(MainMenuFrame.this, playerCount, playerNames);
            }
        });

        setLayout(new BorderLayout());
        add(menuPanel, BorderLayout.CENTER);

        wireButtons();
    }

    private void wireButtons() {
        JButton localGame = menuPanel.getButtonByText("Local Game");
        JButton singlePlayer = menuPanel.getButtonByText("Single Player");
        JButton online = menuPanel.getButtonByText("Online Multiplayer");
        JButton rules = menuPanel.getButtonByText("Game Rules");

        if (localGame != null) {
            localGame.addActionListener(e -> menuPanel.showLocalSetup());
        }
        if (singlePlayer != null) {
            singlePlayer.addActionListener(e -> showComingSoon("Single Player"));
        }
        if (online != null) {
            online.addActionListener(e -> menuPanel.showLanSetup(new LanSetupPanel.LanSetupListener() {
                @Override
                public void onBack() {
                    menuPanel.showMainMenu();
                }

                @Override
                public void onHost(int playerCount, java.util.List<String> playerNames, int port) {
                    // Create a server and open the waiting hall
                    com.monopolydeal.network.GameServer server =
                            new com.monopolydeal.network.GameServer(port, playerCount, playerNames);
                    NetworkLobbyFrame lobby = new NetworkLobbyFrame(server, playerNames, MainMenuFrame.this);
                    setVisible(false);
                    lobby.setVisible(true);
                }

                @Override
                public void onJoin(String host, int port) {
                    setVisible(false);
                    com.monopolydeal.gui.NetworkGameFrame.openAsClient(host, port, MainMenuFrame.this);
                }
            }));
        }
        if (rules != null) {
            rules.addActionListener(e -> GameRulesDialog.show(this));
        }
    }

    private void showComingSoon(String modeName) {
        JOptionPane.showMessageDialog(
                this,
                modeName + " is not available yet.\nPlease use Local Game for now.",
                modeName,
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    /**
     * Called when a local session ends so the player returns to the home screen.
     */
    public void showHomeAgain() {
        menuPanel.showMainMenu();
        setVisible(true);
        toFront();
    }

    public static MainMenuFrame createAndShow() {
        MainMenuFrame frame = new MainMenuFrame();
        frame.setVisible(true);
        return frame;
    }
}
