package com.monopolydeal.gui;

import com.monopolydeal.model.Player;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.PropertyCard;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.TransferHandler;
import javax.swing.TransferHandler.TransferSupport;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Bottom player seat: fan-style hand cards with drag-to-play.
 */
public class PlayerPanel extends JPanel {
    private static final int CARD_W = 118;
    private static final int CARD_H = 178;
    private static final int DROP_ZONE_W = 170;
    private static final int DROP_ZONE_H = 100;
    /** Bank + Property + End Turn, with small gaps between. */
    private static final int RIGHT_DOCK_H = DROP_ZONE_H * 3 + 24;

    private final GameFrame mainFrame;
    private final JLabel lblSeat;
    private final JLabel lblActions;
    private final JPanel handCanvas;
    private final ButtonDropZone bankDropZone;
    private final ButtonDropZone propertyDropZone;
    private final ImageActionButton btnEndTurn;

    private IntConsumer bankDropHandler;
    private IntConsumer propertyDropHandler;
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
        setPreferredSize(new Dimension(1400, RIGHT_DOCK_H + 40));

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

        btnEndTurn = new ImageActionButton("End Turn.png", DROP_ZONE_W, DROP_ZONE_H, true);
        btnEndTurn.setImageOffsetX(-1);
        btnEndTurn.setToolTipText("End Turn");
        btnEndTurn.addActionListener(e -> {
            if (gameOver || endTurnHandler == null) {
                return;
            }
            endTurnHandler.run();
        });
        updateEndTurnButtonStyle(0, false, false);

        bankDropZone = new ButtonDropZone("Bank.png", DROP_ZONE_W, DROP_ZONE_H);
        bankDropZone.setImageOffsetY(6);
        bankDropZone.setTransferHandler(new BankDropTransferHandler());
        bankDropZone.setToolTipText("Drag money, action, or property cards here to bank as money");

        propertyDropZone = new ButtonDropZone("Property.png", DROP_ZONE_W, DROP_ZONE_H);
        propertyDropZone.setTransferHandler(new PropertyDropTransferHandler());
        propertyDropZone.setToolTipText("Drag property cards here to play");

