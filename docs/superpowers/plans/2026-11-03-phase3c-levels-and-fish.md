# Phase 3-C Levels, Baits and 60 Fish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the vanilla fishing loot table with a level-driven catch selector (fish level, bait level, fishing level), make caught fish usable as bait, remove two old baits, and add 56 original fish (with the supplied 32x32 textures) next to the 4 vanilla fish.

**Architecture:** A pure-Java `CatchSelector` picks a pool entry from a target level (bait level), a luck-driven chance of one level up, a halving falloff for lower levels, and a 1% rule for the bait's own species. Fish data (`level`, `catch_weight`, `bait_level`) lives in the existing `fish_profiles` JSON and is exposed by `FishProfileLoader`. The hook handler checks the bait against the player's fishing level and calls the selector. The 56 original fish are generated data (JSON, models, lang, copied textures) registered from one id list.

**Tech Stack:** Minecraft 1.20.1, Forge 47.3.0, Java 17, JUnit 5, Gson.

**Environment:** Build with JDK 17: `export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16"` then `cd /f/mod/FishingGUImod && ./gradlew compileJava test`. No python3: use Edit/Write, or a one-off single-file Java program (`java Gen.java`, JDK 17) in the scratchpad for data generation. Java sources live under `src/main/java/com/example/fishinggui/` (written `…/`), tests under `src/test/java/com/example/fishinggui/`. Commit trailer on every commit: `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`. Spec: `docs/superpowers/specs/2026-11-03-phase3c-levels-and-fish-design.md`; fish table: `docs/superpowers/fish-list.md`.

## Global Constraints

- Fish `level` and `bait_level` are integers 1..20 (`LevelCurve.MAX_LEVEL`); `catch_weight` is an integer >= 1 (default 10). Absent `level` = not catchable (`catchLevel` 0); absent `bait_level` = not a bait (`baitLevel` 0).
- Target level T = bait level when a usable bait is in the offhand, otherwise 1.
- Bait is usable when `bait_level <= player's fishing level`; otherwise it is neither applied nor consumed and the action bar shows `gui.fishinggui.bait.locked` (en "This bait needs Fishing Lv.%s", ja "このエサは釣りLv.%s以上で使えます").
- Selection constants: `BASE_UP = 0.10`, `UP_PER_LUCK = 0.03`, `UP_CAP = 0.5`, `LEVEL_DECAY = 0.5`, `SAME_SPECIES_KEEP = 0.01`, `MAX_REDRAWS = 10`. No bait → the up chance is 0.
- Luck = `EnchantmentHelper.getFishingLuckBonus(rod) + player.getLuck()` (no bait bonus any more).
- The worm is `bait_level 3` with no effect. Glow Bug and Sturdy Paste are removed everywhere (items, `BaitType`/`BaitEffect`, tests, model JSON, lang keys, creative tab).
- Bait is read only when the rod is in the main hand (offhand item); it is consumed at fight end, only if the offhand is still the same item, never in creative (unchanged behaviour).
- Original fish: 56 ids listed below, each with the supplied PNG copied to `assets/fishinggui/textures/item/<id>.png` and a model `assets/fishinggui/models/item/<id>.json` = `{"parent":"minecraft:item/generated","textures":{"layer0":"fishinggui:item/<id>"}}`. Source PNGs: `F:\mc-model-kit-v3.0\mc-model-kit-v3.0\claude-code\mc-model-kit\fish\<id>\out\java-vanilla-resourcepack\assets\<id>\textures\item\<id>.png` (do not modify that folder).
- Generated fish JSON (per fish, level L): `stamina = 80 + 8(L-1)`, `dash_interval = round(75 - 1.5(L-1))`, `dash_power = 0.8 + 0.04(L-1)` (2 decimals), `tension_gain = 0.9 + 0.025(L-1)` (3 decimals), `rarity` = common (L1-5) / uncommon (L6-10) / rare (L11-15) / epic (L16-20), `xp = 10 + 6(L-1)`, `level = bait_level = L`, `catch_weight = 100`, `weight_min`/`weight_max` from the table.
- Lang: every fish and the worm need `item.fishinggui.<id>` in both `en_us.json` and `ja_jp.json`. Keep the 37+12 existing tests passing (BaitEffectTest is deleted with `BaitEffect`).

---

### Task 1: Fish data fields and the pure-Java `CatchSelector`

**Files:**
- Modify: `…/fight/FishInfo.java`, `…/data/FishProfileParser.java`
- Create: `…/fight/CatchSelector.java`
- Test: `src/test/java/com/example/fishinggui/fight/CatchSelectorTest.java`, `src/test/java/com/example/fishinggui/data/FishProfileLevelFieldsTest.java`

