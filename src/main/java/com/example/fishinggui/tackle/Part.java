package com.example.fishinggui.tackle;

/** The seven parts of a rod. Each can be fitted at a tackle bench. */
public enum Part {
    ROD_BLANK("rod_blank", true, true),
    REEL("reel", false, true),
    LINE("line", true, true),
    HOOK("hook", true, true),
    FLOAT("float", false, true),
    LEADER("leader", false, true),
    LURE("lure", false, true);

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

    /** Registry name of a part item in a style, e.g. {@code reel_bass_bait_iron}; the plain part when null. */
    public String itemName(int rank, Style style) {
        return style == null ? itemName(rank) : id + "_" + style.id() + "_" + RANKS[rank - 1];
    }
}
