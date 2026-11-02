# Bait Items (Phase 2-A) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add three bait items (worm, glow bug, sturdy paste). A bait in the off hand when a fight starts modifies the fight and is consumed when the fight ends.

**Architecture:** A pure-Java `BaitType` enum + `BaitEffect` helper adjust `FishProfile` and report a loot-luck bonus (unit-tested). `ModItems` registers the items and maps an `ItemStack` to a `BaitType`. `FishingHookHandler` reads the off-hand bait at fight start; `FightManager` stores it in `ActiveFight` and consumes it in `end()`; `CatchResolver.success` adds the luck bonus.

**Tech Stack:** Minecraft 1.20.1, Forge 47.3.0, Java 17, official mappings, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-11-02-phase2a-bait-design.md`

## Global Constraints

- Mod ID `fishinggui`; package root `com.example.fishinggui`.
- Every Gradle command runs with JDK 17: `export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16"` (default `java` is 1.8; do not use it).
- `BaitType` and `BaitEffect` import nothing from `net.minecraft` / `net.minecraftforge`.
- Effects (spec §1): worm = none; glow bug = loot luck +2; sturdy paste = `dashPower` x 0.6. All numbers live as constants in `BaitEffect`.
- Bait is read from the **off hand** only, and only when the rod is in the main hand. Otherwise `BaitType.NONE`.
- Consumption (spec §3): at fight end (any outcome via `FightManager.end`), shrink the off-hand stack by 1 only if it is still the same bait type; never in creative mode (`getAbilities().instabuild`).
- Items are registered only in the creative tab `CreativeModeTabs.TOOLS_AND_UTILITIES`; stack size 64; placeholder vanilla textures.
- Out of scope: datapack difficulty, extra rod damage, bait recipes/drops, GUI changes.

## File Structure

- Create `src/main/java/com/example/fishinggui/fight/BaitType.java` — enum NONE/WORM/GLOW_BUG/STURDY_PASTE.
- Create `src/main/java/com/example/fishinggui/fight/BaitEffect.java` — `apply(BaitType, FishProfile)`, `luckBonus(BaitType)`.
- Create `src/test/java/com/example/fishinggui/fight/BaitEffectTest.java`
- Create `src/main/java/com/example/fishinggui/item/ModItems.java` — registration, creative tab, `typeOf(ItemStack)`.
- Create `src/main/resources/assets/fishinggui/lang/en_us.json`, `models/item/{worm,glow_bug,sturdy_paste}.json`
- Modify `FishingGuiMod.java`, `hook/FishingHookHandler.java`, `fight/FightManager.java`, `hook/CatchResolver.java`

---

### Task 1: BaitType and BaitEffect (pure Java)

**Files:**
- Create: `src/main/java/com/example/fishinggui/fight/BaitType.java`
- Create: `src/main/java/com/example/fishinggui/fight/BaitEffect.java`
- Test: `src/test/java/com/example/fishinggui/fight/BaitEffectTest.java`

**Interfaces:**
- Consumes: `FishProfile(float stamina, int dashInterval, float dashPower, float tensionGain)` record.
- Produces: `enum BaitType { NONE, WORM, GLOW_BUG, STURDY_PASTE }`; `BaitEffect.apply(BaitType, FishProfile) -> FishProfile`; `BaitEffect.luckBonus(BaitType) -> float`.

- [ ] **Step 1: Write the failing test**

```java
package com.example.fishinggui.fight;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BaitEffectTest {
    private static final FishProfile BASE = new FishProfile(100f, 70, 1.0f, 1.0f);

    @Test
    void noneAndWormLeaveTheProfileUnchanged() {
        assertEquals(BASE, BaitEffect.apply(BaitType.NONE, BASE));
        assertEquals(BASE, BaitEffect.apply(BaitType.WORM, BASE));
        assertEquals(BASE, BaitEffect.apply(BaitType.GLOW_BUG, BASE));
    }

    @Test
    void sturdyPasteWeakensDashesOnly() {
        FishProfile p = BaitEffect.apply(BaitType.STURDY_PASTE, BASE);
        assertEquals(0.6f, p.dashPower(), 1e-6f);
        assertEquals(BASE.stamina(), p.stamina());
        assertEquals(BASE.dashInterval(), p.dashInterval());
        assertEquals(BASE.tensionGain(), p.tensionGain());
    }

    @Test
    void onlyGlowBugAddsLuck() {
        assertEquals(0f, BaitEffect.luckBonus(BaitType.NONE));
        assertEquals(0f, BaitEffect.luckBonus(BaitType.WORM));
        assertEquals(2f, BaitEffect.luckBonus(BaitType.GLOW_BUG));
        assertEquals(0f, BaitEffect.luckBonus(BaitType.STURDY_PASTE));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16" && ./gradlew test --tests '*BaitEffectTest'`
