package com.example.fishinggui.tackle;

import java.util.ArrayList;
import java.util.List;

/**
 * A fishing style a part can be made in, on top of the plain part of its rank. A style trades the part's own number
 * ({@code power}: +0.08 = 8% more of what the rank gives) against its weight ({@code weightDelta}). Pure Java.
 */
public enum Style {
    BEGINNER_SPINNING("beginner_spinning", 1, -0.05, -1),
    SPINNING("spinning", 1, 0.05, 1),
    TELESCOPIC("telescopic", 1, -0.10, -2),
    BASS_SPINNING("bass_spinning", 2, -0.04, -1),
    BASS_BAIT("bass_bait", 2, 0.08, 2),
    EGING("eging", 3, -0.04, -1),
    OFFSHORE_JIGGING("offshore_jigging", 3, 0.08, 2),
    GT_POPPING("gt_popping", 4, 0.04, 1),
    BIG_GAME("big_game", 4, 0.10, 2);

    private final String id;
    private final int rank;
    private final double power;
    private final int weightDelta;

    Style(String id, int rank, double power, int weightDelta) {
        this.id = id;
        this.rank = rank;
        this.power = power;
        this.weightDelta = weightDelta;
    }

    public String id() {
        return id;
    }

    /** The rank (1 = wood) the style belongs to; its parts only exist at that rank. */
    public int rank() {
        return rank;
    }

    public double power() {
        return power;
    }

    public int weightDelta() {
        return weightDelta;
    }

    public static Style byId(String id) {
        for (Style s : values()) {
            if (s.id.equals(id)) {
                return s;
            }
        }
        return null;
    }

    public static List<Style> ofRank(int rank) {
        List<Style> list = new ArrayList<>();
        for (Style s : values()) {
            if (s.rank == rank) {
                list.add(s);
            }
        }
        return list;
    }
}
