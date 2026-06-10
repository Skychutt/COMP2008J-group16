package com.monopolydeal.network;

import com.monopolydeal.model.Deck;
import com.monopolydeal.model.GameManager;
import com.monopolydeal.model.Player;
import com.monopolydeal.model.PropertySet;
import com.monopolydeal.model.card.ActionCard;
import com.monopolydeal.model.card.Card;
import com.monopolydeal.model.card.MoneyCard;
import com.monopolydeal.model.card.PropertyCard;

import java.util.List;

/**
 * Convert the complete game status in GameManager into a JSON string and send it to the client through the network
 */
public class GameStateSnapshot {

    /**
     * Serialize game status
     */
    public static String toJson(GameManager gm, int viewerIndex) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"turn\":").append(gm.getTurn()).append(",");
        sb.append("\"currentPlayer\":\"").append(esc(gm.getCurrentPlayer().getName())).append("\",");
        sb.append("\"deckSize\":").append(Deck.getInstance().drawPileSize()).append(",");
        sb.append("\"discardSize\":").append(Deck.getInstance().discardSize()).append(",");
        Card discardTop = Deck.getInstance().getVisibleDiscardTop();
        if (discardTop != null) {
            sb.append("\"discardTop\":{");
            appendCardBrief(sb, discardTop);
            sb.append("},");
        }
        sb.append("\"gameOver\":").append(gm.isGameOver()).append(",");

        sb.append("\"players\":[");
        List<Player> players = gm.getPlayers();
        for (int i = 0; i < players.size(); i++) {
            if (i > 0) sb.append(",");
            appendPlayer(sb, players.get(i), i, viewerIndex);
        }
        sb.append("]}");
        return sb.toString();
    }

    private static void appendPlayer(StringBuilder sb, Player p, int idx, int viewerIdx) {
        sb.append("{");
        sb.append("\"index\":").append(idx).append(",");
        sb.append("\"name\":\"").append(esc(p.getName())).append("\",");
        sb.append("\"bank\":").append(p.getBankArea().total()).append(",");
        sb.append("\"sets\":").append(p.getPropertyArea().countCompleteSets()).append(",");
        sb.append("\"handSize\":").append(p.getHand().size()).append(",");
        sb.append("\"actions\":").append(p.getActions()).append(",");

        sb.append("\"propertySets\":[");
        List<PropertySet> sets = p.getPropertyArea().getSets();
        for (int i = 0; i < sets.size(); i++) {
            if (i > 0) sb.append(",");
            appendPropertySet(sb, sets.get(i));
        }
        sb.append("],");

        sb.append("\"bankCards\":[");
        List<Card> bankCards = p.getBankArea().getMoney();
        for (int i = 0; i < bankCards.size(); i++) {
            if (i > 0) sb.append(",");
            appendCardBrief(sb, bankCards.get(i));
        }
        sb.append("],");

        sb.append("\"hand\":[");
        if (idx == viewerIdx) {
            List<Card> handCards = p.getHand().getCards();
            for (int i = 0; i < handCards.size(); i++) {
                if (i > 0) sb.append(",");
                appendCardDetail(sb, handCards.get(i));
            }
        }
        sb.append("]}");
    }

    private static void appendPropertySet(StringBuilder sb, PropertySet set) {
        sb.append("{");
        sb.append("\"color\":\"").append(set.getColor().name()).append("\",");
        sb.append("\"complete\":").append(set.isComplete()).append(",");
        sb.append("\"rent\":").append(set.getRent()).append(",");
        sb.append("\"cards\":[");
        List<PropertyCard> cards = set.getCards();
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0) sb.append(",");
            appendCardDetail(sb, cards.get(i));
        }
        sb.append("]}");
    }

    private static void appendCardBrief(StringBuilder sb, Card c) {
        sb.append("{");
        sb.append("\"id\":").append(c.getId()).append(",");
        sb.append("\"name\":\"").append(esc(c.getName())).append("\",");
        sb.append("\"value\":").append(c.getValue());
        sb.append("}");
    }

    private static void appendCardDetail(StringBuilder sb, Card c) {
        sb.append("{");
        sb.append("\"id\":").append(c.getId()).append(",");
        sb.append("\"name\":\"").append(esc(c.getName())).append("\",");
        sb.append("\"value\":").append(c.getValue()).append(",");

        String cardType;
        String extra = "";
        if (c instanceof PropertyCard) {
            cardType = "PROPERTY";
            PropertyCard pc = (PropertyCard) c;
            extra = ",\"color\":\"" + pc.getColor().name() + "\""
                  + ",\"isWild\":" + pc.isWild()
                  + ",\"needsChoice\":" + pc.needsColorChoiceOnPlacement();
        } else if (c instanceof ActionCard) {
            cardType = "ACTION";
            ActionCard ac = (ActionCard) c;
            extra = ",\"actionType\":\"" + ac.getType().name() + "\"";
        } else if (c instanceof MoneyCard) {
            cardType = "MONEY";
        } else {
            cardType = "UNKNOWN";
        }

        sb.append("\"cardType\":\"").append(cardType).append("\"");
        sb.append(extra);
        sb.append("}");
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