Expected: compilation FAIL (`BaitType` / `BaitEffect` not found).

- [ ] **Step 3: Write minimal implementation**

`BaitType.java`:

```java
package com.example.fishinggui.fight;

public enum BaitType {
    NONE,
    WORM,
    GLOW_BUG,
    STURDY_PASTE
}
```

`BaitEffect.java`:

```java
package com.example.fishinggui.fight;

/** Maps a bait to its effect on a fight. Pure Java so the numbers stay unit-testable. */
public final class BaitEffect {
    static final float GLOW_BUG_LUCK = 2.0f;
    static final float STURDY_PASTE_DASH_FACTOR = 0.6f;

    private BaitEffect() {
    }

    public static FishProfile apply(BaitType bait, FishProfile base) {
        if (bait == BaitType.STURDY_PASTE) {
            return new FishProfile(base.stamina(), base.dashInterval(),
                    base.dashPower() * STURDY_PASTE_DASH_FACTOR, base.tensionGain());
        }
        return base;
    }

    public static float luckBonus(BaitType bait) {
        return bait == BaitType.GLOW_BUG ? GLOW_BUG_LUCK : 0f;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16" && ./gradlew test`
Expected: PASS (13 existing + 3 new tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/fishinggui/fight/BaitType.java src/main/java/com/example/fishinggui/fight/BaitEffect.java src/test/java/com/example/fishinggui/fight/BaitEffectTest.java
git commit -m "feat: add BaitType and BaitEffect"
```

---

### Task 2: Bait items, registration and assets

**Files:**
- Create: `src/main/java/com/example/fishinggui/item/ModItems.java`
- Create: `src/main/resources/assets/fishinggui/lang/en_us.json`
- Create: `src/main/resources/assets/fishinggui/models/item/worm.json`, `glow_bug.json`, `sturdy_paste.json`
- Modify: `src/main/java/com/example/fishinggui/FishingGuiMod.java`

**Interfaces:**
- Consumes: `BaitType` (Task 1).
- Produces: `ModItems.register(IEventBus)`; `ModItems.typeOf(ItemStack) -> BaitType` (NONE for empty or non-bait stacks).

- [ ] **Step 1: Write ModItems**

```java
package com.example.fishinggui.item;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.fight.BaitType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FishingGuiMod.MOD_ID);

    public static final RegistryObject<Item> WORM = ITEMS.register("worm", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GLOW_BUG =
            ITEMS.register("glow_bug", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STURDY_PASTE =
            ITEMS.register("sturdy_paste", () -> new Item(new Item.Properties()));

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(ModItems::addToCreativeTab);
    }

    public static BaitType typeOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return BaitType.NONE;
        }
        if (stack.is(WORM.get())) {
            return BaitType.WORM;
        }
        if (stack.is(GLOW_BUG.get())) {
            return BaitType.GLOW_BUG;
        }
        if (stack.is(STURDY_PASTE.get())) {
            return BaitType.STURDY_PASTE;
        }
        return BaitType.NONE;
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(WORM);
            event.accept(GLOW_BUG);
            event.accept(STURDY_PASTE);
        }
    }
}
```

- [ ] **Step 2: Register from the mod constructor**

In `FishingGuiMod`, change the constructor to:

```java
    public FishingGuiMod() {
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);
        com.example.fishinggui.item.ModItems.register(modBus);
    }
