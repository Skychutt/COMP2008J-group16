package com.monopolydeal.interfaces;

import com.monopolydeal.model.card.Card;

/**
 * Interface for property cards that support building upgrades (House/Hotel).
 * Only complete property sets should accept upgrades.
 */
public interface IUpgradable {

    /** Attach an upgrade card (e.g., House or Hotel) to this property. */
    void attachUpgrade(Card c);
}