**Interfaces:**
- Produces:
  - `FishInfo(Rarity rarity, float weightMin, float weightMax, int xp, int catchLevel, int catchWeight, int baitLevel)`; the old 4-argument constructor stays (catchLevel 0, catchWeight 10, baitLevel 0); `FishInfo.DEFAULT` uses it.
  - `CatchSelector.Entry(String id, int level, int catchWeight)`.
  - `CatchSelector.select(List<Entry> pool, int targetLevel, boolean hasBait, float luck, String baitId, Random random)` → `Entry` (null only when the pool is empty).
  - `CatchSelector.upChance(boolean hasBait, float luck)` → `double` (package-visible helper, tested).

- [ ] **Step 1: Write the failing tests**

`CatchSelectorTest.java`:

```java
package com.example.fishinggui.fight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.fishinggui.fight.CatchSelector.Entry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class CatchSelectorTest {
    private static final int N = 40000;

    private static List<Entry> oneFishPerLevel(int maxLevel) {
        List<Entry> pool = new ArrayList<>();
        for (int l = 1; l <= maxLevel; l++) {
            pool.add(new Entry("fish" + l, l, 10));
        }
        return pool;
    }

    private static Map<Integer, Integer> run(List<Entry> pool, int target, boolean hasBait, float luck,
            String baitId, long seed) {
        Random r = new Random(seed);
        Map<Integer, Integer> counts = new HashMap<>();
        for (int i = 0; i < N; i++) {
            Entry e = CatchSelector.select(pool, target, hasBait, luck, baitId, r);
            counts.merge(e.level(), 1, Integer::sum);
        }
        return counts;
    }

    private static double share(Map<Integer, Integer> counts, int level) {
        return counts.getOrDefault(level, 0) / (double) N;
    }

    @Test
    void emptyPoolReturnsNull() {
        assertNull(CatchSelector.select(List.of(), 1, false, 0f, null, new Random(1)));
    }

    @Test
    void noBaitOnlyGivesLevelOneEvenWithHighLuck() {
        Map<Integer, Integer> c = run(oneFishPerLevel(5), 1, false, 50f, null, 1);
        assertEquals(N, c.getOrDefault(1, 0));
    }

    @Test
    void baitedCatchFollowsUpChanceAndHalvingFalloff() {
        // T=3: 10% one level up (4), otherwise 3:2:1 = 1 : 0.5 : 0.25 of the remaining 90%.
        Map<Integer, Integer> c = run(oneFishPerLevel(5), 3, true, 0f, null, 2);
        assertEquals(0.10, share(c, 4), 0.015);
        assertEquals(0.90 * (1.0 / 1.75), share(c, 3), 0.02);
        assertEquals(0.90 * (0.5 / 1.75), share(c, 2), 0.02);
        assertEquals(0.90 * (0.25 / 1.75), share(c, 1), 0.02);
        assertEquals(0.0, share(c, 5), 0.0);
    }

    @Test
    void luckRaisesUpChanceAndIsCapped() {
        assertEquals(0.10, CatchSelector.upChance(true, 0f), 1e-9);
        assertEquals(0.25, CatchSelector.upChance(true, 5f), 1e-9);
        assertEquals(0.5, CatchSelector.upChance(true, 100f), 1e-9);
        assertEquals(0.10, CatchSelector.upChance(true, -3f), 1e-9);
        assertEquals(0.0, CatchSelector.upChance(false, 100f), 1e-9);
    }

    @Test
    void levelsWithoutEntriesAreSkipped() {
        List<Entry> pool = List.of(new Entry("a", 1, 10), new Entry("b", 5, 10));
        // T=3: nothing at 3 or 2, level 4 has no entry so the up roll is wasted; only level 1 is left.
        Map<Integer, Integer> c = run(pool, 3, true, 0f, null, 3);
        assertEquals(N, c.getOrDefault(1, 0));
    }

    @Test
    void poolEntirelyAboveTargetFallsBackToLowestLevel() {
        List<Entry> pool = List.of(new Entry("x", 5, 10), new Entry("y", 8, 10));
        Map<Integer, Integer> c = run(pool, 1, true, 0f, null, 4);
        assertEquals(N, c.getOrDefault(5, 0));
    }

    @Test
    void levelTwentyNeverRollsAbove() {
        List<Entry> pool = oneFishPerLevel(20);
        Map<Integer, Integer> c = run(pool, 20, true, 100f, null, 5);
        for (int level : c.keySet()) {
            assertTrue(level <= 20);
        }
        assertTrue(share(c, 20) > 0.5);
    }

    @Test
    void catchWeightsWithinALevelAreRespected() {
        List<Entry> pool = List.of(new Entry("light", 1, 10), new Entry("heavy", 1, 30));
        Random r = new Random(6);
        int heavy = 0;
        for (int i = 0; i < N; i++) {
            if (CatchSelector.select(pool, 1, false, 0f, null, r).id().equals("heavy")) {
                heavy++;
            }
        }
        assertEquals(0.75, heavy / (double) N, 0.015);
    }

    @Test
    void baitSpeciesIsAlmostNeverCaught() {
        List<Entry> pool = List.of(new Entry("bait", 1, 10), new Entry("other", 1, 10));
        Random r = new Random(7);
        int same = 0;
        for (int i = 0; i < N; i++) {
            if (CatchSelector.select(pool, 1, true, 0f, "bait", r).id().equals("bait")) {
                same++;
            }
        }
        assertTrue(same / (double) N < 0.02, "bait species share was " + same / (double) N);
    }

    @Test
    void baitSpeciesStillReturnsWhenItIsTheOnlyChoice() {
        List<Entry> pool = List.of(new Entry("bait", 1, 10));
        Entry e = CatchSelector.select(pool, 1, true, 0f, "bait", new Random(8));
        assertNotNull(e);
        assertEquals("bait", e.id());
    }
}
```

