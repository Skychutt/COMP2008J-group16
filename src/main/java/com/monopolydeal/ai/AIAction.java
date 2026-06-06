package com.monopolydeal.ai;

import com.monopolydeal.enums.PropertyType;

/**
 * One action chosen by the AI brain for the current turn step.
 */
public class AIAction {
    public enum Type {
        PLAY_CARD,
        DEPOSIT_TO_BANK,
        DISCARD,
        END_TURN
    }

    private final Type type;
    private final int cardId;
    private final PropertyType chosenColor;

    public AIAction(Type type) {
        this(type, -1, null);
    }

    public AIAction(Type type, int cardId) {
        this(type, cardId, null);
    }

    public AIAction(Type type, int cardId, PropertyType chosenColor) {
        this.type = type;
        this.cardId = cardId;
        this.chosenColor = chosenColor;
    }

    public Type getType() {
        return type;
    }

    public int getCardId() {
        return cardId;
    }

    public PropertyType getChosenColor() {
        return chosenColor;
    }
}
