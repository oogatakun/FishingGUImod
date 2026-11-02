# Difficulty JSON and Failure Penalty (Phase 2-B) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Pre-roll the catch when a fight starts, pick the fish's `FishProfile` from a datapack JSON keyed by the caught item, and cut rod durability by 5 extra when the line snaps.

**Architecture:** A pure-Java `FishProfileParser` validates one JSON object into an `Entry`. `FishProfileLoader` (a `SimpleJsonResourceReloadListener`) holds the item → profile map and is registered on `AddReloadListenerEvent`. `CatchResolver.roll` draws the loot at fight start; `FightManager` stores it in `ActiveFight` and hands it to `CatchResolver.success`. `CatchResolver.failure` applies the line-break penalty.

**Tech Stack:** Minecraft 1.20.1, Forge 47.3.0, Java 17, official mappings, JUnit 5, Gson (bundled with Minecraft).

**Spec:** `docs/superpowers/specs/2026-11-03-phase2b-difficulty-penalty-design.md`

## Global Constraints

- Every Gradle command runs with JDK 17: `export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16"`.
- JSON files live at `data/<namespace>/fishinggui/fish_profiles/<name>.json` (listener directory `"fishinggui/fish_profiles"`).
- JSON fields: `item` (required string, resource location), `stamina` (default 100, must be > 0), `dash_interval` (default 70, must be >= 1, integer), `dash_power` (default 1.0, must be >= 0), `tension_gain` (default 1.0, must be > 0). Defaults are `FishProfile.DEFAULT`'s values. Invalid file → log a warning and skip it.
- Lookup uses the first stack of the pre-rolled loot; no matching entry → `FishProfile.DEFAULT`. Empty loot → `DEFAULT`. Entries are applied in sorted file-id order, so the last one wins on duplicate `item`.
- Final profile = `BaitEffect.apply(bait, jsonProfile)`, computed once at fight start. Luck for the pre-roll = `EnchantmentHelper.getFishingLuckBonus(rod) + player.getLuck() + BaitEffect.luckBonus(bait)`.
- Failure penalty: only when the fight ended by the line snapping (the session itself reached `FAILED`): extra rod damage `LINE_BREAK_ROD_DAMAGE = 5`, applied to the rod still in the fight's hand. Abort (ESC), item swap, hook removal, logout: no extra damage. Bait handling (2-A) is unchanged.
- `FishProfileParser` imports nothing from `net.minecraft` / `net.minecraftforge`.
- Out of scope: GUI changes, bait recipes, max fight time, GameTests.

## File Structure

- Create `src/main/java/com/example/fishinggui/data/FishProfileParser.java`
- Create `src/test/java/com/example/fishinggui/data/FishProfileParserTest.java`
- Create `src/main/java/com/example/fishinggui/data/FishProfileLoader.java`
- Create `src/main/resources/data/fishinggui/fishinggui/fish_profiles/*.json` (bundled entries, Task 2)
- Modify `FishingGuiMod.java`, `hook/CatchResolver.java`, `hook/FishingHookHandler.java`, `fight/FightManager.java`

---

### Task 1: FishProfileParser (pure Java)

**Files:**
- Create: `src/main/java/com/example/fishinggui/data/FishProfileParser.java`
- Test: `src/test/java/com/example/fishinggui/data/FishProfileParserTest.java`

**Interfaces:**
- Consumes: `FishProfile(float stamina, int dashInterval, float dashPower, float tensionGain)`, `FishProfile.DEFAULT`.
- Produces: `record FishProfileParser.Entry(String item, FishProfile profile)`; `static Entry FishProfileParser.parse(com.google.gson.JsonObject)` throwing `IllegalArgumentException` with a human-readable message on invalid input.

- [ ] **Step 1: Write the failing test**