`FishProfileLevelFieldsTest.java`:

```java
package com.example.fishinggui.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class FishProfileLevelFieldsTest {
    private static JsonObject json(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }

    @Test
    void defaultsWhenAbsent() {
        FishProfileParser.Entry e = FishProfileParser.parse(json("{\"item\":\"minecraft:cod\"}"));
        assertEquals(0, e.info().catchLevel());
        assertEquals(10, e.info().catchWeight());
        assertEquals(0, e.info().baitLevel());
    }

    @Test
    void parsesAllThree() {
        FishProfileParser.Entry e = FishProfileParser.parse(
                json("{\"item\":\"a:b\",\"level\":7,\"catch_weight\":100,\"bait_level\":4}"));
        assertEquals(7, e.info().catchLevel());
        assertEquals(100, e.info().catchWeight());
        assertEquals(4, e.info().baitLevel());
    }

    @Test
    void baitOnlyEntryIsValid() {
        FishProfileParser.Entry e = FishProfileParser.parse(json("{\"item\":\"fishinggui:worm\",\"bait_level\":3}"));
        assertEquals(3, e.info().baitLevel());
        assertEquals(0, e.info().catchLevel());
    }

    @Test
    void rejectsOutOfRangeAndFractions() {
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"level\":0}")));
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"level\":21}")));
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"bait_level\":0}")));
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"bait_level\":2.5}")));
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"catch_weight\":0}")));
    }
}
```

- [ ] **Step 2: Run to confirm failure**

Run: `./gradlew test --tests '*CatchSelectorTest' --tests '*FishProfileLevelFieldsTest'`
Expected: FAIL (compile errors).

- [ ] **Step 3: Implement `FishInfo`**

```java
public record FishInfo(Rarity rarity, float weightMin, float weightMax, int xp, int catchLevel, int catchWeight,
        int baitLevel) {
    public static final FishInfo DEFAULT = new FishInfo(Rarity.COMMON, 0.1f, 1.0f, 10);

    /** Fish data without catch-pool or bait data (not catchable, not a bait). */
    public FishInfo(Rarity rarity, float weightMin, float weightMax, int xp) {
        this(rarity, weightMin, weightMax, xp, 0, 10, 0);
    }
    // rest of the file unchanged
```

- [ ] **Step 4: Parser.** In `FishProfileParser.parse`, replace the final `return` with:

```java
        int catchLevel = integer(json, "level", 0, 1, LevelCurve.MAX_LEVEL);
        int catchWeight = integer(json, "catch_weight", 10, 1, Integer.MAX_VALUE);
        int baitLevel = integer(json, "bait_level", 0, 1, LevelCurve.MAX_LEVEL);
        return new Entry(item, new FishProfile(stamina, (int) dashInterval, dashPower, tensionGain),
                new FishInfo(rarity, weightMin, weightMax, (int) xp, catchLevel, catchWeight, baitLevel));
```

and add the helper (import `com.example.fishinggui.level.LevelCurve`):

