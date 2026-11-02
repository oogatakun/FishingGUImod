# Catch Screen (Phase 3-A) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** After a successful fight, open a catch screen (fish name, rarity badge, weight, XP, Keep / Release). Keep delivers the item; Release does not. The fish's weight is rolled at fight start and makes the tension rise faster.

**Architecture:** Pure-Java `Rarity` + `FishInfo` (rarity, weight range, xp, weight roll, tension scaling) parsed from the existing fish-profile JSON. `FightManager` carries `FishInfo` + weight in `ActiveFight`. On success `CatchResolver.success` does all vanilla bookkeeping immediately but defers spawning the item entities to `PendingCatchManager`, which sends `CatchResultPacket` and applies the player's `CatchChoicePacket`. `CatchScreen` (client) shows the result.

**Tech Stack:** Minecraft 1.20.1, Forge 47.3.0, Java 17, official mappings, JUnit 5, Gson.

**Spec:** `docs/superpowers/specs/2026-11-03-phase3a-catch-screen-design.md`

## Global Constraints

- Every Gradle command runs with JDK 17: `export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16"`.
- `Rarity`, `FishInfo`, `FishProfileParser` import nothing from `net.minecraft` / `net.minecraftforge`.
- Decisions: Keep = item delivered + vanilla XP orb, no fishing XP. Release = no item, no vanilla XP orb, fishing XP "earned" (display only in 3-A; nothing is stored yet). ESC on the screen = Keep. Logout while pending = Keep (items dropped at the hook position). Server stop = discard pending. At most one pending catch per player; a new one first resolves the old as Keep. A `CatchChoicePacket` with no pending catch is ignored.
- Things done at success time, exactly as before: `ItemFishedEvent` (cancelled → rod damage + discard hook only, no screen), `FISHING_ROD_HOOKED` trigger, `FISH_CAUGHT` stat, rod durability, retrieve sound, hook discard. Only item-entity spawning and the vanilla XP orb move to Keep.
- Weight: rolled at fight start in `FishingHookHandler` from the first loot stack's `FishInfo`; heavier fish multiply `tensionGain` by `lerp(0.8, 1.3, t)`, `t = (weight-min)/(max-min)` (0.5 if min == max). Stamina and dash are unaffected. Bait effect (`BaitEffect.apply`) is applied after the weight scaling.
- JSON additions (all optional): `rarity` ("common"|"uncommon"|"rare"|"epic", default common), `weight_min` (>0, default 0.1), `weight_max` (>= weight_min, default 1.0), `xp` (integer >= 0, default 10). Invalid → file skipped with a warning (existing behaviour).
- Client-only classes (`net.minecraft.client.*`) are reached from common code only through `DistExecutor` (as `FightStartPacket` does).
- Strings via lang keys in `ja_jp.json` and `en_us.json`.
- Out of scope: storing/adding fishing XP, levels, price, 3D fish, original fish.

## File Structure

- Create `fight/Rarity.java`, `fight/FishInfo.java`, `src/test/.../fight/FishInfoTest.java`
- Modify `data/FishProfileParser.java` (+ `FishProfileParserTest`), `data/FishProfileLoader.java`, bundled `fish_profiles/*.json`
- Create `hook/PendingCatchManager.java`, `net/CatchResultPacket.java`, `net/CatchChoicePacket.java`, `client/CatchScreen.java`
- Modify `hook/CatchResolver.java`, `hook/FishingHookHandler.java`, `fight/FightManager.java`, `net/ModNetwork.java`, `client/ClientFightHooks.java`, lang files

All paths below are under `src/main/java/com/example/fishinggui/` unless they start with `src/`.

---

### Task 1: Rarity, FishInfo and parser (pure Java)

**Files:**
- Create: `fight/Rarity.java`, `fight/FishInfo.java`
- Modify: `data/FishProfileParser.java`
- Test: `src/test/java/com/example/fishinggui/fight/FishInfoTest.java`, `src/test/java/com/example/fishinggui/data/FishProfileParserTest.java`

