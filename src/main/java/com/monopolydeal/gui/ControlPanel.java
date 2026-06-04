package com.monopolydeal.gui;

import com.monopolydeal.model.Player;
import com.monopolydeal.model.PropertySet;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.PropertyCard;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Left-bottom info area: self assets + log.
 */
public class ControlPanel extends JPanel {

    private final GameFrame mainFrame;
    private final JLabel lblActionsLeft;
    private final JLabel lblBankTotal;
    private final JPanel pnlBank;
    private final JPanel pnlProperty;
    private final JTextArea txtGameLog;

    public ControlPanel(GameFrame mainFrame) {
        this.mainFrame = mainFrame;
        setOpaque(true);
        setBackground(UITheme.PANEL_BG);
        setPreferredSize(new Dimension(330, 420));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        lblActionsLeft = new JLabel("Actions: 0 / 3");
        lblActionsLeft.setFont(UITheme.FONT_SUBTITLE);
        lblActionsLeft.setForeground(UITheme.ACCENT_DARK);
        lblActionsLeft.setAlignmentX(LEFT_ALIGNMENT);
        add(lblActionsLeft);

        lblBankTotal = new JLabel("Bank: 0M");
        lblBankTotal.setFont(UITheme.FONT_BODY);
        lblBankTotal.setForeground(UITheme.TEXT_SUB);
        lblBankTotal.setAlignmentX(LEFT_ALIGNMENT);
        add(lblBankTotal);
        add(Box.createVerticalStrut(8));

        pnlBank = createStripPanel();
        add(createSectionBox("Bank Cards", pnlBank, 96));
        add(Box.createVerticalStrut(6));

        pnlProperty = createStripPanel();
        add(createSectionBox("Property Cards", pnlProperty, 120));
        add(Box.createVerticalStrut(6));

        txtGameLog = new JTextArea(8, 26);
        configureLogArea(txtGameLog);
        add(createLogScroll(txtGameLog));
    }

    public void updateTurnStatus(Player currentPlayer) {
        lblActionsLeft.setText(currentPlayer == null
                ? "Actions: 0 / 3"
                : "Actions: " + currentPlayer.getActions() + " / 3");
    }

    public void updateSelfAssets(Player currentPlayer) {
        pnlBank.removeAll();
        pnlProperty.removeAll();

        if (currentPlayer == null) {
            lblBankTotal.setText("Bank: 0M");
            pnlBank.revalidate();
            pnlProperty.revalidate();
            pnlBank.repaint();
            pnlProperty.repaint();
            return;
        }

        lblBankTotal.setText("Bank: " + currentPlayer.getBankArea().total() + "M");

        renderCardStrip(pnlBank, new ArrayList<>(currentPlayer.getBankArea().getMoney()), 40, 60, 8);

        renderCardStrip(pnlProperty, collectPropertyCards(currentPlayer), 40, 60, 10);

        pnlBank.revalidate();
        pnlProperty.revalidate();
        pnlBank.repaint();
        pnlProperty.repaint();
    }

    /**
     * Keep the asset rows compact by showing only a short preview of each pile.
     */
    private void renderCardStrip(JPanel panel, List<Card> cards, int width, int height, int maxShow) {
        if (cards.isEmpty()) {
            JLabel none = new JLabel("-");
            none.setFont(UITheme.FONT_SMALL);
            none.setForeground(UITheme.TEXT_SUB);
            panel.add(none);
            return;
        }

        int count = Math.min(cards.size(), maxShow);
        for (int i = 0; i < count; i++) {
            Card card = cards.get(i);
            JLabel icon = new JLabel(mainFrame.getImageResolver().getCardIcon(card, width, height));
            icon.setToolTipText(card.getName());
            panel.add(icon);
        }
        if (cards.size() > maxShow) {
            JLabel more = new JLabel("+" + (cards.size() - maxShow));
            more.setFont(UITheme.FONT_SMALL);
            more.setForeground(UITheme.TEXT_SUB);
            panel.add(more);
        }
    }

    private JPanel createSectionBox(String title, JPanel content, int height) {
        JPanel box = new JPanel(new BorderLayout());
        box.setOpaque(true);
        box.setBackground(UITheme.PANEL_SOFT_BG);
        box.setBorder(UITheme.createSectionBorder(title));
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        box.setPreferredSize(new Dimension(310, height));
        box.setAlignmentX(LEFT_ALIGNMENT);
        box.add(content, BorderLayout.CENTER);
        return box;
    }

    private JPanel createStripPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 3));
        panel.setOpaque(true);
        panel.setBackground(UITheme.PANEL_SOFT_BG);
        return panel;
    }

    private JScrollPane createLogScroll(JTextArea logArea) {
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setOpaque(true);
        logScroll.getViewport().setOpaque(true);
        logScroll.setBackground(UITheme.LOG_BG);
        logScroll.getViewport().setBackground(UITheme.LOG_BG);
        logScroll.setBorder(UITheme.createSectionBorder("Recent Log"));
        logScroll.setAlignmentX(LEFT_ALIGNMENT);
        return logScroll;
    }

    private void configureLogArea(JTextArea logArea) {
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setOpaque(true);
        logArea.setBackground(UITheme.LOG_BG);
        logArea.setForeground(UITheme.LOG_TEXT);
        logArea.setText("[System] Ready.\n");
    }

    private List<Card> collectPropertyCards(Player currentPlayer) {
        List<Card> cards = new ArrayList<>();
        for (PropertySet set : currentPlayer.getPropertyArea().getSets()) {
            for (PropertyCard card : set.getCards()) {
                cards.add(card);
            }
        }
        return cards;
    }

    public void logEvent(String event) {
        if (event == null || event.trim().isEmpty()) {
            return;
        }
        txtGameLog.append("[Event] " + event + "\n");
        trimLogLines(16);
        txtGameLog.setCaretPosition(txtGameLog.getDocument().getLength());
    }

    private void trimLogLines(int maxLines) {
        String[] lines = txtGameLog.getText().split("\n");
        if (lines.length <= maxLines) {
            return;
        }
        // Keep only the newest log lines so the panel stays readable.
        StringBuilder sb = new StringBuilder();
        for (int i = lines.length - maxLines; i < lines.length; i++) {
            sb.append(lines[i]).append('\n');
        }
        txtGameLog.setText(sb.toString());
    }
}