```

(Use a normal import for `ModItems` instead of the fully qualified name.)

- [ ] **Step 3: Assets**

`assets/fishinggui/lang/en_us.json`:

```json
{
  "item.fishinggui.worm": "Worm",
  "item.fishinggui.glow_bug": "Glow Bug",
  "item.fishinggui.sturdy_paste": "Sturdy Paste"
}
```

`assets/fishinggui/models/item/worm.json`:

```json
{ "parent": "minecraft:item/generated", "textures": { "layer0": "minecraft:item/string" } }
```

`glow_bug.json`: same, `"layer0": "minecraft:item/glowstone_dust"`.
`sturdy_paste.json`: same, `"layer0": "minecraft:item/slime_ball"`.

- [ ] **Step 4: Build**

Run: `export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16" && ./gradlew compileJava test build`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main
git commit -m "feat: register bait items with placeholder assets"
```

---

### Task 3: Wire bait into the fight

**Files:**
- Modify: `src/main/java/com/example/fishinggui/hook/FishingHookHandler.java`
- Modify: `src/main/java/com/example/fishinggui/fight/FightManager.java`
- Modify: `src/main/java/com/example/fishinggui/hook/CatchResolver.java`

**Interfaces:**
- Consumes: `BaitType`, `BaitEffect.apply/luckBonus`, `ModItems.typeOf`.
- Produces: `FightManager.start(ServerPlayer, FishingHook, InteractionHand, FishProfile, BaitType)`; `CatchResolver.success(ServerPlayer, FishingHook, InteractionHand, float bonusLuck)`.

- [ ] **Step 1: FishingHookHandler** — replace the final `FightManager.start(...)` line with:

```java
        BaitType bait = event.getHand() == InteractionHand.MAIN_HAND
                ? ModItems.typeOf(player.getOffhandItem())
                : BaitType.NONE;
        FightManager.start(player, hook, event.getHand(), BaitEffect.apply(bait, FishProfile.DEFAULT), bait);
```

Add imports `BaitEffect`, `BaitType`, `ModItems`, `net.minecraft.world.InteractionHand`.

- [ ] **Step 2: FightManager**
  - `ActiveFight` record becomes `(FightSession session, FishingHook hook, InteractionHand hand, BaitType bait)`.
  - `start(...)` gains a trailing `BaitType bait` parameter and passes it to `ActiveFight`.
  - In `end(...)`, after the success/failure resolution, call `consumeBait(player, fight.bait())`; success call becomes `CatchResolver.success(player, fight.hook(), fight.hand(), BaitEffect.luckBonus(fight.bait()))`.
  - Add:

```java
    private static void consumeBait(ServerPlayer player, BaitType bait) {
        if (bait == BaitType.NONE || player.getAbilities().instabuild) {
            return;
        }
        ItemStack offhand = player.getOffhandItem();
        if (ModItems.typeOf(offhand) == bait) {
            offhand.shrink(1);
        }
    }
```

  Add imports `ItemStack`, `ModItems`.

- [ ] **Step 3: CatchResolver** — `success` gains a `float bonusLuck` parameter; the luck line becomes `float luck = EnchantmentHelper.getFishingLuckBonus(rod) + player.getLuck() + bonusLuck;`.

- [ ] **Step 4: Build and test**

Run: `export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16" && ./gradlew compileJava test build`
Expected: BUILD SUCCESSFUL; 16 tests pass.

- [ ] **Step 5: Dedicated-server smoke test**

Run `./gradlew runServer` (accept the EULA in `run/eula.txt`), confirm the log shows `Done (` and no errors mentioning `fishinggui`, then stop the server by killing the process (the `stop` command via stdin does not shut it down here).

- [ ] **Step 6: Commit**

```bash
git add src/main
git commit -m "feat: apply and consume off-hand bait during fights"
```

- [ ] **Step 7: Manual in-game checklist (human, `./gradlew runClient`)**

1. Creative → Tools tab shows Worm / Glow Bug / Sturdy Paste with English names and placeholder icons.
2. Survival: `/give @s fishinggui:worm 5`, hold a rod in the main hand and the worm in the off hand, fight a fish → worm count drops by 1 when the fight ends (success, line break, or ESC).
3. No bait in the off hand → fights as in Phase 1.
4. Creative mode → bait count does not drop.
5. Sturdy paste: dashes visibly weaker. Glow bug: no visible change (luck only).
6. Swap the off hand to a different item mid-fight → nothing is consumed.
