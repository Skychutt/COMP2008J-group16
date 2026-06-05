package com.monopolydeal.gui;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Game rules dialog: official quick-start rule cards plus English rules summary.
 */
public final class GameRulesDialog {

    private static final String RULES_LIBRARY = "Card_Library/RulesCard/";

    private static final String[] RULE_CARD_FILES = {
            "quick-start-#1-rules-cards.jpg",
            "quick-start-#2-rules-cards.jpg"
    };

    private static final String[] RULE_CARD_CAPTIONS = {
            "Rule card1",
            "Rule card2"
    };

    /** Max display size; actual size keeps original aspect ratio (source images are ~1280px wide). */
    private static final int CARD_MAX_DISPLAY_WIDTH = 400;
    private static final int CARD_MAX_DISPLAY_HEIGHT = 620;

    private static final String RULES_TEXT =
            "RULES INTRODUCTION\n\n"
            + "OBJECTIVE\n"
            + "Be the first player to collect 3 complete property sets of different colors.\n\n"
            + "SETUP\n"
            + "Each player starts with 5 cards. The deck has 110 cards.\n\n"
            + "TURN STRUCTURE\n"
            + "1) Draw Phase: Draw 2 cards (draw 5 if your hand is empty at turn start).\n"
            + "2) Play Phase: Play up to 3 cards from your hand.\n"
            + "3) End Phase: Discard down to 7 cards if needed, then pass the turn.\n\n"
            + "PAYMENTS\n"
            + "When paying rent or fees, use cards from your bank or property area only.\n"
            + "Hand cards cannot be used to pay. Overpayment is not refunded (no change).\n\n"
            + "DEFENSE\n"
            + "Just Say No can cancel a hostile action or rent. It does not use an action.\n\n"
            + "BUILDINGS\n"
            + "House and Hotel cards may only be placed on completed property sets.\n"
            + "They increase rent on that set.\n\n"
            + "ACTION CARDS\n"
            + "Action cards can be played for their effect, or banked as money (effect is lost).\n\n"
            + "See the quick-start rule cards above for the official card reference.";

    private GameRulesDialog() {
    }

    public static void show(JFrame owner) {
        JDialog dialog = new JDialog(owner, "Game Rules", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(UITheme.PANEL_BG);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        root.add(buildRuleCardsPanel(), BorderLayout.NORTH);
        root.add(buildRulesTextPanel(), BorderLayout.CENTER);
        root.add(buildCloseBar(dialog), BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(720, 640));
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private static JPanel buildRuleCardsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        JLabel heading = new JLabel("Official Quick-Start Rule Cards");
        heading.setFont(UITheme.FONT_SUBTITLE);
        heading.setForeground(Color.BLACK);
        heading.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        panel.add(heading);
        panel.add(Box.createVerticalStrut(10));

        JPanel cardsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 8));
        cardsRow.setOpaque(false);

        for (int i = 0; i < RULE_CARD_FILES.length; i++) {
            cardsRow.add(buildRuleCardTile(RULE_CARD_FILES[i], RULE_CARD_CAPTIONS[i]));
        }

        panel.add(cardsRow);
        return panel;
    }

    private static JPanel buildRuleCardTile(String fileName, String caption) {
        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setOpaque(true);
        tile.setBackground(UITheme.PANEL_SOFT_BG);
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        Image image = loadRulesCardImage(fileName);
        JLabel imageLabel;
        if (image != null) {
            BufferedImage scaled = ImageScaleUtil.scaleToFit(
                    image, CARD_MAX_DISPLAY_WIDTH, CARD_MAX_DISPLAY_HEIGHT);
            imageLabel = new JLabel(new javax.swing.ImageIcon(scaled));
            imageLabel.setPreferredSize(new Dimension(scaled.getWidth(), scaled.getHeight()));
        } else {
            imageLabel = new JLabel("<html><center>Image not found:<br/>" + fileName + "</center></html>");
            imageLabel.setPreferredSize(new Dimension(CARD_MAX_DISPLAY_WIDTH, CARD_MAX_DISPLAY_HEIGHT));
        }
        imageLabel.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        tile.add(imageLabel);

        JLabel captionLabel = new JLabel(caption);
        captionLabel.setFont(UITheme.FONT_BODY);
        captionLabel.setForeground(Color.BLACK);
        captionLabel.setAlignmentX(JPanel.CENTER_ALIGNMENT);
        tile.add(Box.createVerticalStrut(6));
        tile.add(captionLabel);

        return tile;
    }

    private static JScrollPane buildRulesTextPanel() {
        JTextArea area = new JTextArea(RULES_TEXT);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(UITheme.FONT_BODY);
        area.setBackground(UITheme.PANEL_SOFT_BG);
        area.setForeground(UITheme.TEXT_MAIN);
        area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UITheme.BORDER, 1, true),
                "Rules Introduction"
        ));
        scroll.setPreferredSize(new Dimension(680, 200));
        return scroll;
    }

    private static JPanel buildCloseBar(JDialog dialog) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bar.setOpaque(false);
        JButton close = new JButton("Close");
        UITheme.styleMenuButton(close);
        close.addActionListener(e -> dialog.dispose());
        bar.add(close);
        return bar;
    }

    private static Image loadRulesCardImage(String fileName) {
        String resourcePath = RULES_LIBRARY + fileName;

        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (in != null) {
                return ImageIO.read(in);
            }
        } catch (IOException ignored) {
        }

        Path localPath = Paths.get("src", "main", "resources", "Card_Library", "RulesCard", fileName);
        if (Files.exists(localPath)) {
            try (InputStream in = new FileInputStream(localPath.toFile())) {
                return ImageIO.read(in);
            } catch (IOException ignored) {
            }
        }

        return null;
    }
}
