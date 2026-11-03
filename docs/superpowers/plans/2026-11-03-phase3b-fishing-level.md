# Phase 3-B Fishing Level Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a per-player fishing level and XP (earned on Release), shown on the catch/fight screens, with a flashy level-up overlay, XP chat notices, admin commands and a bite sound.

**Architecture:** A pure-Java core (`level` package: `FishingProgress`, `LevelCurve`, parser) is unit-tested. The server stores progress in the player's persistent data under `Player.PERSISTED_NBT_TAG`, and a `FishingService` applies changes and syncs them with one S→C `FishingProgressPacket`. The client keeps the latest values in `ClientProgress`, prints the chat notice, and drives a `LevelUpOverlay` registered as a Forge GUI overlay.

**Tech Stack:** Minecraft 1.20.1, Forge 47.3.0, Java 17, JUnit 5, Gson, Brigadier.

**Environment:** Build with JDK 17: `export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16"` then `cd /f/mod/FishingGUImod && ./gradlew compileJava test`. Java sources live under `src/main/java/com/example/fishinggui/` (written `…/` below), tests under `src/test/java/com/example/fishinggui/`. Commit trailer on every commit: `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.

## Global Constraints

- Level range 1..20 (`MAX_LEVEL = 20`). At max level xp is always 0.
- Default xp needed from Lv.n to Lv.n+1 is `50 × n`. The curve array has exactly 19 integers, each >= 1.
- XP is granted only on Release. Keep (including ESC and logout) grants no fishing XP.
- Datapack directory: `data/<ns>/fishinggui/fishing_levels/*.json`; bundled file `src/main/resources/data/fishinggui/fishinggui/fishing_levels/default.json`. JSON shape: `{"xp_to_next": [19 integers]}`. Invalid files are skipped with a warning; among valid files the last in sorted id order wins; none valid → default curve.
- Storage: `player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound("fishinggui")` with int keys `level` and `xp`; absent → Lv.1 / 0.
- Commands (op level 2): `/fishinggui level set|add <players> <int>`, `/fishinggui xp set|add <players> <int>`, `/fishinggui query <player>`. `level set` range 1..20 and resets xp to 0; `level add` may be negative and the result is clamped to 1..20; `xp set` and `xp add` take values >= 0.
- Bite sound: `SoundEvents.EXPERIENCE_ORB_PICKUP`, sent only to that player, when the hook's `nibble` goes from 0 to > 0 and the player is not already fighting.
- Level-up overlay lasts about 3000 ms, centered, shows "LEVEL UP!" and the new level with rays, sparkles and a fanfare sound; chat is not used for level-up.
- Chat XP notice: `釣り経験値 +12（Lv.3 80/150）` (ja) / `Fishing XP +12 (Lv.3 80/150)` (en); at max level `（Lv.20 最大）` / `(Lv.20 MAX)`.
- All user-visible text goes through lang files `assets/fishinggui/lang/en_us.json` and `ja_jp.json`.
- `ModNetwork` message ids are assigned in registration order; append the new packet last.

---

### Task 1: Pure-Java level core

**Files:**
- Create: `…/level/LevelCurve.java`, `…/level/FishingProgress.java`, `…/data/LevelCurveParser.java`
- Create: `src/main/resources/data/fishinggui/fishinggui/fishing_levels/default.json`
- Test: `src/test/java/com/example/fishinggui/level/FishingProgressTest.java`, `src/test/java/com/example/fishinggui/data/LevelCurveParserTest.java`

**Interfaces:**
- Produces:
  - `LevelCurve.DEFAULT`, `LevelCurve.of(int[] xpToNext)` (throws `IllegalArgumentException`), `int xpToNext(int level)` (0 when `level >= MAX_LEVEL`), `LevelCurve.MAX_LEVEL = 20`.
  - `record FishingProgress(int level, int xp)`: `START`, `boolean atMax()`, `FishingProgress addXp(int amount, LevelCurve curve)`, `FishingProgress withLevel(int level)`, `FishingProgress withXp(int xp, LevelCurve curve)`.
  - `LevelCurveParser.parse(JsonObject)` → `LevelCurve` (throws `IllegalArgumentException`).

- [ ] **Step 1: Write the failing tests**

`FishingProgressTest.java`:

```java
package com.example.fishinggui.level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FishingProgressTest {
    private static final LevelCurve C = LevelCurve.DEFAULT;

    @Test
    void defaultCurveIsFiftyTimesLevel() {
        assertEquals(50, C.xpToNext(1));
        assertEquals(450, C.xpToNext(9));
        assertEquals(950, C.xpToNext(19));
        assertEquals(0, C.xpToNext(20));
    }

    @Test
    void addXpBelowThresholdKeepsLevel() {
        FishingProgress p = FishingProgress.START.addXp(30, C);
        assertEquals(new FishingProgress(1, 30), p);
    }

    @Test
    void addXpCarriesOverIntoNextLevel() {
        FishingProgress p = FishingProgress.START.addXp(70, C);
        assertEquals(new FishingProgress(2, 20), p);
    }

    @Test
    void addXpCanRaiseSeveralLevelsAtOnce() {
        // 50 (Lv1) + 100 (Lv2) = 150, remaining 10
        FishingProgress p = FishingProgress.START.addXp(160, C);
        assertEquals(new FishingProgress(3, 10), p);
    }

    @Test
    void maxLevelPinsXpToZero() {
        FishingProgress p = new FishingProgress(19, 0).addXp(100000, C);
        assertEquals(new FishingProgress(20, 0), p);
        assertTrue(p.atMax());
        assertEquals(p, p.addXp(500, C));
    }

    @Test
    void nonPositiveAmountChangesNothing() {
        FishingProgress p = new FishingProgress(4, 12);
        assertEquals(p, p.addXp(0, C));
        assertEquals(p, p.addXp(-5, C));
    }

    @Test
    void withLevelClampsAndResetsXp() {
        assertEquals(new FishingProgress(20, 0), new FishingProgress(2, 7).withLevel(99));
        assertEquals(new FishingProgress(1, 0), new FishingProgress(5, 7).withLevel(-3));
        assertEquals(new FishingProgress(8, 0), new FishingProgress(5, 7).withLevel(8));
    }

    @Test
    void withXpReplacesXpAndCarriesOver() {
        assertEquals(new FishingProgress(3, 25), new FishingProgress(3, 90).withXp(25, C));
        // Lv3 needs 150, so 160 carries into Lv4 with 10
        assertEquals(new FishingProgress(4, 10), new FishingProgress(3, 0).withXp(160, C));
    }
}
```

`LevelCurveParserTest.java`:

```java
package com.example.fishinggui.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.fishinggui.level.LevelCurve;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class LevelCurveParserTest {
    private static JsonObject json(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }

    private static String array(int n, String element) {
        StringBuilder sb = new StringBuilder("{\"xp_to_next\":[");
        for (int i = 0; i < n; i++) {
            sb.append(i == 0 ? "" : ",").append(element);
        }
        return sb.append("]}").toString();
    }

    @Test
    void parsesNineteenValues() {
        LevelCurve c = LevelCurveParser.parse(json(array(19, "7")));
        assertEquals(7, c.xpToNext(1));
        assertEquals(7, c.xpToNext(19));
        assertEquals(0, c.xpToNext(20));
    }

    @Test
    void rejectsWrongLength() {
        assertThrows(IllegalArgumentException.class, () -> LevelCurveParser.parse(json(array(18, "7"))));
        assertThrows(IllegalArgumentException.class, () -> LevelCurveParser.parse(json(array(20, "7"))));
    }

    @Test
    void rejectsZeroNegativeAndFractions() {
        assertThrows(IllegalArgumentException.class, () -> LevelCurveParser.parse(json(array(19, "0"))));
        assertThrows(IllegalArgumentException.class, () -> LevelCurveParser.parse(json(array(19, "-4"))));
        assertThrows(IllegalArgumentException.class, () -> LevelCurveParser.parse(json(array(19, "2.5"))));
    }

    @Test
    void rejectsMissingOrNonArray() {
        assertThrows(IllegalArgumentException.class, () -> LevelCurveParser.parse(json("{}")));
        assertThrows(IllegalArgumentException.class,
                () -> LevelCurveParser.parse(json("{\"xp_to_next\": 5}")));
    }
}
```

- [ ] **Step 2: Run the tests and confirm they fail to compile**

Run: `./gradlew test --tests '*FishingProgressTest' --tests '*LevelCurveParserTest'`
Expected: FAIL (classes not found).

- [ ] **Step 3: Implement**

`…/level/LevelCurve.java`:

```java
package com.example.fishinggui.level;

/** XP needed to go from level n to n+1, for n in 1..MAX_LEVEL-1. */
public final class LevelCurve {
    public static final int MAX_LEVEL = 20;
    public static final LevelCurve DEFAULT = defaultCurve();

