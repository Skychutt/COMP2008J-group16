package com.monopolydeal.gui;

import com.monopolydeal.network.GameStateParser;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.TransferHandler;
import javax.swing.TransferHandler.TransferSupport;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * LAN Battle Game hand card panel
 */
public class NetworkPlayerPanel extends JPanel {

    private static final int CARD_W      = 118;
    private static final int CARD_H      = 178;
    private static final int DROP_ZONE_W = 170;
    private static final int DROP_ZONE_H = 100;
    private static final int RIGHT_DOCK_H = DROP_ZONE_H * 3 + 24;  // 324px

    private final NetworkGameFrame frame;

    private final JLabel lblSeat;
    private final JLabel lblActions;
    private final JPanel handCanvas;
    private final ButtonDropZone bankDropZone;
    private final ButtonDropZone propertyDropZone;
    private final ImageActionButton endTurnBtn;

    private IntConsumer bankDropHandler;
    private IntConsumer propertyDropHandler;
    private Runnable    endTurnHandler;

    private boolean myTurn      = false;
    private boolean discardMode = false;
    private boolean gameOver    = false;

    public NetworkPlayerPanel(NetworkGameFrame frame) {
        this.frame = frame;
        setOpaque(false);
        setLayout(new BorderLayout(8, 0));
        setPreferredSize(new Dimension(0, RIGHT_DOCK_H + 40));

        JPanel leftInfo = new JPanel();
        leftInfo.setOpaque(false);
        leftInfo.setLayout(new BoxLayout(leftInfo, BoxLayout.Y_AXIS));
        leftInfo.setPreferredSize(new Dimension(140, RIGHT_DOCK_H + 40));

        lblSeat = new JLabel("You");
        lblSeat.setFont(UITheme.FONT_SUBTITLE);
        lblSeat.setForeground(UITheme.ACCENT_DARK);
        leftInfo.add(lblSeat);

        lblActions = new JLabel("Actions: 0 / 3");
        lblActions.setFont(UITheme.FONT_BODY);
        lblActions.setForeground(UITheme.TEXT_SUB);
        leftInfo.add(lblActions);

        add(leftInfo, BorderLayout.WEST);

        handCanvas = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        handCanvas.setOpaque(false);

        JPanel handWrapper = new JPanel(new BorderLayout());
        handWrapper.setOpaque(false);
        handWrapper.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                "Your Hand"));
        handWrapper.add(handCanvas, BorderLayout.CENTER);
        add(handWrapper, BorderLayout.CENTER);

        JPanel rightDock = new JPanel();
        rightDock.setOpaque(false);
        rightDock.setLayout(new BoxLayout(rightDock, BoxLayout.Y_AXIS));
        Dimension dockSize = new Dimension(DROP_ZONE_W + 8, RIGHT_DOCK_H);
        rightDock.setPreferredSize(dockSize);
        rightDock.setMinimumSize(dockSize);
        rightDock.setMaximumSize(dockSize);
        rightDock.setBorder(BorderFactory.createEmptyBorder(0, 8, 4, 0));

        bankDropZone = new ButtonDropZone("Bank.png", DROP_ZONE_W, DROP_ZONE_H);
        bankDropZone.setImageOffsetY(6);
        bankDropZone.setToolTipText("Drag money or action cards here to bank");
        bankDropZone.setTransferHandler(new BankDropHandler());
        alignDock(bankDropZone);
        rightDock.add(bankDropZone);
        rightDock.add(Box.createVerticalStrut(8));

        propertyDropZone = new ButtonDropZone("Property.png", DROP_ZONE_W, DROP_ZONE_H);
        propertyDropZone.setToolTipText("Drag property cards here to place");
        propertyDropZone.setTransferHandler(new PropertyDropHandler());
        alignDock(propertyDropZone);
        rightDock.add(propertyDropZone);
        rightDock.add(Box.createVerticalStrut(8));

        endTurnBtn = new ImageActionButton("End Turn.png", DROP_ZONE_W, DROP_ZONE_H);
        endTurnBtn.setToolTipText("End your turn");
        endTurnBtn.addActionListener(e -> {
            if (endTurnHandler != null) endTurnHandler.run();
        });
        endTurnBtn.setEnabled(false);
        endTurnBtn.setVisualState(ImageActionButton.VisualState.DISABLED);
        alignDock(endTurnBtn);
        rightDock.add(endTurnBtn);

        add(rightDock, BorderLayout.EAST);
    }

    public void setBankDropHandler(IntConsumer h)     { this.bankDropHandler = h; }
    public void setPropertyDropHandler(IntConsumer h) { this.propertyDropHandler = h; }
    public void setEndTurnHandler(Runnable r)         { this.endTurnHandler = r; }

    public void updateFromSnapshot(GameStateParser.Snapshot snap,
                                   boolean myTurn, boolean discardMode, int discardRemaining) {
        if (snap == null) return;

        this.myTurn      = myTurn;
        this.discardMode = discardMode;
        this.gameOver    = snap.gameOver;

        GameStateParser.PlayerInfo me = snap.getMyInfo(frame.getMyPlayerIndex());
        if (me != null) {
            lblSeat.setText(me.name + " (You)");
            lblActions.setText("Actions: " + me.actions + " / 3");
        }

        if (discardMode && discardRemaining > 0) {
            lblActions.setText("Discard " + discardRemaining + " card(s)!");
            lblActions.setForeground(Color.RED);
        } else {
            lblActions.setForeground(UITheme.TEXT_SUB);
        }

        handCanvas.removeAll();
        List<GameStateParser.CardInfo> hand = snap.myHand;
        if (hand != null) {
            for (GameStateParser.CardInfo card : hand) {
                handCanvas.add(buildHandCard(card));
            }
        }
        handCanvas.revalidate();
        handCanvas.repaint();

        refreshEndTurnButton();
    }

    public void setMyTurn(boolean myTurn, boolean discardMode, int discardRemaining) {
        this.myTurn      = myTurn;
        this.discardMode = discardMode;
        if (discardMode) {
            lblActions.setText("Discard " + discardRemaining + " card(s)!");
            lblActions.setForeground(Color.RED);
        }
        refreshEndTurnButton();
    }

    private void refreshEndTurnButton() {
        boolean canEnd = myTurn && !gameOver;
        endTurnBtn.setEnabled(canEnd);
        if (canEnd && !discardMode) {
            endTurnBtn.setVisualState(ImageActionButton.VisualState.ENABLED);
        } else if (myTurn && discardMode) {
            endTurnBtn.setVisualState(ImageActionButton.VisualState.WAITING);
        } else {
            endTurnBtn.setVisualState(ImageActionButton.VisualState.DISABLED);
        }
    }

    private JPanel buildHandCard(GameStateParser.CardInfo card) {
        JLabel label = new JLabel();
        label.setPreferredSize(new Dimension(CARD_W, CARD_H));
        label.setToolTipText(card.name + "  " + card.value + "M  [" + card.cardType + "]");

        javax.swing.ImageIcon icon =
                frame.getImageResolver().getIconByName(card.name, CARD_W, CARD_H);
        if (icon != null) {
            label.setIcon(icon);
        } else {
            label.setText("<html><center>" + card.name + "<br>" + card.value + "M</center></html>");
            label.setHorizontalAlignment(JLabel.CENTER);
            label.setOpaque(true);
            label.setBackground(UITheme.PANEL_BG);
            label.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        }

        if (myTurn) {
            label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    frame.handleCenterDrop(card.id);
                }
            });
            label.setTransferHandler(new CardDragHandler(card.id));
            label.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    label.getTransferHandler().exportAsDrag(label, e, TransferHandler.COPY);
                }
            });
        }

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(label, BorderLayout.CENTER);
        return wrapper;
    }

    private static class CardDragHandler extends TransferHandler {
        private final int cardId;
        CardDragHandler(int cardId) { this.cardId = cardId; }

        @Override
        public int getSourceActions(javax.swing.JComponent c) { return COPY; }

        @Override
        protected Transferable createTransferable(javax.swing.JComponent c) {
            return new StringSelection(String.valueOf(cardId));
        }
    }

    private class BankDropHandler extends TransferHandler {
        @Override
        public boolean canImport(TransferSupport s) {
            boolean ok = myTurn && !discardMode && !gameOver
                    && s.isDrop() && s.isDataFlavorSupported(DataFlavor.stringFlavor);
            bankDropZone.setHighlight(ok);
            return ok;
        }
        @Override
        public boolean importData(TransferSupport s) {
            if (!canImport(s)) return false;
            try {
                int id = parseCardId(s);
                if (bankDropHandler != null) bankDropHandler.accept(id);
                return true;
            } catch (Exception ignored) { return false; }
            finally { bankDropZone.setHighlight(false); }
        }
    }

    private class PropertyDropHandler extends TransferHandler {
        @Override
        public boolean canImport(TransferSupport s) {
            boolean ok = myTurn && !discardMode && !gameOver
                    && s.isDrop() && s.isDataFlavorSupported(DataFlavor.stringFlavor);
            propertyDropZone.setHighlight(ok);
            return ok;
        }
        @Override
        public boolean importData(TransferSupport s) {
            if (!canImport(s)) return false;
            try {
                int id = parseCardId(s);
                if (propertyDropHandler != null) propertyDropHandler.accept(id);
                return true;
            } catch (Exception ignored) { return false; }
            finally { propertyDropZone.setHighlight(false); }
        }
    }

    private static int parseCardId(TransferSupport s) throws Exception {
        return Integer.parseInt(
                ((String) s.getTransferable().getTransferData(DataFlavor.stringFlavor)).trim());
    }

    private static void alignDock(Component c) {
        if (c instanceof javax.swing.JComponent) {
            ((javax.swing.JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
        }
    }
}