package com.monopolydeal.gui.board;

import com.monopolydeal.gui.image.CardImageResolver;

import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.logic.GameLogic;
import com.monopolydeal.model.GameManager;
import com.monopolydeal.model.Player;

/**
 * Shared panel API for local and LAN game windows.
 */
public interface GamePanelHost {

    CardImageResolver getImageResolver();

    GameLogic getGameLogic();

    GameManager getGameManager();

    Player getViewPlayer();

    boolean canBankCard(int cardId);

    void bankCardById(int cardId);

    boolean canPlacePropertyInColor(int cardId, PropertyType color);

    void placePropertyByIdToColor(int cardId, PropertyType color);
}
