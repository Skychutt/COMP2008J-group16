package com.monopolydeal.network;

/**
 * Network communication message category
 */
public class NetworkMessage {

    public static final String WELCOME      = "WELCOME";
    public static final String GAME_START   = "GAME_START";
    public static final String GAME_STATE   = "GAME_STATE";
    public static final String YOUR_TURN    = "YOUR_TURN";
    public static final String WAIT         = "WAIT";
    public static final String EVENT        = "EVENT";
    public static final String GAME_OVER    = "GAME_OVER";

    public static final String PLAY_CARD    = "PLAY_CARD";
    public static final String BANK_CARD    = "BANK_CARD";
    public static final String PLACE_PROP   = "PLACE_PROP";
    public static final String END_TURN     = "END_TURN";
    public static final String DISCARD      = "DISCARD";

    private final String type;
    private final String data;
    public NetworkMessage(String type, String data) {
        this.type = (type == null) ? "UNKNOWN" : type;
        this.data = (data == null) ? "" : data;
    }

    public String getType() { return type; }
    public String getData() { return data; }

    public String toJson() {
        return "{\"type\":\"" + escape(type) + "\",\"data\":\"" + escape(data) + "\"}";
    }
    public static NetworkMessage fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return new NetworkMessage("UNKNOWN", "");
        }
        String type = extractValue(json, "type");
        String data = extractValue(json, "data");
        return new NetworkMessage(type, data);
    }

    private static String extractValue(String json, String key) {
        String marker = "\"" + key + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) return "";
        start += marker.length();
        int end = start;
        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == '\\') {
                end += 2;
            } else if (c == '"') {
                break;
            } else {
                end++;
            }
        }
        return unescape(json.substring(start, end));
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String unescape(String s) {
        return s.replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\\", "\\");
    }

    @Override
    public String toString() {
        return toJson();
    }
}
