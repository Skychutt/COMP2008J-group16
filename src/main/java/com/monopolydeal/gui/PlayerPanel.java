package com.monopolydeal.gui;

import com.monopolydeal.model.Player;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.PropertyCard;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.TransferHandler;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Bottom player seat: fan-style hand cards with drag-to-play.
 */
public class PlayerPanel extends JPanel {
    private static final Color END_TURN_READY_BG = new Color(45, 143, 76);
    private static final Color END_TURN_READY_TEXT = Color.WHITE;
    private static final Color END_TURN_WAIT_BG = new Color(242, 197, 67);
    private static final Color END_TURN_WAIT_TEXT = new Color(56, 43, 16);
    private static final Color END_TURN_DISABLED_BG = new Color(101, 101, 101);
    private static final Color END_TURN_DISABLED_TEXT = new Color(232, 232, 232);
    private static final Color BANK_ZONE_DISCARD_BG = new Color(225, 225, 225);
    private static final Color BANK_ZONE_DISCARD_BORDER = new Color(165, 165, 165);

    private final GameFrame mainFrame;
    private final JLabel lblSeat;
    private final JLabel lblActions;
    private final JPanel handCanvas;
    private final JPanel bankDropZone;
    private final JButton btnEndTurn;

    private IntConsumer bankDropHandler;
    private Runnable endTurnHandler;
    private boolean gameOver;
    private boolean discardMode;
    private int discardRemaining;
    private int lastActionsLeft = Integer.MIN_VALUE;
    private boolean lastGameOver;
    private boolean lastDiscardMode;

    public PlayerPanel(GameFrame mainFrame) {
        this.mainFrame = mainFrame;
        setOpaque(false);
        setLayout(new BorderLayout(10, 6));
        setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        setPreferredSize(new Dimension(1400, 270));

        lblSeat = new JLabel("Your Seat");
        lblSeat.setFont(UITheme.FONT_SUBTITLE);
        lblSeat.setForeground(new Color(250, 241, 209));
        lblActions = new JLabel("Actions: 0 / 3");
        lblActions.setFont(UITheme.FONT_BODY);
        lblActions.setForeground(new Color(252, 239, 197));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(lblSeat, BorderLayout.WEST);
        header.add(lblActions, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        handCanvas = new JPanel(null);
        handCanvas.setOpaque(false);
        handCanvas.setPreferredSize(new Dimension(1040, 220));

        JScrollPane scroll = new JScrollPane(handCanvas);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(176, 142, 75, 120), 1, true));
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.setPreferredSize(new Dimension(1040, 220));
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        JPanel handWrapper = new JPanel(new BorderLayout());
        handWrapper.setOpaque(false);
        handWrapper.add(scroll, BorderLayout.CENTER);
        add(handWrapper, BorderLayout.CENTER);