    private final int[] xpToNext;

    private LevelCurve(int[] xpToNext) {
        this.xpToNext = xpToNext;
    }

    public static LevelCurve of(int[] values) {
        if (values.length != MAX_LEVEL - 1) {
            throw new IllegalArgumentException("xp_to_next must have " + (MAX_LEVEL - 1) + " entries");
        }
        for (int v : values) {
            if (v < 1) {
                throw new IllegalArgumentException("xp_to_next entries must be >= 1");
            }
        }
        return new LevelCurve(values.clone());
    }

    public int xpToNext(int level) {
        if (level >= MAX_LEVEL) {
            return 0;
        }
        return xpToNext[Math.max(1, level) - 1];
    }

    private static LevelCurve defaultCurve() {
        int[] v = new int[MAX_LEVEL - 1];
        for (int i = 0; i < v.length; i++) {
            v[i] = 50 * (i + 1);
        }
        return new LevelCurve(v);
    }
}
```

`…/level/FishingProgress.java`:

```java
package com.example.fishinggui.level;

/** A player's fishing level and the XP collected towards the next one. */
public record FishingProgress(int level, int xp) {
    public static final FishingProgress START = new FishingProgress(1, 0);

    public FishingProgress {
        level = Math.max(1, Math.min(LevelCurve.MAX_LEVEL, level));
        xp = level >= LevelCurve.MAX_LEVEL ? 0 : Math.max(0, xp);
    }