```java
    /** Optional integer in [min, max]; {@code fallback} when absent (the fallback itself is not range-checked). */
    private static int integer(JsonObject json, String key, int fallback, int min, int max) {
        if (!json.has(key)) {
            return fallback;
        }
        float v = number(json, key, fallback);
        if (v != Math.floor(v) || v < min || v > max) {
            throw new IllegalArgumentException("'" + key + "' must be an integer in " + min + ".." + max);
        }
        return (int) v;
    }
```

- [ ] **Step 5: `CatchSelector`**

```java
package com.example.fishinggui.fight;

import com.example.fishinggui.level.LevelCurve;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

/** Picks what a fight will land: target level, a lucky step up, a halving falloff downwards. Pure Java. */
public final class CatchSelector {
    static final double BASE_UP = 0.10;
    static final double UP_PER_LUCK = 0.03;
    static final double UP_CAP = 0.5;
    static final double LEVEL_DECAY = 0.5;
    static final double SAME_SPECIES_KEEP = 0.01;
    static final int MAX_REDRAWS = 10;

    public record Entry(String id, int level, int catchWeight) {
    }

    private CatchSelector() {
    }

    static double upChance(boolean hasBait, float luck) {
        if (!hasBait) {
            return 0.0;
        }
        return Math.max(BASE_UP, Math.min(UP_CAP, BASE_UP + UP_PER_LUCK * luck));
    }

    /** Returns the chosen entry, or null when the pool is empty. */
    public static Entry select(List<Entry> pool, int targetLevel, boolean hasBait, float luck, String baitId,
            Random random) {
        if (pool.isEmpty()) {
            return null;
        }
        Entry picked = null;
        for (int attempt = 0; attempt <= MAX_REDRAWS; attempt++) {
            picked = pickOnce(pool, targetLevel, hasBait, luck, random);
            if (baitId == null || !picked.id().equals(baitId) || random.nextDouble() < SAME_SPECIES_KEEP) {
                return picked;
            }
        }
        return picked;
    }

    private static Entry pickOnce(List<Entry> pool, int target, boolean hasBait, float luck, Random random) {
        int level = chooseLevel(pool, target, hasBait, luck, random);
        List<Entry> atLevel = new ArrayList<>();
        int total = 0;
        for (Entry e : pool) {
            if (e.level() == level) {
                atLevel.add(e);
                total += e.catchWeight();
            }
        }
        int roll = random.nextInt(total);
        for (Entry e : atLevel) {
            roll -= e.catchWeight();
            if (roll < 0) {
                return e;
            }
        }
        return atLevel.get(atLevel.size() - 1);
    }

    private static int chooseLevel(List<Entry> pool, int target, boolean hasBait, float luck, Random random) {
        TreeSet<Integer> levels = new TreeSet<>();
        for (Entry e : pool) {
            levels.add(e.level());
        }
        if (target < LevelCurve.MAX_LEVEL && random.nextDouble() < upChance(hasBait, luck)
                && levels.contains(target + 1)) {
            return target + 1;
        }
        double total = 0.0;
        for (int l : levels) {
            if (l <= target) {
                total += Math.pow(LEVEL_DECAY, target - l);
            }
        }
        if (total == 0.0) {
            return levels.first();
        }
        double roll = random.nextDouble() * total;
        int last = levels.first();
        for (int l : levels) {
            if (l > target) {
                break;
            }
            last = l;
            roll -= Math.pow(LEVEL_DECAY, target - l);
            if (roll < 0) {
                return l;
            }
        }
        return last;
    }
}
```

- [ ] **Step 6: Run all tests**

