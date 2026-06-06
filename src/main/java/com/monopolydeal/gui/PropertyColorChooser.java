package com.monopolydeal.gui;

import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.model.WildcardArtOrientation;
import com.monopolydeal.model.card.PropertyCard;

import javafx.scene.control.ChoiceDialog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Prompts the player to choose a color when placing a wildcard property.
 * Uses a JavaFX {@link ChoiceDialog}.
 */
public final class PropertyColorChooser {

    private PropertyColorChooser() {}

    /**
     * @return the chosen {@link PropertyType}, or null if cancelled / not needed.
     */
    public static PropertyType prompt(Object ignoredParent, PropertyCard card) {
        if (card == null) return null;
        if (card.isColorCommitted()) return card.getColor();
        if (!card.needsColorChoiceOnPlacement()) return null;

        List<PropertyType> options = buildOptions(card);
        if (options.isEmpty()) return null;
        if (options.size() == 1) return options.get(0);

        String[] labels = new String[options.size()];
        for (int i = 0; i < options.size(); i++) {
            labels[i] = formatColor(options.get(i));
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(labels[0], labels);
        dialog.setTitle("Choose Color");
        dialog.setHeaderText("Choose a color for: " + card.getName());
        dialog.setContentText("Color:");

        Optional<String> result = dialog.showAndWait();
        if (!result.isPresent()) return null;

        String chosen = result.get();
        for (int i = 0; i < labels.length; i++) {
            if (labels[i].equals(chosen)) return options.get(i);
        }
        return null;
    }

    private static List<PropertyType> buildOptions(PropertyCard card) {
        List<PropertyType> options = new ArrayList<>();
        if (card.isTwoColorWild()) {
            PropertyType top    = WildcardArtOrientation.getTopColorOnArt(card);
            PropertyType bottom = WildcardArtOrientation.getBottomColorOnArt(card);
            if (top != null && bottom != null) {
                options.add(top);
                options.add(bottom);
            } else {
                options.addAll(card.getNameColorOrder());
            }
            if (options.isEmpty()) options.addAll(card.getAssignableColors());
        } else if (card.isFullColorWild()) {
            options.addAll(card.getAssignableColors());
            options.sort(Comparator.comparing(Enum::name));
        } else {
            options.addAll(card.getAssignableColors());
        }
        return options;
    }

    private static String formatColor(PropertyType type) {
        if (type == null) return "Unknown";
        switch (type) {
            case BROWN:      return "Brown";
            case LIGHTBLUE:  return "Light Blue";
            case PURPLE:     return "Pink";
            case ORANGE:     return "Orange";
            case RED:        return "Red";
            case YELLOW:     return "Yellow";
            case GREEN:      return "Green";
            case BLUE:       return "Dark Blue";
            case BLACK:      return "Railroad";
            case LIGHTGREEN: return "Utility";
            default:         return type.name();
        }
    }
}