**Interfaces:**
- Consumes: `FishProfile(float stamina, int dashInterval, float dashPower, float tensionGain)`.
- Produces:
  - `enum Rarity { COMMON, UNCOMMON, RARE, EPIC }` with `static Rarity fromName(String)` (case-insensitive, throws `IllegalArgumentException`) and `String langKey()` (`"gui.fishinggui.rarity.<lowercase name>"`).
  - `record FishInfo(Rarity rarity, float weightMin, float weightMax, int xp)`; `FishInfo.DEFAULT = new FishInfo(Rarity.COMMON, 0.1f, 1.0f, 10)`; `float rollWeight(java.util.Random)`; `FishProfile scaleForWeight(FishProfile, float weight)`.
  - `FishProfileParser.Entry` becomes `record Entry(String item, FishProfile profile, FishInfo info)`.

- [ ] **Step 1: Write the failing tests**

`FishInfoTest.java`:

```java
package com.example.fishinggui.fight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class FishInfoTest {
    private static final FishInfo INFO = new FishInfo(Rarity.RARE, 1.0f, 3.0f, 40);
    private static final FishProfile BASE = new FishProfile(100f, 70, 1.0f, 1.0f);

    @Test
    void rolledWeightStaysInRangeAndHasOneDecimal() {
        Random r = new Random(7);
        for (int i = 0; i < 500; i++) {
            float w = INFO.rollWeight(r);
            assertTrue(w >= 1.0f && w <= 3.0f, "weight " + w);
            assertEquals(Math.round(w * 10f) / 10f, w, 1e-6f);
        }
    }

    @Test
    void fixedRangeAlwaysRollsThatWeight() {
        FishInfo fixed = new FishInfo(Rarity.COMMON, 0.5f, 0.5f, 1);
        assertEquals(0.5f, fixed.rollWeight(new Random(1)), 1e-6f);
    }

    @Test
    void scaleForWeightMapsRangeToPointEightThroughOnePointThree() {
        assertEquals(0.8f, INFO.scaleForWeight(BASE, 1.0f).tensionGain(), 1e-6f);
        assertEquals(1.3f, INFO.scaleForWeight(BASE, 3.0f).tensionGain(), 1e-6f);
        assertEquals(1.05f, INFO.scaleForWeight(BASE, 2.0f).tensionGain(), 1e-6f);
    }

    @Test
    void scaleForWeightUsesMiddleWhenRangeIsFixed() {
        FishInfo fixed = new FishInfo(Rarity.COMMON, 0.5f, 0.5f, 1);
        assertEquals(1.05f, fixed.scaleForWeight(BASE, 0.5f).tensionGain(), 1e-6f);
    }

    @Test
    void scaleForWeightOnlyTouchesTensionGain() {
        FishProfile p = INFO.scaleForWeight(BASE, 3.0f);
        assertEquals(BASE.stamina(), p.stamina());
        assertEquals(BASE.dashInterval(), p.dashInterval());
        assertEquals(BASE.dashPower(), p.dashPower());
    }

    @Test
    void rarityParsingAndKeys() {
        assertEquals(Rarity.EPIC, Rarity.fromName("Epic"));
        assertEquals("gui.fishinggui.rarity.uncommon", Rarity.UNCOMMON.langKey());
        assertThrows(IllegalArgumentException.class, () -> Rarity.fromName("mythic"));
    }
}
```

Append to `FishProfileParserTest.java` (inside the class):

```java
    @Test
    void parsesFishInfoFields() {
        FishProfileParser.Entry e = FishProfileParser.parse(json(
                "{\"item\":\"a:b\",\"rarity\":\"epic\",\"weight_min\":2,\"weight_max\":5,\"xp\":80}"));
        assertEquals(new com.example.fishinggui.fight.FishInfo(
                com.example.fishinggui.fight.Rarity.EPIC, 2f, 5f, 80), e.info());
    }

    @Test
    void fishInfoDefaultsWhenOmitted() {
        assertEquals(com.example.fishinggui.fight.FishInfo.DEFAULT,
                FishProfileParser.parse(json("{\"item\":\"a:b\"}")).info());
    }

    @Test
    void rejectsInvalidFishInfo() {
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"rarity\":\"mythic\"}")));
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"weight_min\":0}")));
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"weight_min\":2,\"weight_max\":1}")));
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"xp\":-1}")));
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"xp\":1.5}")));
        assertThrows(IllegalArgumentException.class,
                () -> FishProfileParser.parse(json("{\"item\":\"a:b\",\"rarity\":3}")));
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16" && ./gradlew test`
Expected: compilation FAIL (`Rarity` / `FishInfo` not found).