    public boolean atMax() {
        return level >= LevelCurve.MAX_LEVEL;
    }

    public FishingProgress addXp(int amount, LevelCurve curve) {
        if (amount <= 0 || atMax()) {
            return this;
        }
        int lv = level;
        long total = (long) xp + amount;
        while (lv < LevelCurve.MAX_LEVEL && total >= curve.xpToNext(lv)) {
            total -= curve.xpToNext(lv);
            lv++;
        }
        return new FishingProgress(lv, lv >= LevelCurve.MAX_LEVEL ? 0 : (int) total);
    }

    public FishingProgress withLevel(int newLevel) {
        return new FishingProgress(newLevel, 0);
    }

    public FishingProgress withXp(int newXp, LevelCurve curve) {
        return new FishingProgress(level, 0).addXp(Math.max(0, newXp), curve);
    }
}
```

`…/data/LevelCurveParser.java`:

```java
package com.example.fishinggui.data;

import com.example.fishinggui.level.LevelCurve;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Validates one fishing_levels JSON object. */
public final class LevelCurveParser {
    private LevelCurveParser() {
    }

    public static LevelCurve parse(JsonObject json) {
        JsonElement e = json.get("xp_to_next");
        if (e == null || !e.isJsonArray()) {
            throw new IllegalArgumentException("'xp_to_next' must be an array");
        }
        JsonArray array = e.getAsJsonArray();
        int[] values = new int[array.size()];
        for (int i = 0; i < values.length; i++) {
            JsonElement item = array.get(i);
            if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isNumber()) {
                throw new IllegalArgumentException("xp_to_next[" + i + "] must be a number");
            }
            double d = item.getAsDouble();
            if (d != Math.floor(d) || d > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("xp_to_next[" + i + "] must be an integer");
            }
            values[i] = (int) d;
        }
        return LevelCurve.of(values);
    }
}
```

`default.json`: the 19 values 50, 100, 150, … 950:

```json
{
  "xp_to_next": [50, 100, 150, 200, 250, 300, 350, 400, 450, 500, 550, 600, 650, 700, 750, 800, 850, 900, 950]
}
```

- [ ] **Step 4: Run the tests**

Run: `./gradlew test`
Expected: PASS (existing 37 + new tests).

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat: fishing level core (progress, curve, parser)"
```

---

### Task 2: Server storage, sync, XP on Release, bite sound

**Files:**
- Create: `…/level/FishingData.java`, `…/level/FishingService.java`, `…/data/LevelCurveLoader.java`, `…/net/FishingProgressPacket.java`, `…/client/ClientProgress.java`, `…/hook/BiteSoundHandler.java`
- Modify: `…/net/ModNetwork.java`, `…/hook/PendingCatchManager.java`, `…/fight/FightManager.java`, lang files

