package com.example.fishinggui.tackle;

/** The seven parts of a rod. Float, leader and lure arrive in the second stage ({@link #available()}). */
public enum Part {
    ROD_BLANK("rod_blank", true, true),
    REEL("reel", false, true),
    LINE("line", true, true),
    HOOK("hook", true, true),
    FLOAT("float", false, false),
    LEADER("leader", false, false),
    LURE("lure", false, false);

    public static final String[] RANKS = {"wood", "iron", "diamond", "netherite"};
    public static final int MAX_RANK = RANKS.length;

    private final String id;
    private final boolean required;
    private final boolean available;

    Part(String id, boolean required, boolean available) {
        this.id = id;
        this.required = required;
        this.available = available;
    }

    public String id() {
        return id;
    }

    /** A rod cannot be built without its required parts. */
    public boolean required() {
        return required;
    }

    public boolean available() {
        return available;
    }

    /** Registry name of a part item, e.g. {@code reel_iron}. */
    public String itemName(int rank) {
        return id + "_" + RANKS[rank - 1];
    }
}