        JPanel rightDock = new JPanel();
        rightDock.setLayout(new javax.swing.BoxLayout(rightDock, javax.swing.BoxLayout.Y_AXIS));
        rightDock.setOpaque(false);
        Dimension dockSize = new Dimension(DROP_ZONE_W + 8, RIGHT_DOCK_H);
        rightDock.setPreferredSize(dockSize);
        rightDock.setMinimumSize(dockSize);
        rightDock.setMaximumSize(new Dimension(DROP_ZONE_W + 8, RIGHT_DOCK_H));
        rightDock.setBorder(BorderFactory.createEmptyBorder(0, 8, 4, 0));
        alignDockButton(bankDropZone);
        alignDockButton(propertyDropZone);
        alignDockButton(btnEndTurn);
        rightDock.add(bankDropZone);
        rightDock.add(javax.swing.Box.createVerticalStrut(8));
        rightDock.add(propertyDropZone);
        rightDock.add(javax.swing.Box.createVerticalStrut(8));
        rightDock.add(btnEndTurn);
        add(rightDock, BorderLayout.EAST);
    }

    public void setBankDropHandler(IntConsumer bankDropHandler) {
        this.bankDropHandler = bankDropHandler;
    }

    public void setPropertyDropHandler(IntConsumer propertyDropHandler) {
        this.propertyDropHandler = propertyDropHandler;
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
            lblSeat.setText(current.getName() + " - Drag to center to discard " + this.discardRemaining + " card(s)");
        } else {
            lblSeat.setText(current.getName()
                    + " - Drag to center to play actions, bank (right) for money, property (right) for land cards");
        }
        refreshDropZoneHighlight(false, false);
        lblActions.setText("Actions: " + current.getActions() + " / 3");
        updateEndTurnButtonStyle(current.getActions(), gameOver, discardMode);
        renderHand(current);
    }

    private static void alignDockButton(java.awt.Component button) {
        if (button instanceof JComponent) {
            ((JComponent) button).setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        }
    }

    private void refreshDropZoneHighlight(boolean bankHovered, boolean propertyHovered) {
        boolean enabled = !gameOver && !discardMode;
        bankDropZone.setHighlight(enabled && bankHovered);
        bankDropZone.setHoverText(enabled && bankHovered ? "Move to Bank" : null);
        propertyDropZone.setHighlight(enabled && propertyHovered);
        propertyDropZone.setHoverText(enabled && propertyHovered ? "Move to Property" : null);
    }

    private void updateEndTurnButtonStyle(int actionsLeft, boolean gameOver, boolean discardMode) {
        if (actionsLeft == lastActionsLeft && gameOver == lastGameOver && discardMode == lastDiscardMode) {
            return;
        }

        if (gameOver || discardMode) {
            btnEndTurn.setEnabled(false);
            btnEndTurn.setVisualState(ImageActionButton.VisualState.DISABLED);
        } else if (actionsLeft > 0) {
            btnEndTurn.setEnabled(true);
            btnEndTurn.setVisualState(ImageActionButton.VisualState.WAITING);
        } else {
            btnEndTurn.setEnabled(true);
            btnEndTurn.setVisualState(ImageActionButton.VisualState.ENABLED);
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

        // Keep cards mostly fully visible with a light fan so the hand stays readable.
        int step = CARD_W - 10;
        int n = cards.size();
        int totalW = CARD_W + Math.max(0, n - 1) * step;
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
            cardLabel.setBounds(x, y, CARD_W, CARD_H);
            handCanvas.add(cardLabel);
        }

        handCanvas.revalidate();
        handCanvas.repaint();
    }

    private JLabel buildDraggableCardLabel(Player current, Card card) {
        JLabel label = new JLabel(mainFrame.getImageResolver().getCardIcon(card, CARD_W, CARD_H));
        label.setOpaque(true);
        label.setBackground(new Color(255, 255, 255, 18));
        label.setBorder(BorderFactory.createLineBorder(new Color(246, 229, 173), 1, true));
        label.setToolTipText(card.getName() + " (" + card.getValue() + "M)");

        boolean canPlay = mainFrame.getGameLogic().getRuleValidator().canPlayCard(current, card);
        boolean canBank = current.getActions() > 0
                && (!(card instanceof PropertyCard) || ((PropertyCard) card).canBankAsMoney());
        boolean canPlaceProperty = card instanceof PropertyCard && canPlay;
        boolean draggable = discardMode || canPlay || canBank;

        label.setEnabled(draggable);
        if (!draggable) {
            label.setBorder(BorderFactory.createLineBorder(new Color(130, 130, 130), 1, true));
            label.setCursor(Cursor.getDefaultCursor());
        } else {
            label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            if (discardMode) {
                label.setToolTipText(card.getName() + " (" + card.getValue() + "M) - Drag to center to discard");
            } else if (canPlaceProperty && canBank) {
                label.setBorder(BorderFactory.createLineBorder(new Color(76, 130, 68), 2, true));
                label.setToolTipText(card.getName() + " (" + card.getValue() + "M) - Property zone, center, or Bank");
            } else if (canPlaceProperty) {
                label.setBorder(BorderFactory.createLineBorder(new Color(76, 130, 68), 2, true));
                label.setToolTipText(card.getName() + " (" + card.getValue() + "M) - Drag to Property zone or center");
            } else if (!canPlay && canBank) {
                label.setBorder(BorderFactory.createLineBorder(UITheme.BANK_ZONE_BORDER, 2, true));
                label.setToolTipText(card.getName() + " (" + card.getValue() + "M) - Bank only");
            }
            CardDragTransferHandler handler = new CardDragTransferHandler(card.getId());
            label.setTransferHandler(handler);
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    // Use the card image itself as the drag image so the card follows the mouse.
                    handler.prepareDragImage(label, e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    handler.clearDragImage();
                }
            });
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

        void prepareDragImage(JLabel label, MouseEvent e) {
            if (label == null || label.getIcon() == null) {
                return;
            }
            setDragImage(createDragImage(label));
            // Center the hotspot so the card feels "pinched" instead of grabbed from a corner.
            setDragImageOffset(new Point(label.getWidth() / 2, label.getHeight() / 2));
        }

        void clearDragImage() {
            setDragImage(null);
        }

        /**
         * Paint the card label itself so the drag image matches the hand card exactly.
         */
        private java.awt.Image createDragImage(JLabel label) {
            int width = Math.max(1, label.getWidth());
            int height = Math.max(1, label.getHeight());
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = image.createGraphics();
            label.paint(g2);
            g2.dispose();
            return image;
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
            if (gameOver || discardMode || !support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                refreshDropZoneHighlight(false, false);
                return false;
            }
            Card card = findDraggedCard(support);
            boolean ok = card != null && canDropOnBank(card);
            refreshDropZoneHighlight(ok, false);
            return ok;
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            try {
                int cardId = parseCardId(support);
                if (bankDropHandler != null) {
                    bankDropHandler.accept(cardId);
                }
                return true;
            } catch (Exception ignored) {
                return false;
            } finally {
                refreshDropZoneHighlight(false, false);
            }
        }
    }

    private class PropertyDropTransferHandler extends TransferHandler {
        @Override
        public boolean canImport(TransferSupport support) {
            if (gameOver || discardMode || !support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                refreshDropZoneHighlight(false, false);
                return false;
            }
            Card card = findDraggedCard(support);
            boolean ok = card instanceof PropertyCard;
            refreshDropZoneHighlight(false, ok);
            return ok;
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            try {
                int cardId = parseCardId(support);
                if (propertyDropHandler != null) {
                    propertyDropHandler.accept(cardId);
                }
                return true;
            } catch (Exception ignored) {
                return false;
            } finally {
                refreshDropZoneHighlight(false, false);
            }
        }
    }

    private Card findDraggedCard(TransferSupport support) {
        try {
            int cardId = parseCardId(support);
            Player current = mainFrame.getGameManager().getCurrentPlayer();
            if (current == null) {
                return null;
            }
            return current.getHand().findCard(cardId);
        } catch (Exception ex) {
            return null;
        }
    }

    private static int parseCardId(TransferSupport support) throws Exception {
        String cardIdText = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
        return Integer.parseInt(cardIdText.trim());
    }

    private static boolean canDropOnBank(Card card) {
        if (card == null) {
            return false;
        }
        if (!(card instanceof PropertyCard)) {
            return true;
        }
        return ((PropertyCard) card).canBankAsMoney();
    }
}