**Interfaces:**
- Consumes: Task 1 (`FishingProgress`, `LevelCurve`, `LevelCurveParser`).
- Produces:
  - `LevelCurveLoader.curve()` → current `LevelCurve` (static).
  - `FishingData.get(Player)` → `FishingProgress`; `FishingData.set(Player, FishingProgress)`.
  - `FishingService.award(ServerPlayer, int xp)`; `FishingService.change(ServerPlayer, FishingProgress newProgress, int gainedXp)`; `FishingService.sync(ServerPlayer)` (gained 0, not leveled).
  - `FishingProgressPacket(int level, int xp, int xpToNext, int gained, boolean leveledUp)`.
  - `ClientProgress`: static `level()`, `xp()`, `xpToNext()`, `apply(level, xp, xpToNext, gained, leveledUp)`. Defaults 1 / 0 / 50. `apply` prints the chat notice when `gained > 0` and calls `LevelUpOverlay.start(level)` when `leveledUp` (Task 3 provides `LevelUpOverlay.start(int)`; in this task add a stub `client/LevelUpOverlay.java` with an empty `public static void start(int level) {}` so it compiles).

- [ ] **Step 1: `FishingData`**

```java
package com.example.fishinggui.level;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** Reads and writes fishing progress in the player's persistent data (kept across death by Forge). */
public final class FishingData {
    private static final String KEY = "fishinggui";

    private FishingData() {
    }

    public static FishingProgress get(Player player) {
        CompoundTag root = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag tag = root.getCompound(KEY);
        if (!tag.contains("level")) {
            return FishingProgress.START;
        }
        return new FishingProgress(tag.getInt("level"), tag.getInt("xp"));
    }

    public static void set(Player player, FishingProgress progress) {
        CompoundTag persistent = player.getPersistentData();
        CompoundTag root = persistent.getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag tag = new CompoundTag();
        tag.putInt("level", progress.level());
        tag.putInt("xp", progress.xp());
        root.put(KEY, tag);
        persistent.put(Player.PERSISTED_NBT_TAG, root);
    }
}
```

- [ ] **Step 2: `LevelCurveLoader`** (same pattern as `data/FishProfileLoader.java`)

```java
package com.example.fishinggui.data;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.level.LevelCurve;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/** Loads the XP curve from datapacks (data/&lt;ns&gt;/fishinggui/fishing_levels). Server side only. */
@Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID)
public final class LevelCurveLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    private static volatile LevelCurve curve = LevelCurve.DEFAULT;

    private LevelCurveLoader() {
        super(GSON, "fishinggui/fishing_levels");
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new LevelCurveLoader());
    }

    public static LevelCurve curve() {
        return curve;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        LevelCurve loaded = LevelCurve.DEFAULT;
        for (Map.Entry<ResourceLocation, JsonElement> file : new TreeMap<>(files).entrySet()) {
            try {
                loaded = LevelCurveParser.parse(file.getValue().getAsJsonObject());
            } catch (RuntimeException e) {
                LOGGER.warn("Skipping fishing level file {}: {}", file.getKey(), e.getMessage());
            }
        }
        curve = loaded;
        LOGGER.info("Loaded fishing level curve");
    }
}
```