- [ ] **Step 3: Implement**

`fight/Rarity.java`:

```java
package com.example.fishinggui.fight;

import java.util.Locale;

public enum Rarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC;

    public static Rarity fromName(String name) {
        for (Rarity r : values()) {
            if (r.name().equalsIgnoreCase(name)) {
                return r;
            }
        }
        throw new IllegalArgumentException("unknown rarity '" + name + "'");
    }

    public String langKey() {
        return "gui.fishinggui.rarity." + name().toLowerCase(Locale.ROOT);
    }
}
```

`fight/FishInfo.java`:

```java
package com.example.fishinggui.fight;

import java.util.Random;

/** Per-fish catch data: rarity, weight range (kg) and fishing XP. Pure Java. */
public record FishInfo(Rarity rarity, float weightMin, float weightMax, int xp) {
    public static final FishInfo DEFAULT = new FishInfo(Rarity.COMMON, 0.1f, 1.0f, 10);

    static final float LIGHTEST_TENSION_FACTOR = 0.8f;
    static final float HEAVIEST_TENSION_FACTOR = 1.3f;

    /** Uniform weight in the range, rounded to one decimal. */
    public float rollWeight(Random random) {
        float w = weightMin + random.nextFloat() * (weightMax - weightMin);
        return Math.max(weightMin, Math.min(weightMax, Math.round(w * 10f) / 10f));
    }

    /** Heavier fish raise tension faster; stamina and dashes are untouched. */
    public FishProfile scaleForWeight(FishProfile base, float weight) {
        float t = weightMax > weightMin ? (weight - weightMin) / (weightMax - weightMin) : 0.5f;
        t = Math.max(0f, Math.min(1f, t));
        float factor = LIGHTEST_TENSION_FACTOR + (HEAVIEST_TENSION_FACTOR - LIGHTEST_TENSION_FACTOR) * t;
        return new FishProfile(base.stamina(), base.dashInterval(), base.dashPower(), base.tensionGain() * factor);
    }
}
```

`data/FishProfileParser.java`: change `Entry` to `record Entry(String item, FishProfile profile, FishInfo info)`. In `parse`, after the existing validations add:

```java
        FishInfo di = FishInfo.DEFAULT;
        Rarity rarity = di.rarity();
        if (json.has("rarity")) {
            rarity = Rarity.fromName(string(json, "rarity"));
        }
        float weightMin = number(json, "weight_min", di.weightMin());
        float weightMax = number(json, "weight_max", di.weightMax());
        float xp = number(json, "xp", di.xp());
        if (weightMin <= 0f) {
            throw new IllegalArgumentException("weight_min must be > 0");
        }
        if (weightMax < weightMin) {
            throw new IllegalArgumentException("weight_max must be >= weight_min");
        }
        if (xp < 0f || xp != Math.floor(xp)) {
            throw new IllegalArgumentException("xp must be an integer >= 0");
        }
```

and return `new Entry(item, new FishProfile(...), new FishInfo(rarity, weightMin, weightMax, (int) xp))`. Add imports `FishInfo`, `Rarity`. (`string(json, "rarity")` already rejects a non-string such as `3`.)

- [ ] **Step 4: Run tests to verify they pass**

Run: `export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16" && ./gradlew test`
Expected: PASS (all tests).

- [ ] **Step 5: Commit**

```bash
git add src
git commit -m "feat: add Rarity, FishInfo and parse catch data from fish profiles"
```

---

### Task 2: Loader lookup, bundled data and lang keys

