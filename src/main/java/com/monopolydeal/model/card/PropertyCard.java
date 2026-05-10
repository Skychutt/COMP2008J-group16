package com.monopolydeal.model.card;

import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.interfaces.IPlayable;
import com.monopolydeal.interfaces.IUpgradable;
import com.monopolydeal.model.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a property card that belongs to a specific color group.
 * Property cards are placed in the player's property area to form complete sets.
 * Wild property cards (isWild = true) can substitute for different color groups.
 * Implements IPlayable for turn-based play and IUpgradable for House/Hotel attachments.
 */
public class PropertyCard extends Card implements IPlayable, IUpgradable {
    private PropertyType color;     // The color group this property belongs to
    private boolean isWild;         // True if this is a wildcard property
    private List<Card> upgrades;    // Attached upgrades (House/Hotel)

    /**
     * Construct a new PropertyCard.
     * @param name   display name of the property
     * @param value  monetary value when used as payment
     * @param color  the color group of this property
     * @param isWild true if this card is a wildcard that can change color
     */
    public PropertyCard(String name, int value, PropertyType color, boolean isWild) {
        super(name, value);
        this.color = color;
        this.isWild = isWild;
        this.upgrades = new ArrayList<>();
    }

    /** @return the color group of this property */
    public PropertyType getColor() {
        return color;
    }

    /** Set the color group (used when a wild card changes its assigned color). */
    public void setColor(PropertyType color) {
        this.color = color;
    }

    /** @return true if this is a wildcard property */
    public boolean isWild() {
        return isWild;
    }

    /** @return the list of upgrade cards (House/Hotel) attached to this property */
    public List<Card> getUpgrades() {
        return upgrades;
    }

    /**
     * Play this property card by placing it into the player's property area.
     * @param p the player who plays this card
     */
    @Override
    public void executePlay(Player p) {
        p.getPropertyArea().add(this);
    }

    /**
     * Attach an upgrade card (House or Hotel) to this property.
     * @param c the upgrade card to attach
     */
    @Override
    public void attachUpgrade(Card c) {
        upgrades.add(c);
    }

    @Override
    public String toString() {
        return name + " [" + color + "]" + (isWild ? " (Wild)" : "") + " (Value: " + value + "M)";
    }
}