- [ ] **Step 3: `FishingProgressPacket`** (follow `CatchResultPacket`'s shape)

```java
package com.example.fishinggui.net;

import com.example.fishinggui.client.ClientProgress;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server to client: the player's current fishing progress, plus what just changed. */
public record FishingProgressPacket(int level, int xp, int xpToNext, int gained, boolean leveledUp) {
    public static void encode(FishingProgressPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.level);
        buf.writeVarInt(msg.xp);
        buf.writeVarInt(msg.xpToNext);
        buf.writeVarInt(msg.gained);
        buf.writeBoolean(msg.leveledUp);
    }

    public static FishingProgressPacket decode(FriendlyByteBuf buf) {
        return new FishingProgressPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readBoolean());
    }

    public static void handle(FishingProgressPacket msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientProgress.apply(msg.level, msg.xp, msg.xpToNext,
                msg.gained, msg.leveledUp));
    }
}
```

Register it last in `ModNetwork.register()`:

```java
        CHANNEL.messageBuilder(FishingProgressPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FishingProgressPacket::encode).decoder(FishingProgressPacket::decode)
                .consumerMainThread(FishingProgressPacket::handle).add();
```

- [ ] **Step 4: `FishingService`**

```java
package com.example.fishinggui.level;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.data.LevelCurveLoader;
import com.example.fishinggui.net.FishingProgressPacket;
import com.example.fishinggui.net.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/** Applies fishing progress changes on the server and keeps the client in sync. */
@Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID)
public final class FishingService {
    private FishingService() {
    }

    public static void award(ServerPlayer player, int xp) {
        FishingProgress now = FishingData.get(player);
        change(player, now.addXp(xp, LevelCurveLoader.curve()), xp);
    }

    /** Stores {@code next}; {@code gainedXp} is shown in chat when positive. */
    public static void change(ServerPlayer player, FishingProgress next, int gainedXp) {
        FishingProgress before = FishingData.get(player);
        FishingData.set(player, next);
        send(player, next, Math.max(0, gainedXp), next.level() > before.level());
    }

    public static void sync(ServerPlayer player) {
        send(player, FishingData.get(player), 0, false);
    }

    private static void send(ServerPlayer player, FishingProgress p, int gained, boolean leveledUp) {
        int needed = LevelCurveLoader.curve().xpToNext(p.level());
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new FishingProgressPacket(p.level(), p.xp(), needed, gained, leveledUp));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }
}
```

- [ ] **Step 5: `ClientProgress`** (plus the stub `LevelUpOverlay`)

```java
package com.example.fishinggui.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Latest fishing progress known to the client. */
public final class ClientProgress {
    private static int level = 1;
    private static int xp = 0;
    private static int xpToNext = 50;

    private ClientProgress() {
    }

    public static int level() {
        return level;
    }

    public static int xp() {
        return xp;
    }

    public static int xpToNext() {
        return xpToNext;
    }

    public static void apply(int newLevel, int newXp, int newXpToNext, int gained, boolean leveledUp) {
        level = newLevel;
        xp = newXp;
        xpToNext = newXpToNext;
        var player = Minecraft.getInstance().player;
        if (gained > 0 && player != null) {
            Component msg = xpToNext == 0
                    ? Component.translatable("gui.fishinggui.xp.gained.max", gained, level)
                    : Component.translatable("gui.fishinggui.xp.gained", gained, level, xp, xpToNext);
            player.sendSystemMessage(msg);
        }
        if (leveledUp) {
            LevelUpOverlay.start(level);
        }
    }
}
```

Stub `client/LevelUpOverlay.java` (replaced in Task 3):

```java
package com.example.fishinggui.client;

public final class LevelUpOverlay {
    private LevelUpOverlay() {
    }

    public static void start(int level) {
    }
}
```

- [ ] **Step 6: XP on Release.** In `PendingCatchManager`:
  - Change the record to `private record Pending(Vec3 origin, List<ItemStack> loot, int xp)`.
  - In `open(...)` build it as `new Pending(origin, loot, info.xp())`; the old pending resolved there stays "Keep" (no XP).
  - In `choose(player, keep)`: after removing `pending`, add `else if (pending != null) { FishingService.award(player, pending.xp()); }` so Release awards `pending.xp()` and Keep delivers items. Import `com.example.fishinggui.level.FishingService`.

- [ ] **Step 7: Sync at fight start.** In `FightManager.start(...)`, after sending the `FightStartPacket`, call `FishingService.sync(player);` (import it).

- [ ] **Step 8: Bite sound.** `…/hook/BiteSoundHandler.java`:

```java
package com.example.fishinggui.hook;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.fight.FightManager;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Plays an XP-orb chime to the player when a fish takes the bait (before they click). */
@Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID)
public final class BiteSoundHandler {
    private static final Set<UUID> BITING = new HashSet<>();

    private BiteSoundHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        FishingHook hook = player.fishing;
        boolean biting = hook != null && hook.nibble > 0;
        boolean wasBiting = biting ? !BITING.add(player.getUUID()) : BITING.remove(player.getUUID());
        if (biting && !wasBiting && !FightManager.isFighting(player)) {
            player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.9f, 1.0f);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        BITING.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        BITING.clear();
    }
}
```

(`wasBiting`: `BITING.add` returns false when already present, so `!add` = was already biting; `remove` returns true if it was present. If `playNotifySound` is not available on `ServerPlayer` in 1.20.1, fall back to `player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.EXPERIENCE_ORB_PICKUP), SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), 0.9f, 1.0f, player.level().getRandom().nextLong()))`.)

- [ ] **Step 9: Lang.** Append to `en_us.json`:

```json
  "gui.fishinggui.level": "Fishing Lv.%s",
  "gui.fishinggui.xp.gained": "Fishing XP +%s (Lv.%s %s/%s)",
  "gui.fishinggui.xp.gained.max": "Fishing XP +%s (Lv.%s MAX)",
```

and to `ja_jp.json`:

```json
  "gui.fishinggui.level": "釣りLv.%s",
  "gui.fishinggui.xp.gained": "釣り経験値 +%s（Lv.%s %s/%s）",
  "gui.fishinggui.xp.gained.max": "釣り経験値 +%s（Lv.%s 最大）",
```

(Mind the trailing commas of the existing last entries.)

- [ ] **Step 10: Verify and commit**

Run: `./gradlew compileJava test build`
Expected: BUILD SUCCESSFUL, all tests pass.
Dedicated-server smoke test (as in earlier phases): log shows "Loaded fishing level curve" and "Done (", no fishinggui errors.

```bash
git add -A && git commit -m "feat: fishing progress storage, sync, xp on release, bite sound"
```

---

### Task 3: Client UI (catch screen, fight screen, level-up overlay)

**Files:**
- Modify: `…/client/CatchScreen.java`, `…/client/FightScreen.java`, `…/client/LevelUpOverlay.java` (replace the stub), lang files
- Create: `…/client/ClientEvents.java` (overlay registration)

**Interfaces:**
- Consumes: `ClientProgress.level()/xp()/xpToNext()`, `LevelUpOverlay.start(int)` (Task 2).
- Produces: `LevelUpOverlay.start(int level)`, `LevelUpOverlay.render(GuiGraphics g, int width, int height)`.

- [ ] **Step 1: CatchScreen level bar.** Make the stats box taller and add a level row. In `render(...)` change the stats-box block to:

```java
        // stats box
        int boxW = 190;
        int boxH = 56;
        int boxX = cx - boxW / 2;
        g.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0x99000000);
        g.renderOutline(boxX, boxY, boxW, boxH, 0xFFFFFFFF);
        drawStat(g, cx - boxW / 4, boxY + 6, Component.translatable("gui.fishinggui.catch.weight"),
                String.format(Locale.ROOT, "%.1f kg", weight));
        drawStat(g, cx + boxW / 4, boxY + 6, Component.translatable("gui.fishinggui.catch.xp"), "+" + xp);
        drawLevelBar(g, boxX + 8, boxY + 38, boxW - 16);
```

and set `int boxY = this.height - 110;` (it is declared earlier in `render` for the icon sizing; change that single declaration so both uses agree). The buttons stay at `this.height - 44` (box bottom is `height - 54`). Add:

```java
    private void drawLevelBar(GuiGraphics g, int x, int y, int w) {
        int need = ClientProgress.xpToNext();
        float frac = need <= 0 ? 1f : Math.min(1f, ClientProgress.xp() / (float) need);
        String text = need <= 0
                ? Component.translatable("gui.fishinggui.level", ClientProgress.level()).getString() + " MAX"
                : Component.translatable("gui.fishinggui.level", ClientProgress.level()).getString()
                        + "  " + ClientProgress.xp() + "/" + need;
        g.drawString(this.font, text, x, y - 10, 0xFFFFFF, false);
        g.fill(x, y, x + w, y + 6, 0xFF222222);
        g.fill(x, y, x + (int) (w * frac), y + 6, 0xFF55FF55);
        g.renderOutline(x - 1, y - 1, w + 2, 8, 0xFFFFFFFF);
    }
```

- [ ] **Step 2: FightScreen label.** In `FightScreen.render(...)` (starts at line ~96), draw once, near the top-left, after existing drawing and before the method returns:

```java
        g.drawString(this.font, Component.translatable("gui.fishinggui.level", ClientProgress.level()), 6, 6,
                0xFFFFFF, true);
```

(Add `import net.minecraft.network.chat.Component;` if missing. Keep it clear of the shake transform: place it where no `pushPose` shake translation is active, otherwise the label shakes too — that is acceptable but prefer outside.)

- [ ] **Step 3: LevelUpOverlay.** Replace the stub:

```java
package com.example.fishinggui.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/** Full-screen "LEVEL UP!" celebration: rays, sparkles and a fanfare, about three seconds. */
public final class LevelUpOverlay {
    private static final long DURATION_MS = 3000L;
    private static final int RAYS = 14;
    private static final int SPARKLES = 40;

    private static long startMs = -1L;
    private static int level;

    private LevelUpOverlay() {
    }

    public static void start(int newLevel) {
        level = newLevel;
        startMs = System.currentTimeMillis();
        var sounds = Minecraft.getInstance().getSoundManager();
        sounds.play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f));
        sounds.play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f));
    }

    public static void render(GuiGraphics g, int width, int height) {
        if (startMs < 0) {
            return;
        }
        long elapsed = System.currentTimeMillis() - startMs;
        if (elapsed >= DURATION_MS) {
            startMs = -1L;
            return;
        }
        float t = elapsed / (float) DURATION_MS;
        float fade = t < 0.08f ? t / 0.08f : (t > 0.8f ? (1f - t) / 0.2f : 1f);
        int alpha = Math.max(0, Math.min(255, (int) (fade * 255)));
        int cx = width / 2;
        int cy = height / 2 - 20;

        // soft dim behind the text
        g.fill(0, cy - 60, width, cy + 60, ((int) (fade * 110) << 24));

        // rotating rays
        float spin = elapsed / 20f;
        float len = Math.max(width, height) * (0.25f + 0.35f * Math.min(1f, t * 3f));
        for (int i = 0; i < RAYS; i++) {
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0);
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(spin + i * (360f / RAYS)));
            int rayColor = ((int) (fade * 70) << 24) | (i % 2 == 0 ? 0xFFE066 : 0xFFFFFF);
            g.fill(0, -2, (int) len, 2, rayColor);
            g.pose().popPose();
        }

        // sparkles (deterministic positions that twinkle)
        for (int i = 0; i < SPARKLES; i++) {
            double a = i * 2.399963;
            double r = 40 + (i * 37 % 160) + 40 * t;
            int sx = cx + (int) (Math.cos(a) * r * 1.6);
            int sy = cy + (int) (Math.sin(a) * r * 0.7);
            float twinkle = 0.5f + 0.5f * (float) Math.sin(elapsed / 90.0 + i);
            int c = ((int) (fade * twinkle * 255) << 24) | 0xFFFFAA;
            g.fill(sx - 1, sy - 3, sx + 1, sy + 3, c);
            g.fill(sx - 3, sy - 1, sx + 3, sy + 1, c);
        }

        // title with a pop-in scale
        float pop = t < 0.15f ? 0.4f + 1.1f * (t / 0.15f) : (t < 0.25f ? 1.5f - 0.5f * ((t - 0.15f) / 0.1f) : 1.0f);
        drawCentered(g, Component.translatable("gui.fishinggui.levelup.title"), cx, cy - 22, 6f * pop,
                (alpha << 24) | 0xFFD23C);
        drawCentered(g, Component.translatable("gui.fishinggui.levelup.level", level), cx, cy + 22, 3.5f,
                (alpha << 24) | 0xFFFFFF);
    }

    private static void drawCentered(GuiGraphics g, Component text, int x, int y, float scale, int argb) {
        var font = Minecraft.getInstance().font;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1f);
        g.drawString(font, text, -font.width(text) / 2, -font.lineHeight / 2, argb, true);
        g.pose().popPose();
    }
}
```

(`drawString` ignores alpha < ~4; when `alpha` is tiny the text is skipped by vanilla, which is fine. `PLAYER_LEVELUP` is the vanilla level-up fanfare.)

- [ ] **Step 4: Overlay registration.** `…/client/ClientEvents.java`:

```java
package com.example.fishinggui.client;

import com.example.fishinggui.FishingGuiMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("level_up",
                (gui, graphics, partialTick, width, height) -> LevelUpOverlay.render(graphics, width, height));
    }
}
```

- [ ] **Step 5: Lang.** `en_us.json`:

```json
  "gui.fishinggui.levelup.title": "LEVEL UP!",
  "gui.fishinggui.levelup.level": "Fishing Lv.%s",
```

`ja_jp.json`:

```json
  "gui.fishinggui.levelup.title": "レベルアップ！",
  "gui.fishinggui.levelup.level": "釣りLv.%s",
```

- [ ] **Step 6: Verify and commit**

Run: `./gradlew compileJava test build`
Expected: BUILD SUCCESSFUL. (Rendering is checked in-game by the user.)

```bash
git add -A && git commit -m "feat: level bar on catch screen, level label on fight screen, level-up overlay"
```

---

### Task 4: Admin commands

**Files:**
- Create: `…/command/FishingCommand.java`
- Modify: lang files

**Interfaces:**
- Consumes: `FishingService.change(player, next, gained)`, `FishingData.get`, `LevelCurveLoader.curve()`, `FishingProgress.withLevel/withXp/addXp`.

- [ ] **Step 1: `FishingCommand`**

```java
package com.example.fishinggui.command;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.data.LevelCurveLoader;
import com.example.fishinggui.level.FishingData;
import com.example.fishinggui.level.FishingProgress;
import com.example.fishinggui.level.FishingService;
import com.example.fishinggui.level.LevelCurve;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.function.BiFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** /fishinggui level|xp set|add and /fishinggui query (operators only). */
@Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID)
public final class FishingCommand {
    private FishingCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("fishinggui").requires(s -> s.hasPermission(2))
                .then(Commands.literal("level")
                        .then(change("set", IntegerArgumentType.integer(1, LevelCurve.MAX_LEVEL),
                                (p, v) -> p.withLevel(v)))
                        .then(change("add", IntegerArgumentType.integer(-LevelCurve.MAX_LEVEL, LevelCurve.MAX_LEVEL),
                                (p, v) -> p.withLevel(p.level() + v))))
                .then(Commands.literal("xp")
                        .then(change("set", IntegerArgumentType.integer(0),
                                (p, v) -> p.withXp(v, LevelCurveLoader.curve())))
                        .then(change("add", IntegerArgumentType.integer(0),
                                (p, v) -> p.addXp(v, LevelCurveLoader.curve()))))
                .then(Commands.literal("query").then(Commands.argument("target", EntityArgument.player())
                        .executes(FishingCommand::query))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> change(String name,
            com.mojang.brigadier.arguments.ArgumentType<Integer> type,
            BiFunction<FishingProgress, Integer, FishingProgress> op) {
        return Commands.literal(name).then(Commands.argument("targets", EntityArgument.players())
                .then(Commands.argument("value", type).executes(ctx -> {
                    Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                    int value = IntegerArgumentType.getInteger(ctx, "value");
                    boolean isXp = ctx.getNodes().stream().anyMatch(n -> n.getNode().getName().equals("xp"));
                    for (ServerPlayer p : targets) {
                        FishingProgress before = FishingData.get(p);
                        FishingProgress after = op.apply(before, value);
                        int gained = isXp && name.equals("add") ? value : 0;
                        FishingService.change(p, after, gained);
                    }
                    ctx.getSource().sendSuccess(
                            () -> Component.translatable("commands.fishinggui.updated", targets.size()), true);
                    return targets.size();
                })));
    }

    private static int query(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "target");
        FishingProgress progress = FishingData.get(p);
        int need = LevelCurveLoader.curve().xpToNext(progress.level());
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.fishinggui.query",
                p.getDisplayName(), progress.level(), progress.xp(), need), false);
        return progress.level();
    }
}
```

(`level add` with a clamped result is handled by the `FishingProgress` constructor. `xp set` with a value that carries over triggers `leveledUp` through `FishingService.change`. If the `ctx.getNodes()` check for `"xp"` proves awkward, replace it by passing a `boolean xpAdd` parameter into `change(...)` instead — the behaviour must stay: chat XP notice only for `xp add`.)

- [ ] **Step 2: Lang.** `en_us.json`:

```json
  "commands.fishinggui.updated": "Updated fishing progress of %s player(s)",
  "commands.fishinggui.query": "%s: Fishing Lv.%s (%s/%s XP)",
```

`ja_jp.json`:

```json
  "commands.fishinggui.updated": "%s人の釣り進行度を更新しました",
  "commands.fishinggui.query": "%s: 釣りLv.%s（経験値 %s/%s）",
```

- [ ] **Step 3: Verify and commit**

Run: `./gradlew compileJava test build`
Expected: BUILD SUCCESSFUL.
Dedicated-server smoke test: start the server, confirm "Done (" with no fishinggui errors, then stop it.

```bash
git add -A && git commit -m "feat: /fishinggui admin commands for level and xp"
```

---

## Manual in-game checks (user)

1. A chime plays the moment a fish bites, before clicking.
2. Release gives items 0, XP: chat shows "釣り経験値 +N（Lv.L x/y）". Keep gives no XP.
3. Level-up shows the big "LEVEL UP!" overlay with a fanfare; no chat message for the level-up itself.
4. Catch screen shows Lv. and XP bar inside the stats box without overlapping; fight screen shows "釣りLv.N" at the top-left.
5. `/fishinggui level set @s 5`, `xp add @s 30`, `query @s` work; non-ops cannot run them.
6. Quit and rejoin, die and respawn: level and XP are kept.
