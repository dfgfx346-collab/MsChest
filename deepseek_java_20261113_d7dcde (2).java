package ru.rtp674637.mschest;

public enum Rarity {
    COMMON("common"),
    RARE("rare"),
    EPIC("epic"),
    MYTHIC("mythic");

    private final String configKey;

    Rarity(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }

    public static Rarity fromString(String s) {
        if (s == null) return null;
        for (Rarity r : values()) {
            if (r.configKey.equalsIgnoreCase(s) || r.name().equalsIgnoreCase(s)) return r;
        }
        return null;
    }
}