Run: `./gradlew test`
Expected: PASS (existing tests + 9 + 4 new). If a statistical assertion is flaky for a given seed it is deterministic (fixed seeds); if one fails, check the algorithm first, widen tolerance only with a stated reason.

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "feat: fish level/bait/catch-weight fields and CatchSelector"
```

---

### Task 2: Server integration, bait rework and data for existing items

**Files:**
- Modify: `…/data/FishProfileLoader.java`, `…/hook/CatchResolver.java`, `…/hook/FishingHookHandler.java`, `…/fight/FightManager.java`, `…/item/ModItems.java`, lang files, the 20 bundled JSON files under `src/main/resources/data/fishinggui/fishinggui/fish_profiles/`
- Create: `fish_profiles/salmon.json`, `fish_profiles/worm.json`
- Delete: `…/fight/BaitType.java`, `…/fight/BaitEffect.java`, `src/test/java/com/example/fishinggui/fight/BaitEffectTest.java`, `src/main/resources/assets/fishinggui/models/item/glow_bug.json`, `…/sturdy_paste.json`

**Interfaces:**
- Consumes: Task 1 (`FishInfo` new fields, `CatchSelector`).
- Produces:
  - `FishProfileLoader.pool()` → `List<CatchSelector.Entry>` (entries with `catchLevel > 0`, `id` = registry id string); `FishProfileLoader.baitLevelOf(ItemStack)` → `int` (0 = not a bait).
  - `CatchResolver.roll(ServerPlayer player, ItemStack rod, int baitLevel, Item bait)` → `List<ItemStack>` (baitLevel 0 / bait null = no bait). Empty list only if the pool is empty.
  - `FightManager.start(ServerPlayer, FishingHook, InteractionHand, FishProfile, Item bait /*nullable*/, List<ItemStack> loot, FishInfo, float weight)`.

- [ ] **Step 1: Loader.** In `FishProfileLoader` add a `pool` snapshot built in `apply(...)` next to `infos`:

```java
    private static volatile List<CatchSelector.Entry> pool = List.of();
    ...
        List<CatchSelector.Entry> loadedPool = new ArrayList<>();
        // inside the per-file try, after loadedInfo.put(item, entry.info());
        if (entry.info().catchLevel() > 0) {
            loadedPool.add(new CatchSelector.Entry(item.toString(), entry.info().catchLevel(),
                    entry.info().catchWeight()));
        }
        // after the loop
        pool = List.copyOf(loadedPool);
```

(If two files define the same item, the later one wins in `infos`; make sure `loadedPool` does not keep the earlier entry: remove existing entries with the same id before adding, e.g. `loadedPool.removeIf(e -> e.id().equals(item.toString()))`, also when the later file has no `level`.) Add:

```java
    public static List<CatchSelector.Entry> pool() {
        return pool;
    }

    public static int baitLevelOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        FishInfo info = infos.get(id);
        return info == null ? 0 : info.baitLevel();
    }
```

(imports: `ArrayList`, `com.example.fishinggui.fight.CatchSelector`.) Update the log line to include the pool size.

- [ ] **Step 2: `CatchResolver.roll`** replaces the old method (and its now-unused imports `LootParams`, `LootTable`, `BuiltInLootTables`, `LootContextParamSets`, `LootContextParams`):

```java
    public static List<ItemStack> roll(ServerPlayer player, ItemStack rod, int baitLevel, Item bait) {
        float luck = EnchantmentHelper.getFishingLuckBonus(rod) + player.getLuck();
        boolean hasBait = bait != null && baitLevel > 0;
        String baitId = hasBait ? String.valueOf(ForgeRegistries.ITEMS.getKey(bait)) : null;
        CatchSelector.Entry entry = CatchSelector.select(FishProfileLoader.pool(), hasBait ? baitLevel : 1, hasBait,
                luck, baitId, new Random());
        if (entry == null) {
            return List.of();
        }
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(entry.id()));
        if (item == null || item == Items.AIR) {
            return List.of();
        }
        return List.of(finish(new ItemStack(item), player.getRandom()));
    }

    /** Restores what the vanilla fishing loot table added to a few items (enchants, wear, water bottles). */
    private static ItemStack finish(ItemStack stack, RandomSource random) {
        if (stack.is(Items.ENCHANTED_BOOK)) {
            return EnchantmentHelper.enchantItem(random, new ItemStack(Items.BOOK), 30, true);
        }
        if (stack.is(Items.BOW) || stack.is(Items.FISHING_ROD)) {
            stack.setDamageValue((int) (random.nextFloat() * 0.25f * stack.getMaxDamage()));
            return EnchantmentHelper.enchantItem(random, stack, 30, true);
        }
        if (stack.is(Items.POTION)) {
            return PotionUtils.setPotion(stack, Potions.WATER);
        }
        return stack;
    }