        btnEndTurn = new JButton("End Turn");
        btnEndTurn.setFocusPainted(false);
        btnEndTurn.setOpaque(true);
        btnEndTurn.setContentAreaFilled(true);
        btnEndTurn.setBorderPainted(false);
        btnEndTurn.setFont(UITheme.FONT_SUBTITLE);
        btnEndTurn.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));
        btnEndTurn.setPreferredSize(new Dimension(170, 48));
        btnEndTurn.addActionListener(e -> {
            if (gameOver || endTurnHandler == null) {
                return;
            }
            endTurnHandler.run();
        });
        updateEndTurnButtonStyle(0, false, false);

        bankDropZone = new JPanel(new BorderLayout());
        bankDropZone.setOpaque(true);
        bankDropZone.setBackground(UITheme.BANK_ZONE);
        bankDropZone.setBorder(BorderFactory.createLineBorder(UITheme.BANK_ZONE_BORDER, 2, true));
        bankDropZone.setMaximumSize(new Dimension(170, 120));
        bankDropZone.setPreferredSize(new Dimension(170, 120));
        JLabel bankDropText = new JLabel("<html><center>Drag Here<br/>To Bank</center></html>", JLabel.CENTER);
        bankDropText.setFont(UITheme.FONT_SUBTITLE);
        bankDropText.setForeground(UITheme.TEXT_MAIN);
        bankDropZone.add(bankDropText, BorderLayout.CENTER);
        bankDropZone.setTransferHandler(new BankDropTransferHandler());

        JPanel rightDock = new JPanel(new BorderLayout(0, 8));
        rightDock.setOpaque(false);
        rightDock.setPreferredSize(new Dimension(190, 240));
        rightDock.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        rightDock.add(bankDropZone, BorderLayout.NORTH);
        rightDock.add(btnEndTurn, BorderLayout.SOUTH);
        add(rightDock, BorderLayout.EAST);
    }

    public void setBankDropHandler(IntConsumer bankDropHandler) {
        this.bankDropHandler = bankDropHandler;
    }

    public void setEndTurnHandler(Runnable endTurnHandler) {
        this.endTurnHandler = endTurnHandler;
    }

    public void updatePlayerView(Player current, boolean gameOver, boolean discardMode, int discardRemaining) {
        if (current == null) {
            return;
        }
        this.gameOver = gameOver;
        this.discardMode = discardMode;
        this.discardRemaining = Math.max(0, discardRemaining);
        if (discardMode) {
            lblSeat.setText(current.getName() + " - Discard " + this.discardRemaining + " card(s) by dragging to center");
        } else {
            lblSeat.setText(current.getName() + " - Drag to center to PLAY, or drag valuable cards to BANK");
        }
        refreshBankDropZoneStyle(false);
        lblActions.setText("Actions: " + current.getActions() + " / 3");
        updateEndTurnButtonStyle(current.getActions(), gameOver, discardMode);
        renderHand(current);
    }

    private void refreshBankDropZoneStyle(boolean hovered) {
        if (discardMode) {
            bankDropZone.setBackground(BANK_ZONE_DISCARD_BG);
            bankDropZone.setBorder(BorderFactory.createLineBorder(BANK_ZONE_DISCARD_BORDER, 2, true));
            return;
        }
        bankDropZone.setBackground(hovered ? UITheme.BANK_ZONE_ACTIVE : UITheme.BANK_ZONE);
        bankDropZone.setBorder(BorderFactory.createLineBorder(UITheme.BANK_ZONE_BORDER, 2, true));
    }

    private void updateEndTurnButtonStyle(int actionsLeft, boolean gameOver, boolean discardMode) {
        if (actionsLeft == lastActionsLeft && gameOver == lastGameOver && discardMode == lastDiscardMode) {
            return;
        }

        btnEndTurn.setText("End Turn");
        // Match the Hearthstone-style cue: yellow means you can still act, green means end the turn now.
        if (gameOver || discardMode) {
            btnEndTurn.setEnabled(false);
            btnEndTurn.setBackground(END_TURN_DISABLED_BG);
            btnEndTurn.setForeground(END_TURN_DISABLED_TEXT);
        } else if (actionsLeft > 0) {
            btnEndTurn.setEnabled(true);
            btnEndTurn.setBackground(END_TURN_WAIT_BG);
            btnEndTurn.setForeground(END_TURN_WAIT_TEXT);
        } else {
            btnEndTurn.setEnabled(true);
            btnEndTurn.setBackground(END_TURN_READY_BG);
            btnEndTurn.setForeground(END_TURN_READY_TEXT);
        }

        lastActionsLeft = actionsLeft;
        lastGameOver = gameOver;
        lastDiscardMode = discardMode;
    }

    private void renderHand(Player current) {
        handCanvas.removeAll();

        List<Card> cards = new ArrayList<>(current.getHand().getCards());
        if (cards.isEmpty()) {
            JLabel empty = new JLabel("No cards in hand");
            empty.setFont(UITheme.FONT_BODY);
            empty.setForeground(new Color(250, 241, 209));
            empty.setBounds(12, 24, 180, 24);
            handCanvas.add(empty);
            handCanvas.revalidate();
            handCanvas.repaint();
            return;
        }

        int cardW = 118;
        int cardH = 178;
        // Keep cards mostly fully visible (Hearthstone/STS-style arc with very light overlap).
        int step = cardW - 10;
        int n = cards.size();
        int totalW = cardW + Math.max(0, n - 1) * step;
        int canvasW = Math.max(1040, totalW + 80);
        handCanvas.setPreferredSize(new Dimension(canvasW, 220));

        int startX = (canvasW - totalW) / 2;
        double middle = (n - 1) / 2.0;
        for (int i = 0; i < n; i++) {
            Card card = cards.get(i);
            int x = startX + i * step;
            // Keep most of each card visible so the hand reads like a fanned row, not a stack.
            double d = Math.abs(i - middle);
            int y = 14 + (int) (d * d * 2.2);
            JLabel cardLabel = buildDraggableCardLabel(current, card);
            cardLabel.setBounds(x, y, cardW, cardH);
            handCanvas.add(cardLabel);
        }

        handCanvas.revalidate();
        handCanvas.repaint();
    }

    private JLabel buildDraggableCardLabel(Player current, Card card) {
        JLabel label = new JLabel(mainFrame.getImageResolver().getCardIcon(card, 118, 178));
        label.setOpaque(true);
        label.setBackground(new Color(255, 255, 255, 18));
        label.setBorder(BorderFactory.createLineBorder(new Color(246, 229, 173), 1, true));
        label.setToolTipText(card.getName() + " (" + card.getValue() + "M)");

        boolean canPlay = mainFrame.getGameLogic().getRuleValidator().canPlayCard(current, card);
        boolean canBank = current.getActions() > 0 && !(card instanceof PropertyCard);
        boolean draggable = discardMode || canPlay || canBank;

        label.setEnabled(draggable);
        if (!draggable) {
            label.setBorder(BorderFactory.createLineBorder(new Color(130, 130, 130), 1, true));
            label.setCursor(Cursor.getDefaultCursor());
        } else {
            label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            if (discardMode) {
                label.setToolTipText(card.getName() + " (" + card.getValue() + "M) - drag to center to discard");
            } else if (!canPlay && canBank) {
                label.setBorder(BorderFactory.createLineBorder(UITheme.BANK_ZONE_BORDER, 2, true));
                label.setToolTipText(card.getName() + " (" + card.getValue() + "M) - Bank only");
            }
            label.setTransferHandler(new CardDragTransferHandler(card.getId()));
            label.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    JComponent src = (JComponent) e.getSource();
                    src.getTransferHandler().exportAsDrag(src, e, TransferHandler.COPY);
                }
            });
        }
        return label;
    }

    private static class CardDragTransferHandler extends TransferHandler {
        private final int cardId;

        CardDragTransferHandler(int cardId) {
            this.cardId = cardId;
        }

        @Override
        protected Transferable createTransferable(javax.swing.JComponent c) {
            return new StringSelection(String.valueOf(cardId));
        }

        @Override
        public int getSourceActions(javax.swing.JComponent c) {
            return COPY;
        }
    }

    private class BankDropTransferHandler extends TransferHandler {
        @Override
        public boolean canImport(TransferSupport support) {
            boolean ok = !gameOver && !discardMode && support.isDataFlavorSupported(DataFlavor.stringFlavor);
            refreshBankDropZoneStyle(ok);
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
                if (bankDropHandler != null) {
                    bankDropHandler.accept(cardId);
                }
                return true;
            } catch (Exception ignored) {
                return false;
            } finally {
                refreshBankDropZoneStyle(false);
            }
        }
    }
}
