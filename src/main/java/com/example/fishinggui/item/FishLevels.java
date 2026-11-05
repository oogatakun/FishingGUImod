package com.example.fishinggui.item;

import java.util.Map;

/**
 * Fish level (= bait level) per item id, for tooltips on the client, which has no access to the datapack.
 * Keep in sync with the fish_profiles JSON; FishLevelsTest checks it.
 */
public final class FishLevels {
    public static final Map<String, Integer> BY_ID = Map.ofEntries(
            Map.entry("minecraft:cod", 1), Map.entry("minecraft:salmon", 2),
            Map.entry("minecraft:tropical_fish", 3), Map.entry("minecraft:pufferfish", 4),
            Map.entry("fishinggui:worm", 1),
            Map.entry("fishinggui:haze", 1), Map.entry("fishinggui:funa", 1),
            Map.entry("fishinggui:wakasagi", 2), Map.entry("fishinggui:ugui", 2),
            Map.entry("fishinggui:kisu", 3), Map.entry("fishinggui:aji", 3),
            Map.entry("fishinggui:iwashi", 4), Map.entry("fishinggui:oikawa", 4),
            Map.entry("fishinggui:saba", 5), Map.entry("fishinggui:mebaru", 5), Map.entry("fishinggui:nijimasu", 5),
            Map.entry("fishinggui:kasago", 6), Map.entry("fishinggui:ayu", 6), Map.entry("fishinggui:iwana", 6),
            Map.entry("fishinggui:koi", 7), Map.entry("fishinggui:karei", 7), Map.entry("fishinggui:yamame", 7),
            Map.entry("fishinggui:namazu", 8), Map.entry("fishinggui:kurodai", 8),
            Map.entry("fishinggui:black_bass", 8),
            Map.entry("fishinggui:hirame", 9), Map.entry("fishinggui:suzuki", 9), Map.entry("fishinggui:unagi", 9),
            Map.entry("fishinggui:buri", 10), Map.entry("fishinggui:ainame", 10), Map.entry("fishinggui:sougyo", 10),
            Map.entry("fishinggui:madai", 11), Map.entry("fishinggui:kanpachi", 11), Map.entry("fishinggui:itou", 11),
            Map.entry("fishinggui:hiramasa", 12), Map.entry("fishinggui:katsuo", 12),
            Map.entry("fishinggui:chozame", 12),
            Map.entry("fishinggui:shimaaji", 13), Map.entry("fishinggui:ishidai", 13),
            Map.entry("fishinggui:arowana", 13),
            Map.entry("fishinggui:kue", 14), Map.entry("fishinggui:pirarucu", 14),
            Map.entry("fishinggui:onamazu", 14),
            Map.entry("fishinggui:kihada_maguro", 15), Map.entry("fishinggui:rounin_aji", 15),
            Map.entry("fishinggui:tiger_fish", 15),
            Map.entry("fishinggui:kuro_maguro", 16), Map.entry("fishinggui:makajiki", 16),
            Map.entry("fishinggui:nile_perch", 16),
            Map.entry("fishinggui:bashokajiki", 17), Map.entry("fishinggui:alligator_gar", 17),
            Map.entry("fishinggui:akame", 17),
            Map.entry("fishinggui:kurokajiki", 18), Map.entry("fishinggui:minami_maguro", 18),
            Map.entry("fishinggui:beluga", 18),
            Map.entry("fishinggui:hohojirozame", 19), Map.entry("fishinggui:ryugu_no_tsukai", 19),
            Map.entry("fishinggui:mitsukurizame", 19),
            Map.entry("fishinggui:jinbeizame", 20), Map.entry("fishinggui:coelacanth", 20),
            Map.entry("fishinggui:manbo", 20));

    private FishLevels() {
    }
}