**Files:**
- Modify: `data/FishProfileLoader.java`
- Modify: `src/main/resources/data/fishinggui/fishinggui/fish_profiles/*.json`
- Modify: `src/main/resources/assets/fishinggui/lang/en_us.json`, `ja_jp.json`

**Interfaces:**
- Produces: `FishProfileLoader.infoFor(java.util.List<ItemStack> loot) -> FishInfo` (`FishInfo.DEFAULT` when empty/unmatched).

- [ ] **Step 1: Loader** — keep a second `volatile Map<ResourceLocation, FishInfo> infos`; in `apply`, build both maps from each `Entry` (`loaded.put(item, entry.profile()); loadedInfo.put(item, entry.info());`), assign both with `Map.copyOf`, and add:

```java
    public static FishInfo infoFor(List<ItemStack> loot) {
        if (loot.isEmpty()) {
            return FishInfo.DEFAULT;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(loot.get(0).getItem());
        return infos.getOrDefault(id, FishInfo.DEFAULT);
    }
```

- [ ] **Step 2: Bundled data** — add `rarity`, `weight_min`, `weight_max`, `xp` to every existing file (keep the existing fight fields unchanged):

| file | rarity | weight_min | weight_max | xp |
|---|---|---|---|---|
| cod | common | 0.3 | 2.0 | 15 |
| tropical_fish | uncommon | 0.05 | 0.3 | 25 |
| pufferfish | uncommon | 0.2 | 1.5 | 30 |
| bowl, stick, string, bone, leather, leather_boots, rotten_flesh, tripwire_hook, ink_sac, potion, lily_pad | common | 0.1 | 0.6 | 5 |
| fishing_rod | rare | 0.3 | 0.6 | 40 |
| bow | rare | 0.4 | 0.8 | 40 |
| name_tag | rare | 0.1 | 0.2 | 60 |
| nautilus_shell | rare | 0.5 | 1.5 | 60 |
| enchanted_book | epic | 0.5 | 1.0 | 80 |
| saddle | epic | 3.0 | 6.0 | 70 |

(`salmon` has no file: defaults apply.)

- [ ] **Step 3: Lang keys** — add to both lang files:

`en_us.json`:
```json
  "gui.fishinggui.rarity.common": "Common",
  "gui.fishinggui.rarity.uncommon": "Uncommon",
  "gui.fishinggui.rarity.rare": "Rare",
  "gui.fishinggui.rarity.epic": "Epic",
  "gui.fishinggui.catch.keep": "Keep",
  "gui.fishinggui.catch.release": "Release",
  "gui.fishinggui.catch.weight": "Weight",
  "gui.fishinggui.catch.xp": "XP"
```
`ja_jp.json`:
```json
  "gui.fishinggui.rarity.common": "コモン",
  "gui.fishinggui.rarity.uncommon": "アンコモン",
  "gui.fishinggui.rarity.rare": "レア",
  "gui.fishinggui.rarity.epic": "エピック",
  "gui.fishinggui.catch.keep": "キープする",
  "gui.fishinggui.catch.release": "リリースする",
  "gui.fishinggui.catch.weight": "重量",
  "gui.fishinggui.catch.xp": "経験値"
```
Both files stay valid UTF-8 JSON without BOM.

- [ ] **Step 4: Build**

Run: `export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16" && ./gradlew compileJava test build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add src
git commit -m "feat: load fish catch info and add catch screen strings"
```

---

### Task 3: Server side — weight at fight start, pending catch, packets

**Files:**
- Create: `hook/PendingCatchManager.java`, `net/CatchResultPacket.java`, `net/CatchChoicePacket.java`
- Modify: `hook/CatchResolver.java`, `hook/FishingHookHandler.java`, `fight/FightManager.java`, `net/ModNetwork.java`, `client/ClientFightHooks.java`

