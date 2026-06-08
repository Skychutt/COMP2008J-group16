package com.monopolydeal.network;

import com.monopolydeal.enums.ActionType;
import com.monopolydeal.enums.PropertyType;
import com.monopolydeal.model.GameManager;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.PropertySet;
import com.monopolydeal.model.card.ActionCard;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.MoneyCard;
import com.monopolydeal.model.card.PropertyCard;

/**
 * Rebuilds a lightweight {@link GameManager} from a network snapshot so the client
 * can run the same {@link com.monopolydeal.logic.RuleValidator} as local mode.
 */
public final class ClientGameMirror {

    private ClientGameMirror() {
    }

    public static void applySnapshot(GameManager gm, GameStateParser.Snapshot snap, int myIndex) {
        if (gm == null || snap == null || snap.players == null) {
            return;
        }

        gm.applyMirrorFlags(snap.turn, snap.gameOver);

        for (GameStateParser.PlayerInfo info : snap.players) {
            if (info.index < 0 || info.index >= gm.getPlayers().size()) {
                continue;
            }
            Player player = gm.getPlayers().get(info.index);
            player.setActions(info.actions);

            player.getHand().getCards().clear();
            if (info.index == myIndex && snap.myHand != null) {
                for (GameStateParser.CardInfo cardInfo : snap.myHand) {
                    Card card = toCard(cardInfo);
                    if (card != null) {
                        player.getHand().add(card);
                    }
                }
            }

            player.getBankArea().getMoney().clear();
            if (info.bankCards != null) {
                for (GameStateParser.CardInfo cardInfo : info.bankCards) {
                    Card card = toCard(cardInfo);
                    if (card != null) {
                        player.getBankArea().add(card);
                    }
                }
            }

            player.getPropertyArea().clearAll();
            if (info.propertySets != null) {
                for (GameStateParser.PropertySetInfo setInfo : info.propertySets) {
                    PropertyType color = parseColor(setInfo.color);
                    if (color == null) {
                        continue;
                    }
                    if (setInfo.cards != null) {
                        for (GameStateParser.CardInfo cardInfo : setInfo.cards) {
                            Card card = toCard(cardInfo);
                            if (card instanceof PropertyCard) {
                                player.getPropertyArea().add(card, color);
                            }
                        }
                    }
                }
            }
        }
    }

    private static Card toCard(GameStateParser.CardInfo info) {
        if (info == null) {
            return null;
        }
        if ("PROPERTY".equals(info.cardType)) {
            PropertyType color = parseColor(info.color);
            if (color == null) {
                color = PropertyType.RAINBOW;
            }
            return new PropertyCard(info.name, info.value, color, info.isWild);
        }
        if ("ACTION".equals(info.cardType)) {
            ActionType type = ActionType.GO_PASS;
            if (info.actionType != null) {
                try {
                    type = ActionType.valueOf(info.actionType);
                } catch (IllegalArgumentException ignored) {
                }
            }
            return new ActionCard(info.name, info.value, type, true);
        }
        if ("MONEY".equals(info.cardType)) {
            return new MoneyCard(info.name, info.value, info.value);
        }
        return new MoneyCard(info.name, info.value, info.value);
    }

    private static PropertyType parseColor(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return PropertyType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