```

(imports: `FishProfileLoader`, `CatchSelector`, `java.util.Random`, `net.minecraft.resources.ResourceLocation`, `net.minecraft.util.RandomSource`, `net.minecraft.world.item.Item`, `Items`, `net.minecraft.world.item.alchemy.PotionUtils`, `Potions`, `net.minecraftforge.registries.ForgeRegistries`; drop the `FishingHook hook` parameter, which is no longer needed.)

- [ ] **Step 3: Handler.** In `FishingHookHandler.onRightClick`, replace the bait/loot/profile block with:

```java
        int fishingLevel = FishingData.get(player).level();
        ItemStack offhand = event.getHand() == InteractionHand.MAIN_HAND ? player.getOffhandItem() : ItemStack.EMPTY;
        int baitLevel = FishProfileLoader.baitLevelOf(offhand);
        Item bait = null;
        if (baitLevel > 0) {
            if (baitLevel <= fishingLevel) {
                bait = offhand.getItem();
            } else {
                player.displayClientMessage(Component.translatable("gui.fishinggui.bait.locked", baitLevel), true);
            }
        }
        List<ItemStack> loot = CatchResolver.roll(player, event.getItemStack(), bait == null ? 0 : baitLevel, bait);
        FishInfo info = FishProfileLoader.infoFor(loot);
        float weight = info.rollWeight(new Random());
        FishProfile profile = info.scaleForWeight(FishProfileLoader.profileFor(loot), weight);
        FightManager.start(player, hook, event.getHand(), profile, bait, loot, info, weight);
```

Fix imports: remove `BaitEffect`, `BaitType`, `ModItems`; add `com.example.fishinggui.level.FishingData`, `net.minecraft.network.chat.Component`, `net.minecraft.world.item.Item`.

- [ ] **Step 4: `FightManager`.** Change `ActiveFight` to hold `Item bait` (nullable) instead of `BaitType bait`; `start(...)` takes `Item bait`; `consumeBait(player, Item bait)`:

```java
    private static void consumeBait(ServerPlayer player, Item bait) {
        if (bait == null || player.getAbilities().instabuild) {
            return;
        }
        ItemStack offhand = player.getOffhandItem();
        if (offhand.is(bait)) {
            offhand.shrink(1);
        }
    }
