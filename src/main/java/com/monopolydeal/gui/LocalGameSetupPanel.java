package com.monopolydeal.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Local game lobby on top of the main menu background: player count dropdown and name fields.
 */
public class LocalGameSetupPanel extends JPanel {

    public interface SetupListener {
        void onBack();

        void onStart(int playerCount, List<String> playerNames);
    }

    private static final String[] PLAYER_COUNT_OPTIONS = {
            "2 Players", "3 Players", "4 Players", "5 Players"
    };

    private final SetupListener listener;
    private final JComboBox<String> playerCountCombo;
    private final JPanel namesContainer;
    private final List<JTextField> nameFields;

    public LocalGameSetupPanel(SetupListener listener) {
        this.listener = listener;
        this.nameFields = new ArrayList<JTextField>();

        setOpaque(true);
        setBackground(new Color(255, 255, 255, 220));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 2, true),
                BorderFactory.createEmptyBorder(24, 40, 24, 40)
        ));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Local Game Setup");
        title.setFont(UITheme.FONT_MENU_SUBTITLE);
        title.setForeground(Color.BLACK);
        title.setAlignmentX(CENTER_ALIGNMENT);
        add(title);
        add(Box.createVerticalStrut(16));

        JPanel countRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        countRow.setOpaque(false);
        JLabel countLabel = new JLabel("Number of players:");
        countLabel.setFont(UITheme.FONT_BODY);
        countLabel.setForeground(Color.BLACK);
        countRow.add(countLabel);

        playerCountCombo = new JComboBox<String>(PLAYER_COUNT_OPTIONS);
        playerCountCombo.setFont(UITheme.FONT_BODY);
        playerCountCombo.setPreferredSize(new Dimension(160, 28));
        playerCountCombo.setSelectedIndex(1);
        countRow.add(playerCountCombo);
        add(countRow);
        add(Box.createVerticalStrut(14));

        JLabel namesLabel = new JLabel("Player names:");
        namesLabel.setFont(UITheme.FONT_BODY);
        namesLabel.setForeground(Color.BLACK);
        namesLabel.setAlignmentX(CENTER_ALIGNMENT);
        add(namesLabel);
        add(Box.createVerticalStrut(8));

        namesContainer = new JPanel();
        namesContainer.setOpaque(false);
        namesContainer.setLayout(new GridLayout(0, 1, 0, 8));
        namesContainer.setAlignmentX(CENTER_ALIGNMENT);
        namesContainer.setMaximumSize(new Dimension(320, 220));
        add(namesContainer);
        add(Box.createVerticalStrut(20));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        actions.setOpaque(false);
        JButton back = new JButton("Back");
        JButton start = new JButton("Start Game");
        UITheme.styleMenuButton(back);
        UITheme.styleMenuButton(start);
        back.addActionListener(e -> listener.onBack());
        start.addActionListener(e -> onStartClicked());
        actions.add(back);
        actions.add(start);
        add(actions);

        playerCountCombo.addActionListener(e -> rebuildNameFields());
        rebuildNameFields();
    }

    public void resetToDefaults() {
        playerCountCombo.setSelectedIndex(1);
        rebuildNameFields();
    }

    private int getSelectedPlayerCount() {
        return playerCountCombo.getSelectedIndex() + 2;
    }

    private void rebuildNameFields() {
        int count = getSelectedPlayerCount();
        namesContainer.removeAll();
        nameFields.clear();

        for (int i = 0; i < count; i++) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            row.setOpaque(false);
            JLabel label = new JLabel("Player " + (i + 1) + ":");
            label.setFont(UITheme.FONT_BODY);
            label.setForeground(Color.BLACK);
            label.setPreferredSize(new Dimension(72, 24));

            JTextField field = new JTextField("Player " + (i + 1), 18);
            field.setFont(UITheme.FONT_BODY);
            field.setForeground(Color.BLACK);
            nameFields.add(field);

            row.add(label);
            row.add(field);
            namesContainer.add(row);
        }

        namesContainer.revalidate();
        namesContainer.repaint();
    }

    private void onStartClicked() {
        int count = getSelectedPlayerCount();
        List<String> names = new ArrayList<String>();
        for (int i = 0; i < nameFields.size(); i++) {
            JTextField field = nameFields.get(i);
            String text = field.getText();
            if (text == null || text.trim().length() == 0) {
                text = "Player " + (i + 1);
            }
            names.add(text.trim());
        }
        listener.onStart(count, names);
    }
}