**Interfaces:**
- Consumes: `FishInfo`, `FishProfileLoader.infoFor/profileFor`, `ReelInputPacket` as the pattern for a C→S packet handler (mirror its `handle`).
- Produces:
  - `FightManager.start(ServerPlayer, FishingHook, InteractionHand, FishProfile, BaitType, List<ItemStack> loot, FishInfo info, float weight)`
  - `CatchResolver.success(ServerPlayer, FishingHook, InteractionHand, List<ItemStack> loot, FishInfo info, float weight)`
  - `CatchResolver.deliver(ServerPlayer, Vec3 origin, List<ItemStack> loot, boolean vanillaXp)`
  - `PendingCatchManager.open(ServerPlayer, Vec3 origin, List<ItemStack> loot, FishInfo info, float weight)`, `PendingCatchManager.choose(ServerPlayer, boolean keep)`
  - `CatchResultPacket(ItemStack stack, byte rarity, float weight, int xp)`, `CatchChoicePacket(boolean keep)`
  - `ClientFightHooks.onCatchResult(ItemStack, byte rarity, float weight, int xp)` (implemented in Task 4; add it here as a method that calls `Minecraft.getInstance().setScreen(new CatchScreen(...))` once `CatchScreen` exists — until Task 4 lands, leave `ClientFightHooks.onCatchResult` with an empty body and a `// Task 4` marker so the build passes)

- [ ] **Step 1: CatchResolver**
  - `success(player, hook, hand, loot, info, weight)`: same as today's `success` up to and including `ItemFishedEvent` handling (cancelled → rod damage + `hook.discard()` + return). Then: trigger `FISHING_ROD_HOOKED`; for each stack award `FISH_CAUGHT` when `stack.is(ItemTags.FISHES)`; play the retrieve sound; damage the rod by `event.getRodDamage()`; `Vec3 origin = hook.position(); hook.discard();` and, if `loot` is not empty, `PendingCatchManager.open(player, origin, loot, info, weight)`. **Remove the item-entity and `ExperienceOrb` spawning from `success`.**
  - New `deliver`:

```java
    public static void deliver(ServerPlayer player, Vec3 origin, List<ItemStack> loot, boolean vanillaXp) {
        ServerLevel level = player.serverLevel();
        for (ItemStack stack : loot) {
            ItemEntity item = new ItemEntity(level, origin.x, origin.y, origin.z, stack);
            double dx = player.getX() - origin.x;
            double dy = player.getY() - origin.y;
            double dz = player.getZ() - origin.z;
            item.setDeltaMovement(dx * 0.1, dy * 0.1 + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08,
                    dz * 0.1);
            level.addFreshEntity(item);
            if (vanillaXp) {
                level.addFreshEntity(new ExperienceOrb(level, player.getX(), player.getY() + 0.5,
                        player.getZ() + 0.5, player.getRandom().nextInt(6) + 1));
            }
        }
    }
```

  Add imports `net.minecraft.world.phys.Vec3`. Keep `roll` and `failure` unchanged.

- [ ] **Step 2: PendingCatchManager**

```java
package com.example.fishinggui.hook;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.fight.FishInfo;
import com.example.fishinggui.net.CatchResultPacket;
import com.example.fishinggui.net.ModNetwork;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/** Holds a successful catch until the player picks Keep or Release on the catch screen. */
@Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID)
public final class PendingCatchManager {
    private record Pending(Vec3 origin, List<ItemStack> loot) {
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private PendingCatchManager() {
    }

    public static void open(ServerPlayer player, Vec3 origin, List<ItemStack> loot, FishInfo info, float weight) {
        Pending old = PENDING.put(player.getUUID(), new Pending(origin, loot));
        if (old != null) {
            CatchResolver.deliver(player, old.origin(), old.loot(), true);
        }
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new CatchResultPacket(loot.get(0), (byte) info.rarity().ordinal(), weight, info.xp()));
    }

    /** Keep delivers the items (plus the vanilla XP orb); Release delivers nothing. */
    public static void choose(ServerPlayer player, boolean keep) {
        Pending pending = PENDING.remove(player.getUUID());
        if (pending != null && keep) {
            CatchResolver.deliver(player, pending.origin(), pending.loot(), true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            choose(player, true);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING.clear();
    }
}
```