```

Remove the `ModItems` import (if no longer used) and add `net.minecraft.world.item.Item`.

- [ ] **Step 5: Remove old baits.** Delete `BaitType.java`, `BaitEffect.java`, `BaitEffectTest.java`, the two model JSONs. In `ModItems` remove `GLOW_BUG`, `STURDY_PASTE`, their tab entries and `typeOf` (plus the `BaitType` import). Keep `WORM`. Remove `item.fishinggui.glow_bug` and `item.fishinggui.sturdy_paste` from both lang files. Run `grep -rn "BaitType\|BaitEffect\|GLOW_BUG\|STURDY\|glow_bug\|sturdy" src` and fix anything left.

- [ ] **Step 6: Data for existing items.** Add `"level"`, `"catch_weight"` and (for the four vanilla fish) `"bait_level"` to the existing JSON files (keep every existing field and value):

| item file | level | catch_weight | bait_level |
|---|---|---|---|
| cod | 1 | 100 | 1 |
| tropical_fish | 3 | 100 | 3 |
| pufferfish | 4 | 100 | 4 |
| stick, string, lily_pad, bowl | 1 | 4 | — |
| leather, leather_boots, bone, rotten_flesh | 2 | 4 | — |
| ink_sac, tripwire_hook, potion | 3 | 4 | — |
| fishing_rod | 6 | 10 | — |
| bow | 7 | 10 | — |
| name_tag | 8 | 10 | — |
| nautilus_shell | 10 | 10 | — |
| enchanted_book | 11 | 10 | — |
| saddle | 12 | 10 | — |

Create `salmon.json`: `{"item":"minecraft:salmon","stamina":100,"dash_interval":70,"dash_power":1.0,"tension_gain":1.0,"rarity":"common","weight_min":1.0,"weight_max":5.0,"xp":16,"level":2,"catch_weight":100,"bait_level":2}`.
Create `worm.json`: `{"item":"fishinggui:worm","bait_level":3}`.

- [ ] **Step 7: Lang.** Add to `en_us.json`: `"gui.fishinggui.bait.locked": "This bait needs Fishing Lv.%s"`; to `ja_jp.json`: `"gui.fishinggui.bait.locked": "このエサは釣りLv.%s以上で使えます"`. (Mind trailing commas.) `item.fishinggui.worm` already exists; ja is "ミミズ".

- [ ] **Step 8: Verify and commit**

Run: `./gradlew compileJava test build` — Expected: BUILD SUCCESSFUL, all tests pass.
Dedicated-server smoke test (as in earlier phases): log shows "Loaded 2x fish profiles" with the pool size, "Done (", no fishinggui errors.

```bash
git add -A && git commit -m "feat: level-driven catch selection, worm-only baits, leveled vanilla items"
```

---

### Task 3: 56 original fish (items, data, textures, lang)

**Files:**
- Create (generated): 56 × `fish_profiles/<id>.json`, 56 × `assets/fishinggui/models/item/<id>.json`, 56 × `assets/fishinggui/textures/item/<id>.png` (copied)
- Modify: `…/item/ModItems.java`, `en_us.json`, `ja_jp.json`
- Create: `…/item/FishIds.java`

**Interfaces:**
- Consumes: Task 2 (data format, `ModItems`).
- Produces: `FishIds.ALL` (`List<String>` of the 56 ids, in level order, below); `ModItems.FISH` (`Map<String, RegistryObject<Item>>`, same order).

The data table (id, 日本語, English, level, weight_min, weight_max in kg):

```
haze	ハゼ	Goby	1	0.05	0.2
funa	フナ	Crucian Carp	1	0.1	0.8
wakasagi	ワカサギ	Pond Smelt	2	0.02	0.1
ugui	ウグイ	Japanese Dace	2	0.1	0.5
kisu	キス	Sillago	3	0.05	0.3
aji	アジ	Horse Mackerel	3	0.1	0.6
iwashi	イワシ	Sardine	4	0.05	0.3
oikawa	オイカワ	Pale Chub	4	0.05	0.2
saba	サバ	Mackerel	5	0.3	1.5
mebaru	メバル	Rockfish	5	0.2	0.8
nijimasu	ニジマス	Rainbow Trout	5	0.5	3
kasago	カサゴ	Scorpionfish	6	0.2	1.2
ayu	アユ	Sweetfish	6	0.1	0.4
iwana	イワナ	Char	6	0.3	2
koi	コイ	Koi	7	1	15
karei	カレイ	Flounder	7	0.3	2.5
yamame	ヤマメ	Masu Trout	7	0.2	1
namazu	ナマズ	Catfish	8	1	8
kurodai	クロダイ	Black Sea Bream	8	0.5	4
black_bass	ブラックバス	Black Bass	8	0.5	4
hirame	ヒラメ	Olive Flounder	9	1	8
suzuki	スズキ	Sea Bass	9	1	10
unagi	ウナギ	Eel	9	0.3	2
buri	ブリ	Yellowtail	10	3	12
ainame	アイナメ	Greenling	10	0.5	3
sougyo	ソウギョ	Grass Carp	10	3	30
madai	マダイ	Red Sea Bream	11	1	10
kanpachi	カンパチ	Greater Amberjack	11	3	20
itou	イトウ	Taimen	11	5	40
hiramasa	ヒラマサ	Yellowtail Amberjack	12	5	25
katsuo	カツオ	Skipjack Tuna	12	2	10
chozame	チョウザメ	Sturgeon	12	10	100
shimaaji	シマアジ	Striped Jack	13	2	10
ishidai	イシダイ	Striped Beakfish	13	1	6
arowana	アロワナ	Arowana	13	3	20
kue	クエ	Longtooth Grouper	14	5	40
pirarucu	ピラルク	Pirarucu	14	20	150
onamazu	オオナマズ	Giant Catfish	14	10	80
kihada_maguro	キハダマグロ	Yellowfin Tuna	15	20	150
rounin_aji	ロウニンアジ	Giant Trevally	15	10	80
tiger_fish	タイガーフィッシュ	Tigerfish	15	5	50
kuro_maguro	クロマグロ	Bluefin Tuna	16	100	400
makajiki	マカジキ	Striped Marlin	16	40	150
nile_perch	ナイルパーチ	Nile Perch	16	20	150
bashokajiki	バショウカジキ	Sailfish	17	30	90
alligator_gar	アリゲーターガー	Alligator Gar	17	20	120
akame	アカメ	Japanese Lates	17	10	60
kurokajiki	クロカジキ	Black Marlin	18	100	500
minami_maguro	ミナミマグロ	Southern Bluefin Tuna	18	50	150
beluga	ベルーガ	Beluga Sturgeon	18	200	1000
hohojirozame	ホホジロザメ	Great White Shark	19	300	1500
ryugu_no_tsukai	リュウグウノツカイ	Oarfish	19	50	250
mitsukurizame	ミツクリザメ	Goblin Shark	19	60	200
jinbeizame	ジンベエザメ	Whale Shark	20	1000	15000
coelacanth	シーラカンス	Coelacanth	20	20	90
manbo	マンボウ	Ocean Sunfish	20	200	1500
```

- [ ] **Step 1: Save the table** to the scratchpad as `fish.tsv` (tab separated, exactly as above) and write a one-off single-file Java program `Gen.java` in the scratchpad that, for every row, writes into the repo (UTF-8, LF):
  - `src/main/resources/data/fishinggui/fishinggui/fish_profiles/<id>.json` with the fields from Global Constraints (`"item": "fishinggui:<id>"`; numbers formatted as plain JSON numbers).
  - `src/main/resources/assets/fishinggui/models/item/<id>.json` as specified.
  - copies the source PNG to `src/main/resources/assets/fishinggui/textures/item/<id>.png` (abort with a clear message if a source PNG is missing).
  - prints the `item.fishinggui.<id>` lines for en and ja, and the `FishIds` list, to files in the scratchpad (`lang_en.txt`, `lang_ja.txt`, `ids.txt`) so they can be pasted in.
  Run it with `java Gen.java` using JDK 17 (`$JAVA_HOME/bin/java`).

- [ ] **Step 2: `FishIds`**

```java
package com.example.fishinggui.item;

