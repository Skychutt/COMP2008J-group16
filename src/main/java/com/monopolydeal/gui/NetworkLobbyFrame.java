package com.monopolydeal.gui;

import com.monopolydeal.network.GameServer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.net.InetAddress;
import java.util.List;

/**
 * Waiting at the lobby window
 *
 * After creating a room, Host will display this window with the IP address and port number of the machine, as well as the current number of connected/total players
 */
public class NetworkLobbyFrame extends JFrame {

    private final JLabel lblStatus;
    private final JLabel lblIpInfo;
    private final JTextArea txtLog;
    private final JButton btnCancel;

    private final GameServer server;
    private Thread serverThread;

    public NetworkLobbyFrame(GameServer server, List<String> playerNames, JFrame homeFrame) {
        this.server = server;

        setTitle("LAN Game — Waiting for Players");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(480, 360);
        setMinimumSize(new Dimension(400, 300));
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(new Color(245, 245, 235));
        content.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        JLabel title = new JLabel("Waiting for Players...");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_MAIN);
        title.setAlignmentX(CENTER_ALIGNMENT);
        content.add(title);
        content.add(Box.createVerticalStrut(12));

        String ipText = getLocalIp() + "   Port: " + server.getPort();
        lblIpInfo = new JLabel("Your IP:  " + ipText);
        lblIpInfo.setFont(new Font("Monospaced", Font.BOLD, 14));
        lblIpInfo.setForeground(UITheme.ACCENT_DARK);
        lblIpInfo.setAlignmentX(CENTER_ALIGNMENT);
        content.add(lblIpInfo);
        content.add(Box.createVerticalStrut(10));

        lblStatus = new JLabel("Players connected: 0 / " + server.getPlayerCount());
        lblStatus.setFont(UITheme.FONT_SUBTITLE);
        lblStatus.setForeground(UITheme.TEXT_MAIN);
        lblStatus.setAlignmentX(CENTER_ALIGNMENT);
        content.add(lblStatus);
        content.add(Box.createVerticalStrut(12));

        txtLog = new JTextArea(8, 30);
        txtLog.setEditable(false);
        txtLog.setBackground(UITheme.LOG_BG);
        txtLog.setForeground(UITheme.LOG_TEXT);
        txtLog.setFont(UITheme.FONT_SMALL);
        txtLog.setLineWrap(true);
        JScrollPane logScroll = new JScrollPane(txtLog);
        logScroll.setAlignmentX(CENTER_ALIGNMENT);
        logScroll.setMaximumSize(new Dimension(400, 160));
        content.add(logScroll);
        content.add(Box.createVerticalStrut(14));

        btnCancel = new JButton("Cancel");
        UITheme.styleMenuButton(btnCancel);
        btnCancel.setAlignmentX(CENTER_ALIGNMENT);
        btnCancel.setMaximumSize(new Dimension(140, 38));
        content.add(btnCancel);

        add(content, BorderLayout.CENTER);

        btnCancel.addActionListener(e -> cancelAndReturn(homeFrame));
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                cancelAndReturn(homeFrame);
            }
        });

        server.setStatusListener(new GameServer.ServerStatusListener() {
            @Override
            public void onPlayerJoined(int count, int total) {
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("Players connected: " + count + " / " + total);
                    appendLog("Player " + count + " connected!");
                });
            }

            @Override
            public void onGameStarted() {
                SwingUtilities.invokeLater(() -> {
                    appendLog("All players connected. Game starting!");
                    new javax.swing.Timer(400, ev -> {
                        ((javax.swing.Timer) ev.getSource()).stop();
                        setVisible(false);
                        dispose();
                    }).start();
                });
            }

            @Override
            public void onPlayerDisconnected(int idx) {
                SwingUtilities.invokeLater(() ->
                        appendLog("Player " + (idx + 1) + " disconnected."));
            }
        });

        appendLog("Server starting on port " + server.getPort() + "...");
        serverThread = new Thread(() -> {
            try {
                server.bind();
                SwingUtilities.invokeLater(() ->
                        appendLog("Port ready. Host connecting as Player 1..."));
                NetworkGameFrame.openAsClient("localhost", server.getPort(), homeFrame);
                server.acceptRemainingPlayers();
            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                        appendLog("Server error: " + e.getMessage()));
            }
        }, "GameServerThread");
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private void appendLog(String text) {
        txtLog.append(text + "\n");
        txtLog.setCaretPosition(txtLog.getDocument().getLength());
    }

    private void cancelAndReturn(JFrame homeFrame) {
        server.stop();
        dispose();
        if (homeFrame instanceof MainMenuFrame) {
            ((MainMenuFrame) homeFrame).showHomeAgain();
        } else if (homeFrame != null) {
            homeFrame.setVisible(true);
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