- [ ] **Step 3: Packets** (registered in `ModNetwork.register()` after the existing three, ids continuing)
  - `CatchResultPacket` (S→C, `PLAY_TO_CLIENT`): `encode` = `buf.writeItem(stack); buf.writeByte(rarity); buf.writeFloat(weight); buf.writeVarInt(xp);` `decode` reads in the same order; `handle` = `DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientFightHooks.onCatchResult(msg.stack, msg.rarity, msg.weight, msg.xp))` (same pattern as `FightStartPacket`).
  - `CatchChoicePacket(boolean keep)` (C→S, `PLAY_TO_SERVER`): `encode` = `writeBoolean`, `decode` = `readBoolean`; `handle` mirrors `ReelInputPacket.handle`: take the sender from the context and, when non-null, call `PendingCatchManager.choose(sender, msg.keep)`.
  - Bump nothing else; `PROTOCOL` stays `"1"`.

- [ ] **Step 4: FishingHookHandler and FightManager**
  - `FishingHookHandler`: after rolling `loot`:

```java
        FishInfo info = FishProfileLoader.infoFor(loot);
        float weight = info.rollWeight(new java.util.Random());
        FishProfile profile = BaitEffect.apply(bait,
                info.scaleForWeight(FishProfileLoader.profileFor(loot), weight));
        FightManager.start(player, hook, event.getHand(), profile, bait, loot, info, weight);
```

    (use a normal import for `Random` and `FishInfo`).
  - `FightManager`: `ActiveFight` gains trailing `FishInfo info, float weight`; `start(...)` gains matching trailing parameters; in `end`, the success call becomes `CatchResolver.success(player, fight.hook(), fight.hand(), fight.loot(), fight.info(), fight.weight())`.

- [ ] **Step 5: Build and test**

Run: `export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16" && ./gradlew compileJava test build`
Expected: BUILD SUCCESSFUL; all tests pass.

- [ ] **Step 6: Commit**

```bash
git add src
git commit -m "feat: roll weight at fight start and hold catches until the player chooses"
```

---

### Task 4: CatchScreen (client)

**Files:**
- Create: `client/CatchScreen.java`
- Modify: `client/ClientFightHooks.java`

**Interfaces:**
- Consumes: `Rarity`, `CatchChoicePacket`, lang keys from Task 2.
- Produces: `ClientFightHooks.onCatchResult(ItemStack stack, byte rarity, float weight, int xp)` opens `CatchScreen`.

- [ ] **Step 1: CatchScreen**

