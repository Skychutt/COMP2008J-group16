package com.monopolydeal.gui;

import com.monopolydeal.model.Deck;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.card.Card;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.TransferHandler;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.util.function.IntConsumer;

/**
 * Center table hub: deck/discard, turn status, drop-to-play zone, and recent action.
 */
public class TopStatusPanel extends JPanel {
    private static final Color DROP_ZONE_PLAY_BG = UITheme.DROP_ZONE;
    private static final Color DROP_ZONE_PLAY_BORDER = UITheme.DROP_ZONE_BORDER;
    private static final Color DROP_ZONE_PLAY_HOVER_BORDER = new Color(126, 84, 24);
    private static final Color DROP_ZONE_DISCARD_BG = new Color(168, 56, 56);
    private static final Color DROP_ZONE_DISCARD_BORDER = new Color(176, 54, 54);
    private static final Color DROP_ZONE_DISCARD_TEXT = Color.WHITE;
    private static final Color DROP_ZONE_DISCARD_HINT = new Color(255, 235, 235);

    private final JLabel lblCurrentPlayer;
    private final JLabel lblActions;
    private final JLabel lblDrawTop;
    private final JLabel lblDrawCount;
    private final JLabel lblDiscardTop;
    private final JLabel lblDiscardCount;
    private final JLabel lblDiscardHint;
    private final JLabel lblRecentEvent;
    private final JPanel dropZone;
    private final JLabel lblDropText;
    private final javax.swing.border.Border dropBorderNormal;
    private final javax.swing.border.Border dropBorderActive;
    private final javax.swing.border.Border dropBorderDiscard;

    private IntConsumer cardDropHandler;
    private boolean gameOver;
    private boolean discardMode;
    private int discardRemaining;

    public TopStatusPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel hub = new JPanel(new BorderLayout(10, 10));
        hub.setOpaque(true);
        hub.setBackground(UITheme.PANEL_BG);
        hub.setBorder(UITheme.createSectionBorder("Table Center"));
        add(hub, BorderLayout.CENTER);