```java
package com.example.fishinggui.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.fishinggui.fight.FishProfile;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class FishProfileParserTest {
    private static JsonObject json(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }

    @Test
    void parsesAllFields() {
        FishProfileParser.Entry e = FishProfileParser.parse(json(
                "{\"item\":\"minecraft:pufferfish\",\"stamina\":60,\"dash_interval\":40,"
                        + "\"dash_power\":1.5,\"tension_gain\":1.2}"));
        assertEquals("minecraft:pufferfish", e.item());
        assertEquals(new FishProfile(60f, 40, 1.5f, 1.2f), e.profile());
    }

    @Test
    void omittedFieldsUseDefaults() {
        FishProfileParser.Entry e = FishProfileParser.parse(json("{\"item\":\"minecraft:cod\",\"stamina\":70}"));
        assertEquals(new FishProfile(70f, FishProfile.DEFAULT.dashInterval(), FishProfile.DEFAULT.dashPower(),
                FishProfile.DEFAULT.tensionGain()), e.profile());
    }

    @Test
    void rejectsMissingItem() {
        assertThrows(IllegalArgumentException.class, () -> FishProfileParser.parse(json("{\"stamina\":70}")));
    }

    @Test
    void rejectsOutOfRangeValues() {
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"stamina\":0}")));
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"dash_interval\":0}")));
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"dash_power\":-1}")));
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"tension_gain\":0}")));
    }

    @Test
    void rejectsWrongTypes() {
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"stamina\":\"lots\"}")));
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"dash_interval\":40.5}")));
        assertThrows(IllegalArgumentException.class, () -> FishProfileParser.parse(json("{\"item\":5}")));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16" && ./gradlew test --tests '*FishProfileParserTest'`
Expected: compilation FAIL (`FishProfileParser` not found). If Gson is not on the test classpath, add `testImplementation 'com.google.code.gson:gson:2.10.1'` to `build.gradle` and report it.

- [ ] **Step 3: Write minimal implementation**

```java
package com.example.fishinggui.data;

import com.example.fishinggui.fight.FishProfile;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/** Validates one fish-profile JSON object. Pure Java apart from Gson so it stays unit-testable. */
public final class FishProfileParser {
    public record Entry(String item, FishProfile profile) {
    }

    private FishProfileParser() {
    }

    public static Entry parse(JsonObject json) {
        String item = string(json, "item");
        FishProfile d = FishProfile.DEFAULT;
        float stamina = number(json, "stamina", d.stamina());
        float dashInterval = number(json, "dash_interval", d.dashInterval());
        float dashPower = number(json, "dash_power", d.dashPower());
        float tensionGain = number(json, "tension_gain", d.tensionGain());
        if (stamina <= 0f) {
            throw new IllegalArgumentException("stamina must be > 0");
        }
        if (dashInterval < 1f || dashInterval != Math.floor(dashInterval)) {
            throw new IllegalArgumentException("dash_interval must be an integer >= 1");
        }
        if (dashPower < 0f) {
            throw new IllegalArgumentException("dash_power must be >= 0");
        }
        if (tensionGain <= 0f) {
            throw new IllegalArgumentException("tension_gain must be > 0");
        }
        return new Entry(item, new FishProfile(stamina, (int) dashInterval, dashPower, tensionGain));
    }

    private static String string(JsonObject json, String key) {
        JsonElement e = json.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("'" + key + "' must be a string");
        }
        return e.getAsString();
    }

    private static float number(JsonObject json, String key, float fallback) {
        JsonElement e = json.get(key);
        if (e == null) {
            return fallback;
        }
        if (!e.isJsonPrimitive()) {
            throw new IllegalArgumentException("'" + key + "' must be a number");
        }
        JsonPrimitive p = e.getAsJsonPrimitive();
        if (!p.isNumber()) {
            throw new IllegalArgumentException("'" + key + "' must be a number");
        }
        return p.getAsFloat();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16" && ./gradlew test`