```java
package com.example.fishinggui.client;

import com.example.fishinggui.fight.Rarity;
import com.example.fishinggui.net.CatchChoicePacket;
import com.example.fishinggui.net.ModNetwork;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Shown after a successful fight: name, rarity, weight, XP and Keep / Release. ESC counts as Keep. */
public class CatchScreen extends Screen {
    private static final int[] RARITY_COLORS = {0xFFFFFFFF, 0xFF55FF55, 0xFF55AAFF, 0xFFAA55FF};
    private static final int BTN_W = 100;
    private static final int BTN_H = 20;

    private final ItemStack stack;
    private final Rarity rarity;
    private final float weight;
    private final int xp;
    private boolean chosen;

    public CatchScreen(ItemStack stack, Rarity rarity, float weight, int xp) {
        super(Component.literal("Catch"));
        this.stack = stack;
        this.rarity = rarity;
        this.weight = weight;
        this.xp = xp;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height - 44;
        addRenderableWidget(Button.builder(Component.translatable("gui.fishinggui.catch.keep"), b -> choose(true))
                .bounds(cx - BTN_W - 6, y, BTN_W, BTN_H).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.fishinggui.catch.release"), b -> choose(false))
                .bounds(cx + 6, y, BTN_W, BTN_H).build());
    }

    private void choose(boolean keep) {
        if (chosen) {
            return;
        }
        chosen = true;
        ModNetwork.CHANNEL.sendToServer(new CatchChoicePacket(keep));
        onClose();
    }

    @Override
    public void removed() {
        if (!chosen) {
            chosen = true;
            if (Minecraft.getInstance().getConnection() != null) {
                ModNetwork.CHANNEL.sendToServer(new CatchChoicePacket(true));
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int cx = this.width / 2;
        int rarityColor = RARITY_COLORS[Math.min(rarity.ordinal(), RARITY_COLORS.length - 1)];

        // name (3x)
        g.pose().pushPose();
        g.pose().scale(3f, 3f, 1f);
        g.drawCenteredString(this.font, stack.getHoverName(), cx / 3, (this.height / 8) / 3, 0xFFFFFF);
        g.pose().popPose();

        // rarity badge
        Component badge = Component.translatable(rarity.langKey());
        int bw = this.font.width(badge) + 24;
        int bx = cx - bw / 2;
        int by = this.height / 8 + 34;
        g.fill(bx, by, bx + bw, by + 16, 0x99000000);
        g.renderOutline(bx, by, bw, 16, rarityColor);
        g.drawCenteredString(this.font, badge, cx, by + 4, rarityColor);

        // item (8x)
        g.pose().pushPose();
        g.pose().translate(cx - 64, this.height / 2 - 56, 0);
        g.pose().scale(8f, 8f, 1f);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();

        // stats box
        int boxW = 190;
        int boxY = this.height - 90;
        int boxX = cx - boxW / 2;
        g.fill(boxX, boxY, boxX + boxW, boxY + 36, 0x99000000);
        g.renderOutline(boxX, boxY, boxW, 36, 0xFFFFFFFF);
        drawStat(g, cx - boxW / 4, boxY + 6, Component.translatable("gui.fishinggui.catch.weight"),
                String.format(Locale.ROOT, "%.1f kg", weight));
        drawStat(g, cx + boxW / 4, boxY + 6, Component.translatable("gui.fishinggui.catch.xp"), "+" + xp);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawStat(GuiGraphics g, int x, int y, Component label, String value) {
        g.drawCenteredString(this.font, label, x, y, 0xAAAAAA);
        g.drawCenteredString(this.font, Component.literal(value), x, y + 14, 0xFFFFFF);
    }
}
```

If `GuiGraphics.renderOutline` or another call does not exist under these mappings, replace it with four 1-px `fill` calls and report it.

- [ ] **Step 2: ClientFightHooks** — implement:

```java
    public static void onCatchResult(ItemStack stack, byte rarity, float weight, int xp) {
        Rarity[] all = Rarity.values();
        Rarity r = all[Math.max(0, Math.min(all.length - 1, rarity))];
        Minecraft.getInstance().setScreen(new CatchScreen(stack, r, weight, xp));
    }
```

  with imports `Rarity`, `ItemStack`. (`FightScreen` closes itself on the SUCCESS state packet, which the server sends before the result packet, so the catch screen replaces a closed screen.)

- [ ] **Step 3: Build and test**

Run: `export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16" && ./gradlew compileJava test build`
Expected: BUILD SUCCESSFUL; all tests pass.

- [ ] **Step 4: Dedicated-server smoke test**

Run `./gradlew runServer` (EULA already accepted in `run/eula.txt`); confirm `Loaded 20 fish profiles` and `Done (` with no errors mentioning `fishinggui` (in particular no client-class `NoClassDefFoundError` from the new packets). Stop it by killing only the server's java process.

- [ ] **Step 5: Commit**

```bash
git add src
git commit -m "feat: add catch screen with keep and release"
```

- [ ] **Step 6: Manual in-game checklist (human, `./gradlew runClient`)**

1. A successful fight opens the catch screen: big name, rarity badge, large item, weight and XP box, two buttons; JP/EN text correct.
2. Keep → the item flies to you (plus a vanilla XP orb). Release → no item, no orb. ESC → same as Keep.
3. A heavier fish (high weight within its range) raises tension noticeably faster than a light one of the same species.
4. Quit to the title while the catch screen is open → rejoin: the item lies at the hook position (Keep behaviour).
5. Rod durability, `FISH_CAUGHT` stat and the retrieve sound still happen at success.