        JPanel rowStatus = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 6));
        rowStatus.setOpaque(false);
        lblCurrentPlayer = new JLabel("Current Player: -");
        lblCurrentPlayer.setFont(new Font(UITheme.FONT_TITLE.getName(), Font.BOLD, 20));
        lblCurrentPlayer.setForeground(UITheme.TEXT_MAIN);

        lblActions = new JLabel("Actions: 0 / 3");
        lblActions.setFont(UITheme.FONT_TITLE);
        lblActions.setForeground(UITheme.ACCENT_DARK);
        rowStatus.add(lblCurrentPlayer);
        rowStatus.add(new JLabel("|"));
        rowStatus.add(lblActions);
        hub.add(rowStatus, BorderLayout.NORTH);

        JPanel middle = new JPanel(new BorderLayout(12, 10));
        middle.setOpaque(false);
        hub.add(middle, BorderLayout.CENTER);

        JPanel leftDeck = buildDeckSlot("Draw Pile");
        lblDrawTop = (JLabel) leftDeck.getClientProperty("card");
        lblDrawCount = (JLabel) leftDeck.getClientProperty("count");
        middle.add(leftDeck, BorderLayout.WEST);

        JPanel rightDiscard = buildDeckSlot("Discard Top");
        lblDiscardTop = (JLabel) rightDiscard.getClientProperty("card");
        lblDiscardCount = (JLabel) rightDiscard.getClientProperty("count");
        middle.add(rightDiscard, BorderLayout.EAST);

        JPanel centerPlay = new JPanel();
        centerPlay.setOpaque(false);
        centerPlay.setLayout(new BoxLayout(centerPlay, BoxLayout.Y_AXIS));

        dropZone = new JPanel(new BorderLayout());
        dropZone.setOpaque(true);
        dropZone.setBackground(DROP_ZONE_PLAY_BG);
        dropBorderNormal = BorderFactory.createLineBorder(DROP_ZONE_PLAY_BORDER, 2, true);
        dropBorderActive = BorderFactory.createLineBorder(DROP_ZONE_PLAY_HOVER_BORDER, 3, true);
        dropBorderDiscard = BorderFactory.createLineBorder(DROP_ZONE_DISCARD_BORDER, 3, true);
        dropZone.setBorder(dropBorderNormal);
        dropZone.setPreferredSize(new Dimension(430, 168));
        lblDropText = new JLabel("Drag Card Here To Play", JLabel.CENTER);
        lblDropText.setFont(new Font(UITheme.FONT_TITLE.getName(), Font.BOLD, 20));
        lblDropText.setForeground(UITheme.TEXT_MAIN);
        dropZone.add(lblDropText, BorderLayout.CENTER);
        dropZone.setTransferHandler(new CardDropTransferHandler());

        lblDiscardHint = new JLabel(" ", JLabel.CENTER);
        lblDiscardHint.setFont(new Font(UITheme.FONT_BODY.getName(), Font.BOLD, 14));
        lblDiscardHint.setForeground(DROP_ZONE_DISCARD_HINT);

        lblRecentEvent = new JLabel("Recent: -", JLabel.CENTER);
        lblRecentEvent.setFont(new Font(UITheme.FONT_BODY.getName(), Font.BOLD, 13));
        lblRecentEvent.setForeground(UITheme.TEXT_SUB);

        centerPlay.add(dropZone);
        centerPlay.add(Box.createVerticalStrut(10));
        centerPlay.add(lblDiscardHint);
        centerPlay.add(Box.createVerticalStrut(4));
        centerPlay.add(lblRecentEvent);
        middle.add(centerPlay, BorderLayout.CENTER);
    }

    private JPanel buildDeckSlot(String title) {
        JPanel slot = new JPanel();
        slot.setOpaque(false);
        slot.setLayout(new BoxLayout(slot, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(UITheme.FONT_SUBTITLE);
        lblTitle.setForeground(UITheme.TEXT_MAIN);
        lblTitle.setAlignmentX(CENTER_ALIGNMENT);

        JLabel image = new JLabel();
        image.setAlignmentX(CENTER_ALIGNMENT);
        image.setBorder(BorderFactory.createLineBorder(UITheme.BORDER, 1, true));

        JLabel lblCount = new JLabel(" ");
        lblCount.setFont(UITheme.FONT_SUBTITLE);
        lblCount.setForeground(UITheme.TEXT_MAIN);
        lblCount.setAlignmentX(CENTER_ALIGNMENT);

        slot.putClientProperty("card", image);
        slot.putClientProperty("count", lblCount);
        slot.add(lblTitle);
        slot.add(Box.createVerticalStrut(5));
        slot.add(image);
        slot.add(Box.createVerticalStrut(6));
        slot.add(lblCount);
        return slot;
    }

    public void setCardDropHandler(IntConsumer cardDropHandler) {
        this.cardDropHandler = cardDropHandler;
    }

    public void updateTableCenter(Player currentPlayer, CardImageResolver resolver, String latestEvent,
                                  boolean gameOver, boolean discardMode, int discardRemaining) {
        this.gameOver = gameOver;
        this.discardMode = discardMode;
        this.discardRemaining = discardRemaining;
        if (currentPlayer == null) {
            showEmptyTableState(latestEvent);
            return;
        }

        Deck deck = Deck.getInstance();
        lblCurrentPlayer.setText("Current Player: " + currentPlayer.getName());
        lblActions.setText("Actions: " + currentPlayer.getActions() + " / 3");
        updateDeckPreviews(deck, resolver);
        refreshDropZoneState(false);
        setRecentEventText(latestEvent);
    }

    private void showEmptyTableState(String latestEvent) {
        // No active turn yet: keep the table neutral and show only the latest message.
        lblCurrentPlayer.setText("Current Player: -");
        lblActions.setText("Actions: 0 / 3");
        lblDrawCount.setText("Remaining: 0");
        lblDiscardCount.setText("Total discarded: 0");
        lblDrawTop.setIcon(null);
        lblDiscardTop.setIcon(null);
        refreshDropZoneState(false);
        setRecentEventText(latestEvent);
    }

    private void updateDeckPreviews(Deck deck, CardImageResolver resolver) {
        int remaining = deck.drawPileSize();
        lblDrawTop.setIcon(resolver.getFallbackIcon(76, 118));
        lblDrawTop.setToolTipText("Draw pile — " + remaining + " card(s) left");
        lblDrawCount.setText("Remaining: " + remaining);

        int totalDiscarded = deck.getTotalDiscardedCount();
        lblDiscardCount.setText("Total discarded: " + totalDiscarded);

        Card discardTop = deck.getVisibleDiscardTop();
        if (discardTop == null) {
            lblDiscardTop.setIcon(resolver.getFallbackIcon(76, 118));
            lblDiscardTop.setToolTipText("No cards discarded yet");
            return;
        }

        lblDiscardTop.setIcon(resolver.getCardIcon(discardTop, 76, 118));
        String pileNote = deck.discardSize() > 0
                ? "Top of discard pile"
                : "Last discarded (placed under draw pile)";
        lblDiscardTop.setToolTipText(discardTop.getName() + " — " + pileNote);
    }

    /**
     * Switch the center zone between play mode and discard mode.
     * Discard mode uses a red highlight so the player knows the drop target changed.
     */
    private void refreshDropZoneState(boolean hovered) {
        if (discardMode) {
            dropZone.setBackground(DROP_ZONE_DISCARD_BG);
            dropZone.setBorder(dropBorderDiscard);
            lblDropText.setText("Drag Here To Discard");
            lblDropText.setForeground(DROP_ZONE_DISCARD_TEXT);
            lblDiscardHint.setText(discardRemaining > 0
                    ? "You still need to discard " + discardRemaining + " card(s)."
                    : "Discard the extra cards.");
            lblDiscardHint.setForeground(DROP_ZONE_DISCARD_HINT);
        } else {
            dropZone.setBackground(DROP_ZONE_PLAY_BG);
            dropZone.setBorder(hovered ? dropBorderActive : dropBorderNormal);
            lblDropText.setText("Drag Card Here To Play");
            lblDropText.setForeground(UITheme.TEXT_MAIN);
            lblDiscardHint.setText(" ");
        }
    }

    /**
     * Keep the recent-event label short so the center panel stays readable.
     */
    private void setRecentEventText(String latestEvent) {
        String event = latestEvent == null ? "-" : latestEvent.trim();
        if (event.isEmpty()) {
            event = "-";
        }
        if (event.length() > 84) {
            event = event.substring(0, 84) + "...";
        }
        lblRecentEvent.setText("Recent: " + event);
    }

    private class CardDropTransferHandler extends TransferHandler {
        @Override
        public boolean canImport(TransferSupport support) {
            boolean ok = support.isDrop() && !gameOver && support.isDataFlavorSupported(DataFlavor.stringFlavor);
            refreshDropZoneState(ok);
            return ok;
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            try {
                String cardIdText = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                int cardId = Integer.parseInt(cardIdText.trim());
                if (cardDropHandler != null) {
                    cardDropHandler.accept(cardId);
                }
                return true;
            } catch (Exception ignored) {
                return false;
            } finally {
                refreshDropZoneState(false);
            }
        }
    }
}