Expected: PASS (all tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/fishinggui/data src/test/java/com/example/fishinggui/data
git commit -m "feat: add FishProfileParser"
```

---

### Task 2: FishProfileLoader and bundled data

**Files:**
- Create: `src/main/java/com/example/fishinggui/data/FishProfileLoader.java`
- Create: `src/main/resources/data/fishinggui/fishinggui/fish_profiles/<item>.json` (one file per row below)
- Modify: `src/main/java/com/example/fishinggui/FishingGuiMod.java`

**Interfaces:**
- Consumes: `FishProfileParser.parse`.
- Produces: `FishProfileLoader.profileFor(java.util.List<ItemStack> loot) -> FishProfile` (DEFAULT when empty or unmatched).

- [ ] **Step 1: Write the loader**

```java
package com.example.fishinggui.data;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.fight.FishProfile;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

/** Loads per-item fish profiles from datapacks (data/&lt;ns&gt;/fishinggui/fish_profiles). Server side only. */
@Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID)
public final class FishProfileLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    private static volatile Map<ResourceLocation, FishProfile> profiles = Map.of();

    private FishProfileLoader() {
        super(GSON, "fishinggui/fish_profiles");
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new FishProfileLoader());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, FishProfile> loaded = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> file : new TreeMap<>(files).entrySet()) {
            try {
                FishProfileParser.Entry entry = FishProfileParser.parse(file.getValue().getAsJsonObject());
                ResourceLocation item = ResourceLocation.tryParse(entry.item());
                if (item == null) {
                    throw new IllegalArgumentException("invalid item id '" + entry.item() + "'");
                }
                loaded.put(item, entry.profile());
            } catch (RuntimeException e) {
                LOGGER.warn("Skipping fish profile {}: {}", file.getKey(), e.getMessage());
            }
        }
        profiles = Map.copyOf(loaded);
        LOGGER.info("Loaded {} fish profiles", loaded.size());
    }

    public static FishProfile profileFor(List<ItemStack> loot) {
        if (loot.isEmpty()) {
            return FishProfile.DEFAULT;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(loot.get(0).getItem());
        return profiles.getOrDefault(id, FishProfile.DEFAULT);
    }
}
```

`@Mod.EventBusSubscriber` registers only static `@SubscribeEvent` methods on the Forge bus, so no change to `FishingGuiMod` is needed (the `FishingGuiMod.java` entry in Files is not modified). If `AddReloadListenerEvent` is not delivered because of the private constructor or the class being subscribed, adjust minimally and report it.

- [ ] **Step 2: Bundled data files**

Create one file per row at `src/main/resources/data/fishinggui/fishinggui/fish_profiles/<name>.json`, with body `{ "item": "minecraft:<name>", "stamina": S, "dash_interval": I, "dash_power": P, "tension_gain": G }` using these values (S / I / P / G):

| name | S | I | P | G |
|---|---|---|---|---|
| cod | 70 | 90 | 0.8 | 1.0 |
| tropical_fish | 60 | 50 | 1.2 | 1.0 |
| pufferfish | 80 | 45 | 1.4 | 1.0 |
| bowl, stick, string, bone, leather, leather_boots, rotten_flesh, tripwire_hook, ink_sac, potion, lily_pad | 50 | 110 | 0.6 | 1.0 |
| fishing_rod | 110 | 65 | 1.1 | 1.0 |
| enchanted_book | 140 | 60 | 1.3 | 1.1 |
| name_tag | 120 | 60 | 1.2 | 1.0 |
| saddle | 130 | 60 | 1.2 | 1.0 |
| nautilus_shell | 130 | 60 | 1.2 | 1.0 |
| bow | 120 | 60 | 1.2 | 1.0 |

(`salmon` is intentionally absent: it uses the default.) File names are the item path (`ink_sac.json`, `potion.json`, ...).

- [ ] **Step 3: Build**

Run: `export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16" && ./gradlew compileJava test build`
Expected: BUILD SUCCESSFUL; all tests pass.

- [ ] **Step 4: Commit**

```bash
git add src/main
git commit -m "feat: load fish profiles from datapack JSON"
```

---

### Task 3: Pre-rolled loot, profile selection and line-break penalty

**Files:**
- Modify: `src/main/java/com/example/fishinggui/hook/CatchResolver.java`
- Modify: `src/main/java/com/example/fishinggui/hook/FishingHookHandler.java`
- Modify: `src/main/java/com/example/fishinggui/fight/FightManager.java`

**Interfaces:**
- Consumes: `FishProfileLoader.profileFor`, `BaitEffect.apply/luckBonus`, `ModItems.typeOf`.
- Produces:
  - `CatchResolver.roll(ServerPlayer, FishingHook, ItemStack rod, float bonusLuck) -> List<ItemStack>`
  - `CatchResolver.success(ServerPlayer, FishingHook, InteractionHand, List<ItemStack> loot)` (replaces the `bonusLuck` overload)
  - `CatchResolver.failure(ServerPlayer, FishingHook, InteractionHand, boolean lineBroke)`
  - `FightManager.start(ServerPlayer, FishingHook, InteractionHand, FishProfile, BaitType, List<ItemStack> loot)`

- [ ] **Step 1: CatchResolver**
  - Add `public static final int LINE_BREAK_ROD_DAMAGE = 5;`
  - Extract the loot-table drawing from `success` into:

