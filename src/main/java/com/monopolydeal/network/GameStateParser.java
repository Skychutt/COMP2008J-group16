package com.monopolydeal.network;

import java.util.ArrayList;
import java.util.List;

/**
 * Game state JSON parser
 */
public class GameStateParser {

    public static class Snapshot {
        public int turn;
        public String currentPlayer;
        public int deckSize;
        public boolean gameOver;

        public List<PlayerInfo> players;
        public List<CardInfo> myHand;

        public PlayerInfo getMyInfo(int myIndex) {
            if (players == null || myIndex < 0 || myIndex >= players.size()) return null;
            return players.get(myIndex);
        }
    }

    /** Public information of individual players */
    public static class PlayerInfo {
        public int index;
        public String name;
        public int bank;
        public int sets;
        public int handSize;
        public int actions;

        public List<PropertySetInfo> propertySets;
        public List<CardInfo> bankCards;
        public List<CardInfo> hand;
    }

    public static class PropertySetInfo {
        public String color;        // PropertyType 枚举名
        public boolean complete;
        public int rent;
        public List<CardInfo> cards;
    }

    public static class CardInfo {
        public int id;
        public String name;
        public int value;
        public String cardType;     // "PROPERTY" | "ACTION" | "MONEY" | "UNKNOWN"
        public String color;
        public boolean isWild;
        public boolean needsChoice;
        public String actionType;
    }

    /**
     * Parse the game status JSON sent by the server
     *
     * @param json        JSON string sent by the server
     * @param myIndex     This player ID
     * @return Parse result, return null upon failure
     */
    public static Snapshot parse(String json, int myIndex) {
        if (json == null || json.isEmpty()) return null;
        try {
            Snapshot snap = new Snapshot();
            snap.turn          = parseInt(json, "turn");
            snap.currentPlayer = parseStr(json, "currentPlayer");
            snap.deckSize      = parseInt(json, "deckSize");
            snap.gameOver      = parseBool(json, "gameOver");

            snap.players = new ArrayList<>();
            String playersArr = extractArray(json, "players");
            if (playersArr != null) {
                for (String playerJson : splitObjects(playersArr)) {
                    PlayerInfo pi = parsePlayer(playerJson);
                    snap.players.add(pi);
                    if (pi.index == myIndex) {
                        snap.myHand = pi.hand;
                    }
                }
            }
            if (snap.myHand == null) snap.myHand = new ArrayList<>();

            return snap;
        } catch (Exception e) {
            System.err.println("[GameStateParser] Parse error: " + e.getMessage());
            return null;
        }
    }

    private static PlayerInfo parsePlayer(String json) {
        PlayerInfo pi = new PlayerInfo();
        pi.index    = parseInt(json, "index");
        pi.name     = parseStr(json, "name");
        pi.bank     = parseInt(json, "bank");
        pi.sets     = parseInt(json, "sets");
        pi.handSize = parseInt(json, "handSize");
        pi.actions  = parseInt(json, "actions");

        pi.propertySets = new ArrayList<>();
        String setsArr = extractArray(json, "propertySets");
        if (setsArr != null) {
            for (String s : splitObjects(setsArr)) {
                pi.propertySets.add(parsePropertySet(s));
            }
        }

        pi.bankCards = new ArrayList<>();
        String bankArr = extractArray(json, "bankCards");
        if (bankArr != null) {
            for (String c : splitObjects(bankArr)) {
                pi.bankCards.add(parseCard(c));
            }
        }

        pi.hand = new ArrayList<>();
        String handArr = extractArray(json, "hand");
        if (handArr != null) {
            for (String c : splitObjects(handArr)) {
                if (!c.trim().isEmpty()) {
                    pi.hand.add(parseCard(c));
                }
            }
        }

        return pi;
    }

    private static PropertySetInfo parsePropertySet(String json) {
        PropertySetInfo psi = new PropertySetInfo();
        psi.color    = parseStr(json, "color");
        psi.complete = parseBool(json, "complete");
        psi.rent     = parseInt(json, "rent");
        psi.cards    = new ArrayList<>();
        String cardsArr = extractArray(json, "cards");
        if (cardsArr != null) {
            for (String c : splitObjects(cardsArr)) {
                psi.cards.add(parseCard(c));
            }
        }
        return psi;
    }

    private static CardInfo parseCard(String json) {
        CardInfo ci = new CardInfo();
        ci.id         = parseInt(json, "id");
        ci.name       = parseStr(json, "name");
        ci.value      = parseInt(json, "value");
        ci.cardType   = parseStr(json, "cardType");
        ci.color      = parseStr(json, "color");
        ci.isWild     = parseBool(json, "isWild");
        ci.needsChoice= parseBool(json, "needsChoice");
        ci.actionType = parseStr(json, "actionType");
        return ci;
    }

    /** Extract string values from "key": "value" */
    static String parseStr(String json, String key) {
        String marker = "\"" + key + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) return "";
        start += marker.length();
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == '\\') { end += 2; }
            else if (c == '"') { break; }
            else { end++; }
        }
        String raw = json.substring(start, Math.min(end, json.length()));
        return raw.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n");
    }

    static int parseInt(String json, String key) {
        String marker = "\"" + key + "\":";
        int start = json.indexOf(marker);
        if (start < 0) return 0;
        start += marker.length();
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start < json.length() && json.charAt(start) == '"') return 0;
        int end = start;
        if (end < json.length() && json.charAt(end) == '-') end++;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        try { return Integer.parseInt(json.substring(start, end)); }
        catch (NumberFormatException e) { return 0; }
    }

    static boolean parseBool(String json, String key) {
        String marker = "\"" + key + "\":";
        int start = json.indexOf(marker);
        if (start < 0) return false;
        start += marker.length();
        return json.substring(start, Math.min(start + 5, json.length())).trim().startsWith("true");
    }

    static String extractArray(String json, String key) {
        String marker = "\"" + key + "\":[";
        int start = json.indexOf(marker);
        if (start < 0) return null;
        start += marker.length() - 1;  // 指向 '['

        int depth = 0;
        int i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '[' || c == '{') depth++;
            else if (c == ']' || c == '}') {
                depth--;
                if (depth == 0) break;
            }
            i++;
        }
        return json.substring(start + 1, i);  // 去掉 '[' 和 ']'
    }

    static List<String> splitObjects(String arrayContent) {
        List<String> result = new ArrayList<>();
        if (arrayContent == null || arrayContent.trim().isEmpty()) return result;

        int depth = 0;
        int start = -1;
        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    result.add(arrayContent.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return result;
    }
}