import java.util.List;

/** Ids of the original fish, in level order. Keep in sync with the fish_profiles JSON and lang files. */
public final class FishIds {
    public static final List<String> ALL = List.of(
            "haze", "funa", "wakasagi", "ugui", "kisu", "aji", "iwashi", "oikawa",
            "saba", "mebaru", "nijimasu", "kasago", "ayu", "iwana", "koi", "karei", "yamame",
            "namazu", "kurodai", "black_bass", "hirame", "suzuki", "unagi", "buri", "ainame", "sougyo",
            "madai", "kanpachi", "itou", "hiramasa", "katsuo", "chozame", "shimaaji", "ishidai", "arowana",
            "kue", "pirarucu", "onamazu", "kihada_maguro", "rounin_aji", "tiger_fish",
            "kuro_maguro", "makajiki", "nile_perch", "bashokajiki", "alligator_gar", "akame",
            "kurokajiki", "minami_maguro", "beluga", "hohojirozame", "ryugu_no_tsukai", "mitsukurizame",
            "jinbeizame", "coelacanth", "manbo");

    private FishIds() {
    }
}
```

(Verify against `fish.tsv`: 56 ids, same order.)

- [ ] **Step 3: `ModItems`.** Register the fish and show them in the tab, after `WORM`:

```java
    public static final Map<String, RegistryObject<Item>> FISH = registerFish();

    private static Map<String, RegistryObject<Item>> registerFish() {
        Map<String, RegistryObject<Item>> map = new LinkedHashMap<>();
        for (String id : FishIds.ALL) {
            map.put(id, ITEMS.register(id, () -> new Item(new Item.Properties())));
        }
        return map;
    }
```

Place the declaration after `ITEMS` and `WORM` (field order matters: `ITEMS` must be initialised first). In the creative tab `displayItems`, after `output.accept(WORM.get());` add `FISH.values().forEach(r -> output.accept(r.get()));`. (Imports: `java.util.LinkedHashMap`, `java.util.Map`.)

- [ ] **Step 4: Lang.** Append the generated `item.fishinggui.<id>` lines to `en_us.json` (English names) and `ja_jp.json` (Japanese names), keeping valid JSON. Add `item.fishinggui.worm` ja = "ミミズ" if not already present.

- [ ] **Step 5: Consistency check** (a one-off check, not a committed test): confirm there are exactly 56 JSON files, 56 models, 56 PNGs, 56 lang lines per language, and that every id in `FishIds.ALL` has all of them; print the counts.

- [ ] **Step 6: Verify and commit**

Run: `./gradlew compileJava test build` — Expected: BUILD SUCCESSFUL.
Dedicated-server smoke test: "Loaded 7x fish profiles" (20 original + salmon + worm + 56 = 78), "Done (", no fishinggui errors and no "Skipping fish profile" warnings.

```bash
git add -A && git commit -m "feat: add 56 original fish with textures, data and lang"
```

---

## Manual in-game checks (user)

1. Creative tab "Fishing" shows the worm and 56 fish with the new images; names are in Japanese/English.
2. Fishing with no bait: only Lv.1 catches (cod, haze, funa) and a few Lv.1 junk items.
3. With the worm at Fishing Lv.3+: mostly Lv.3 fish, some Lv.2 and Lv.1, rarely Lv.4. At Lv.1–2 the action bar says the bait needs Lv.3, the worm is not used up, and you catch Lv.1 fish.
4. A caught fish used as bait (offhand): catches follow that fish's level; the same species almost never comes back.
5. Treasure items (enchanted book, bow, rod) are enchanted/worn like vanilla; potion is a water bottle.
6. `/fishinggui level set @s 10` then fish with a Lv.10 fish as bait: Lv.10 and lower fish, now and then Lv.11.