```java
    public static List<ItemStack> roll(ServerPlayer player, FishingHook hook, ItemStack rod, float bonusLuck) {
        ServerLevel level = player.serverLevel();
        float luck = EnchantmentHelper.getFishingLuckBonus(rod) + player.getLuck() + bonusLuck;
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, hook.position())
                .withParameter(LootContextParams.TOOL, rod)
                .withParameter(LootContextParams.THIS_ENTITY, hook)
                .withParameter(LootContextParams.KILLER_ENTITY, player)
                .withLuck(luck)
                .create(LootContextParamSets.FISHING);
        LootTable table = level.getServer().getLootData().getLootTable(BuiltInLootTables.FISHING);
        return table.getRandomItems(params);
    }
```

  - `success(player, hook, hand, loot)` keeps everything after the draw (ItemFishedEvent, advancement, item entities, XP, stats, sound, rod damage, discard) but takes `loot` as a parameter; remove the luck/LootParams code from it (it still reads `rod = player.getItemInHand(hand)` for `FISHING_ROD_HOOKED` and damage).
  - Replace `failure` with:

```java
    public static void failure(ServerPlayer player, FishingHook hook, InteractionHand hand, boolean lineBroke) {
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 1.0F, 0.3F);
        if (lineBroke) {
            ItemStack rod = player.getItemInHand(hand);
            if (rod.getItem() instanceof FishingRodItem) {
                rod.hurtAndBreak(LINE_BREAK_ROD_DAMAGE, player, p -> p.broadcastBreakEvent(hand));
            }
        }
        hook.discard();
    }
```

  Add the `FishingRodItem` import.

- [ ] **Step 2: FishingHookHandler** — replace the bait/start block with:

```java
        BaitType bait = event.getHand() == InteractionHand.MAIN_HAND
                ? ModItems.typeOf(player.getOffhandItem())
                : BaitType.NONE;
        List<ItemStack> loot = CatchResolver.roll(player, hook, event.getItemStack(), BaitEffect.luckBonus(bait));
        FishProfile profile = BaitEffect.apply(bait, FishProfileLoader.profileFor(loot));
        FightManager.start(player, hook, event.getHand(), profile, bait, loot);
```

  Add imports `java.util.List`, `ItemStack`, `FishProfileLoader`.

- [ ] **Step 3: FightManager**
  - `ActiveFight` gains a trailing `List<ItemStack> loot` component; `start(...)` gains a trailing `List<ItemStack> loot` parameter and passes it on.
  - `end(...)` gains a `boolean lineBroke` parameter. Callers: ABORT in `handleInput` → `false`; `stillValid` failure in `onPlayerTick` → `false`; the session-finished branch in `onPlayerTick` → `session.phase() == FightSession.Phase.FAILED` (i.e. `true` when failed).
  - In `end`: success → `CatchResolver.success(player, fight.hook(), fight.hand(), fight.loot())`; else → `CatchResolver.failure(player, fight.hook(), fight.hand(), lineBroke)`. Keep `consumeBait` last. `BaitEffect.luckBonus` is no longer used in `FightManager`; remove that call.

- [ ] **Step 4: Build and test**

Run: `export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16" && ./gradlew compileJava test build`
Expected: BUILD SUCCESSFUL; all tests pass.

- [ ] **Step 5: Dedicated-server smoke test**

Run `./gradlew runServer` (EULA already accepted in `run/eula.txt`); confirm the log shows `Loaded 18 fish profiles` (or the actual number of bundled files) and `Done (`, with no errors mentioning `fishinggui`. Stop the server by killing the java process.

- [ ] **Step 6: Commit**

```bash
git add src/main
git commit -m "feat: pre-roll catch, per-fish profile and line-break rod damage"
```

- [ ] **Step 7: Manual in-game checklist (human, `./gradlew runClient`)**

1. Catching a cod vs a pufferfish vs treasure feels different (stamina and dash rhythm).
2. Line snap at tension 100 reduces the rod durability by 5 extra (check with `/give @s fishing_rod` and the durability bar); ESC abort does not.
3. A successful catch yields exactly the pre-rolled kind of item (loot is no longer re-rolled at the end).
4. Edit a bundled JSON in a datapack (or the dev `run` world's datapacks) and `/reload`: the log prints `Loaded N fish profiles` and the new numbers apply.
5. Bait still works (2-A): off-hand bait drops by 1 at fight end; sturdy paste weakens dashes on top of the fish's own profile.
