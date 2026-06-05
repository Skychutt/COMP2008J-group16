package com.monopolydeal.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.util.List;

/**
 * Home screen: main background image with menu or local setup overlay.
 */
public class MainMenuPanel extends JPanel {

    private static final String CARD_MENU = "menu";
    private static final String CARD_SETUP = "setup";
    private static final String CARD_LAN   = "lan";

    private final Image background;
    private final CardLayout cardLayout;
    private final JPanel cardHost;
    private final JPanel menuCard;
    private final LocalGameSetupPanel setupPanel;
    private LanSetupPanel lanSetupPanel;

    public MainMenuPanel(LocalGameSetupPanel.SetupListener setupListener) {
        background = BackgroundUtil.getMainBackground();
        setLayout(new GridBagLayout());
        setOpaque(true);

        cardLayout = new CardLayout();
        cardHost = new JPanel(cardLayout);
        cardHost.setOpaque(false);

        menuCard = buildMenuOverlay();
        setupPanel = new LocalGameSetupPanel(setupListener);

        cardHost.add(menuCard, CARD_MENU);
        cardHost.add(setupPanel, CARD_SETUP);

        add(cardHost, createCenterConstraints());
        showMainMenu();
    }

    private GridBagConstraints createCenterConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        return gbc;
    }

    public void showMainMenu() {
        cardLayout.show(cardHost, CARD_MENU);
    }

    public void showLocalSetup() {
        setupPanel.resetToDefaults();
        cardLayout.show(cardHost, CARD_SETUP);
    }

    /**
     * Display the LAN settings panel
     */
    public void showLanSetup(LanSetupPanel.LanSetupListener listener) {
        if (lanSetupPanel == null) {
            lanSetupPanel = new LanSetupPanel(listener);
            cardHost.add(lanSetupPanel, CARD_LAN);
        } else {
            lanSetupPanel.resetToDefaults();
        }
        cardLayout.show(cardHost, CARD_LAN);
    }

    private JPanel buildMenuOverlay() {
        JPanel overlay = new JPanel();
        overlay.setOpaque(true);
        overlay.setBackground(new Color(255, 255, 255, 200));
        overlay.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 2, true),
                BorderFactory.createEmptyBorder(28, 48, 28, 48)
        ));
        overlay.setLayout(new BoxLayout(overlay, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("MONOPOLY DEAL");
        title.setFont(UITheme.FONT_MENU_TITLE);
        title.setForeground(Color.BLACK);
        title.setAlignmentX(CENTER_ALIGNMENT);
        overlay.add(title);

        JLabel subtitle = new JLabel("Card Game");
        subtitle.setFont(UITheme.FONT_MENU_SUBTITLE);
        subtitle.setForeground(Color.BLACK);
        subtitle.setAlignmentX(CENTER_ALIGNMENT);
        overlay.add(Box.createVerticalStrut(6));
        overlay.add(subtitle);
        overlay.add(Box.createVerticalStrut(28));

        overlay.add(createMenuButton("Local Game"));
        overlay.add(Box.createVerticalStrut(12));
        overlay.add(createMenuButton("Single Player"));
        overlay.add(Box.createVerticalStrut(12));
        overlay.add(createMenuButton("Online Multiplayer"));
        overlay.add(Box.createVerticalStrut(12));
        overlay.add(createMenuButton("Game Rules"));

        return overlay;
    }

    private JButton createMenuButton(String text) {
        JButton button = new JButton(text);
        button.setAlignmentX(CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(280, 46));
        button.setPreferredSize(new Dimension(280, 46));
        button.setMinimumSize(new Dimension(280, 46));
        UITheme.styleMenuButton(button);
        return button;
    }

    public JButton getButtonByText(String text) {
        return findButton(menuCard, text);
    }

    private JButton findButton(java.awt.Container parent, String text) {
        for (int i = 0; i < parent.getComponentCount(); i++) {
            java.awt.Component child = parent.getComponent(i);
            if (child instanceof JButton) {
                JButton btn = (JButton) child;
                if (text.equals(btn.getText())) {
                    return btn;
                }
            }
            if (child instanceof java.awt.Container) {
                JButton found = findButton((java.awt.Container) child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        BackgroundUtil.enableQuality(g2);

        int w = getWidth();
        int h = getHeight();
        if (background != null && w > 0 && h > 0) {
            BackgroundUtil.paintCover(g2, background, w, h);
        } else {
            g2.setColor(UITheme.PAGE_BG);
            g2.fillRect(0, 0, w, h);
        }
        g2.dispose();
    }
}
