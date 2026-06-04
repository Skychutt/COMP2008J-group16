package com.monopolydeal.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * LAN Battle Settings Panel
 */
public class LanSetupPanel extends JPanel {

    public interface LanSetupListener {
        void onBack();
        void onHost(int playerCount, List<String> playerNames, int port);
        void onJoin(String host, int port);
    }

    private static final int DEFAULT_PORT = 12345;
    private static final String CARD_HOST = "host";
    private static final String CARD_JOIN = "join";

    private final LanSetupListener listener;

    private final JRadioButton rbHost;
    private final JRadioButton rbJoin;

    private final CardLayout contentCardLayout;
    private final JPanel contentCard;

    private final JButton btnDecCount;
    private final JButton btnIncCount;
    private final JLabel lblCount;
    private int selectedCount = 2;
    private final JPanel namesContainer;
    private final List<JTextField> nameFields = new ArrayList<>();
    private final JTextField txtHostPort;

    private final JTextField txtJoinIp;
    private final JTextField txtJoinPort;

    private final JButton btnAction;
    private final JButton btnBack;

    public LanSetupPanel(LanSetupListener listener) {
        this.listener = listener;

        setOpaque(true);
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 2, true),
                BorderFactory.createEmptyBorder(24, 40, 24, 40)
        ));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("LAN Multiplayer");
        title.setFont(UITheme.FONT_MENU_SUBTITLE);
        title.setForeground(Color.BLACK);
        title.setAlignmentX(CENTER_ALIGNMENT);
        add(title);
        add(Box.createVerticalStrut(16));

        rbHost = new JRadioButton("Host a game");
        rbJoin = new JRadioButton("Join a game");
        rbHost.setFont(UITheme.FONT_BODY);
        rbJoin.setFont(UITheme.FONT_BODY);
        rbHost.setOpaque(false);
        rbJoin.setOpaque(false);
        rbHost.setForeground(Color.BLACK);
        rbJoin.setForeground(Color.BLACK);
        rbHost.setSelected(true);

        ButtonGroup group = new ButtonGroup();
        group.add(rbHost);
        group.add(rbJoin);

        JPanel modeRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        modeRow.setOpaque(false);
        modeRow.add(rbHost);
        modeRow.add(rbJoin);
        modeRow.setAlignmentX(CENTER_ALIGNMENT);
        add(modeRow);
        add(Box.createVerticalStrut(14));

        contentCardLayout = new CardLayout();
        contentCard = new JPanel(contentCardLayout);
        contentCard.setOpaque(false);
        contentCard.setMaximumSize(new Dimension(420, 240));
        contentCard.setPreferredSize(new Dimension(380, 240));
        contentCard.setAlignmentX(CENTER_ALIGNMENT);

        JPanel hostPanel = new JPanel();
        hostPanel.setOpaque(false);
        hostPanel.setLayout(new BoxLayout(hostPanel, BoxLayout.Y_AXIS));

        btnDecCount = makeSymbolButton("-");
        btnIncCount = makeSymbolButton("+");

        lblCount = new JLabel("2 Players");
        lblCount.setFont(UITheme.FONT_BODY);
        lblCount.setForeground(Color.BLACK);

        JPanel countRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        countRow.setOpaque(false);
        countRow.add(makeLabel("Players:"));
        countRow.add(btnDecCount);
        countRow.add(lblCount);
        countRow.add(btnIncCount);
        hostPanel.add(countRow);
        hostPanel.add(Box.createVerticalStrut(10));

        namesContainer = new JPanel(new GridLayout(0, 1, 0, 6));
        namesContainer.setOpaque(false);
        namesContainer.setAlignmentX(CENTER_ALIGNMENT);
        hostPanel.add(namesContainer);
        hostPanel.add(Box.createVerticalStrut(8));

        JPanel hostPortRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        hostPortRow.setOpaque(false);
        txtHostPort = new JTextField(String.valueOf(DEFAULT_PORT), 6);
        txtHostPort.setFont(UITheme.FONT_BODY);
        hostPortRow.add(makeLabel("Port:"));
        hostPortRow.add(txtHostPort);
        hostPanel.add(hostPortRow);

        contentCard.add(hostPanel, CARD_HOST);

        JPanel joinPanel = new JPanel();
        joinPanel.setOpaque(false);
        joinPanel.setLayout(new BoxLayout(joinPanel, BoxLayout.Y_AXIS));
        joinPanel.add(Box.createVerticalStrut(30));

        JPanel ipRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        ipRow.setOpaque(false);
        txtJoinIp = new JTextField("192.168.1.100", 14);
        txtJoinIp.setFont(UITheme.FONT_BODY);
        ipRow.add(makeLabel("Server IP:"));
        ipRow.add(txtJoinIp);
        joinPanel.add(ipRow);
        joinPanel.add(Box.createVerticalStrut(10));

        JPanel joinPortRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        joinPortRow.setOpaque(false);
        txtJoinPort = new JTextField(String.valueOf(DEFAULT_PORT), 6);
        txtJoinPort.setFont(UITheme.FONT_BODY);
        joinPortRow.add(makeLabel("Port:"));
        joinPortRow.add(txtJoinPort);
        joinPanel.add(joinPortRow);

        contentCard.add(joinPanel, CARD_JOIN);

        add(contentCard);
        add(Box.createVerticalStrut(18));

        btnBack   = new JButton("Back");
        btnAction = new JButton("Host Game");
        UITheme.styleMenuButton(btnBack);
        UITheme.styleMenuButton(btnAction);
        btnBack.setMaximumSize(new Dimension(200, 40));
        btnAction.setMaximumSize(new Dimension(200, 40));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        actions.setOpaque(false);
        actions.add(btnBack);
        actions.add(btnAction);
        add(actions);

        rbHost.addActionListener(e -> switchMode(true));
        rbJoin.addActionListener(e -> switchMode(false));
        btnDecCount.addActionListener(e -> adjustCount(-1));
        btnIncCount.addActionListener(e -> adjustCount(+1));
        btnBack.addActionListener(e -> listener.onBack());
        btnAction.addActionListener(e -> onActionClicked());

        rebuildNameFields();
        contentCardLayout.show(contentCard, CARD_HOST);
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }

    /**
     * Draw Symbol Button
     */
    private static JButton makeSymbolButton(String symbol) {
        JButton btn = new JButton() {
            private boolean hovered = false;
            private boolean pressed = false;

            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(java.awt.event.MouseEvent e)  { hovered = false; repaint(); }
                    @Override public void mousePressed(java.awt.event.MouseEvent e) { pressed = true; repaint(); }
                    @Override public void mouseReleased(java.awt.event.MouseEvent e){ pressed = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                // background
                Color bg = pressed ? new Color(200, 200, 200)
                         : hovered ? new Color(230, 230, 230)
                         : Color.WHITE;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);

                // border
                g2.setColor(new Color(160, 160, 160));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);

                // Symbolic text
                Font font = new Font("Arial", Font.BOLD, 16);
                g2.setFont(font);
                g2.setColor(Color.BLACK);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(symbol)) / 2;
                int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(symbol, tx, ty);

                g2.dispose();
            }

            @Override
            protected void paintBorder(Graphics g) {
            }
        };

        btn.setText("");
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(36, 28));
        btn.setMinimumSize(new Dimension(36, 28));
        btn.setMaximumSize(new Dimension(36, 28));
        btn.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));

        btn.putClientProperty("symbol", symbol);

        return btn;
    }

    private void switchMode(boolean hostMode) {
        contentCardLayout.show(contentCard, hostMode ? CARD_HOST : CARD_JOIN);
        btnAction.setText(hostMode ? "Host Game" : "Join Game");
    }

    private void adjustCount(int delta) {
        selectedCount = Math.max(2, Math.min(5, selectedCount + delta));
        lblCount.setText(selectedCount + " Players");
        rebuildNameFields();
    }

    private void rebuildNameFields() {
        namesContainer.removeAll();
        nameFields.clear();
        for (int i = 0; i < selectedCount; i++) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            row.setOpaque(false);
            JLabel lbl = makeLabel("Player " + (i + 1) + ":");
            lbl.setPreferredSize(new Dimension(68, 22));
            JTextField tf = new JTextField("Player " + (i + 1), 16);
            tf.setFont(UITheme.FONT_BODY);
            tf.setForeground(Color.BLACK);
            nameFields.add(tf);
            row.add(lbl);
            row.add(tf);
            namesContainer.add(row);
        }
        namesContainer.revalidate();
        namesContainer.repaint();
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

    private static JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.FONT_BODY);
        lbl.setForeground(Color.BLACK);
        return lbl;
    }
}