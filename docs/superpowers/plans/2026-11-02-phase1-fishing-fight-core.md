# Fishing Fight Core (Phase 1) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** On a vanilla fishing bite, the first right-click starts a server-authoritative tension minigame (GUI on the client); winning yields the vanilla fishing loot, losing cuts the line.

**Architecture:** A pure-Java `FightSession` (no Minecraft imports) holds all tension/stamina rules and is unit-tested. `FightManager` (server) owns one session per player, ticks it, and talks to the client through 3 Forge `SimpleChannel` packets. `FishingHookHandler` intercepts `PlayerInteractEvent.RightClickItem` and reads `FishingHook.nibble` via an Access Transformer. `FightScreen` (client, non-pausing `Screen`) draws gauges and reports right-mouse hold state.

**Tech Stack:** Minecraft 1.20.1, Forge 47.3.0 (ForgeGradle 6, Gradle wrapper from the Forge MDK), Java 17, official (Mojang) mappings, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-11-02-fishing-fight-design.md` (Phase 1 only: no bait, no datapack difficulty, no custom fish, failure has no extra penalty).

## Global Constraints

- Minecraft `1.20.1`, Forge `47.3.0`, mappings `official` / `1.20.1`, Java 17.
- Mod ID `fishinggui`; Java package root `com.example.fishinggui`.
- Every Gradle command runs with JDK 17: `export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16"` (the default `java` on this machine is 1.8; do not use it).
- `FightSession` and `FishProfile` import nothing from `net.minecraft` / `net.minecraftforge` (so they stay unit-testable).
- Tension model values (spec §3): reel +1.5 tension / -1.0 stamina per tick; release -2.0 tension / +0.3 stamina; dash +2.5 tension (stamina frozen); `MAX_TENSION` 100; danger zone from 70; telegraph 10 ticks before a dash; dash lasts 20-40 ticks.
- Server decides everything; the client only sends input and renders what `FightStatePacket` says.
- Client-only classes (`net.minecraft.client.*`, `Screen`) must never be referenced from common code except through `DistExecutor`.

## File Structure

```
build.gradle, settings.gradle, gradle.properties, .gitignore     (Task 1)
gradlew, gradlew.bat, gradle/wrapper/*                           (Task 1, copied from Forge MDK)
src/main/resources/META-INF/mods.toml                            (Task 1)
src/main/resources/META-INF/accesstransformer.cfg                (Task 1)
src/main/resources/pack.mcmeta                                   (Task 1)
src/main/java/com/example/fishinggui/FishingGuiMod.java          (Task 1; Task 3 adds network setup)
src/main/java/com/example/fishinggui/fight/FishProfile.java      (Task 2)
src/main/java/com/example/fishinggui/fight/FightSession.java     (Task 2)
src/test/java/com/example/fishinggui/fight/FightSessionTest.java (Task 2)
src/main/java/com/example/fishinggui/net/ModNetwork.java         (Task 3)
src/main/java/com/example/fishinggui/net/FightStartPacket.java   (Task 3)
src/main/java/com/example/fishinggui/net/FightStatePacket.java   (Task 3)
src/main/java/com/example/fishinggui/net/ReelInputPacket.java    (Task 3)
src/main/java/com/example/fishinggui/fight/FightManager.java     (Task 3)
src/main/java/com/example/fishinggui/client/ClientFightHooks.java(Task 3 stub, Task 5 fills)
src/main/java/com/example/fishinggui/hook/CatchResolver.java     (Task 4)
src/main/java/com/example/fishinggui/hook/FishingHookHandler.java(Task 4)
src/main/java/com/example/fishinggui/client/FightScreen.java     (Task 5)
```

Tasks are strictly sequential (each depends on the previous). Do not run two Gradle builds in the same directory at once.

---

### Task 1: Project scaffold

**Files:**
- Create: `build.gradle`, `settings.gradle`, `gradle.properties`, `.gitignore`
- Create (copied from MDK): `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`
- Create: `src/main/resources/META-INF/mods.toml`, `src/main/resources/META-INF/accesstransformer.cfg`, `src/main/resources/pack.mcmeta`
- Create: `src/main/java/com/example/fishinggui/FishingGuiMod.java`

**Interfaces:**
- Produces: `FishingGuiMod.MOD_ID` (`public static final String`), a loadable mod, a working `./gradlew build` / `./gradlew test`, and an AT entry making `FishingHook.nibble` public.

- [ ] **Step 1: Get the Gradle wrapper from the official Forge MDK**

Run (in `F:\mod\FishingGUImod`):
```bash
mkdir -p /tmp/mdk && cd /tmp/mdk
curl -L -o mdk.zip https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.3.0/forge-1.20.1-47.3.0-mdk.zip
unzip -o mdk.zip -d mdk
cd /f/mod/FishingGUImod
cp /tmp/mdk/mdk/gradlew /tmp/mdk/mdk/gradlew.bat .
mkdir -p gradle && cp -r /tmp/mdk/mdk/gradle/wrapper gradle/
```
Expected: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` exist. (If the download fails, report BLOCKED; do not invent the wrapper.)

- [ ] **Step 2: Write `settings.gradle`**

```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = 'MinecraftForge'
            url = 'https://maven.minecraftforge.net/'
        }
    }
}

rootProject.name = 'fishinggui'
```

- [ ] **Step 3: Write `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx3G
org.gradle.daemon=false

minecraft_version=1.20.1
minecraft_version_range=[1.20.1,1.21)
forge_version=47.3.0
forge_version_range=[47,)
loader_version_range=[47,)
mapping_channel=official
mapping_version=1.20.1

mod_id=fishinggui
mod_name=Fishing GUI
mod_license=All Rights Reserved
mod_version=0.1.0
mod_group_id=com.example.fishinggui
mod_authors=jskk
mod_description=Tension-based fishing minigame.
```

- [ ] **Step 4: Write `build.gradle`**

```groovy
plugins {
    id 'eclipse'
    id 'idea'
    id 'maven-publish'
    id 'net.minecraftforge.gradle' version '[6.0,6.2)'
}

version = mod_version
group = mod_group_id

base {
    archivesName = mod_id
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

minecraft {
    mappings channel: mapping_channel, version: mapping_version
    copyIdeResources = true
    accessTransformer = file('src/main/resources/META-INF/accesstransformer.cfg')

    runs {
        configureEach {
            workingDirectory project.file('run')
            property 'forge.logging.markers', 'REGISTRIES'
            property 'forge.logging.console.level', 'debug'
            mods {
                "${mod_id}" {
                    source sourceSets.main
                }
            }
        }
        client {
            property 'forge.enabledGameTestNamespaces', mod_id
        }
        server {
            property 'forge.enabledGameTestNamespaces', mod_id
            args '--nogui'
        }
    }
}

sourceSets.main.resources { srcDir 'src/generated/resources' }

dependencies {
    minecraft "net.minecraftforge:forge:${minecraft_version}-${forge_version}"

    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher:1.10.2'
}

tasks.named('test') {
    useJUnitPlatform()
}

tasks.named('processResources', ProcessResources).configure {
    var replaceProperties = [
            minecraft_version: minecraft_version, minecraft_version_range: minecraft_version_range,
            forge_version: forge_version, forge_version_range: forge_version_range,
            loader_version_range: loader_version_range,
            mod_id: mod_id, mod_name: mod_name, mod_license: mod_license, mod_version: mod_version,
            mod_authors: mod_authors, mod_description: mod_description,
    ]
    inputs.properties replaceProperties

    filesMatching(['META-INF/mods.toml', 'pack.mcmeta']) {
        expand replaceProperties + [project: project]
    }
}

tasks.named('jar', Jar).configure {
    manifest {
        attributes([
                'Specification-Title'     : mod_id,
                'Specification-Vendor'    : mod_authors,
                'Specification-Version'   : '1',
                'Implementation-Title'    : project.name,
                'Implementation-Version'  : project.jar.archiveVersion,
                'Implementation-Vendor'   : mod_authors,
        ])
    }
    finalizedBy 'reobfJar'
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}
```

- [ ] **Step 5: Write `.gitignore`**

```
.gradle/
build/
run/
.idea/
.vscode/
*.iml
eclipse/
out/
```

- [ ] **Step 6: Write resources**

`src/main/resources/META-INF/mods.toml`:
```toml
modLoader="javafml"
loaderVersion="${loader_version_range}"
license="${mod_license}"

[[mods]]
modId="${mod_id}"
version="${mod_version}"
displayName="${mod_name}"
authors="${mod_authors}"
description='''${mod_description}'''

[[dependencies.${mod_id}]]
    modId="forge"
    mandatory=true
    versionRange="${forge_version_range}"
    ordering="NONE"
    side="BOTH"

[[dependencies.${mod_id}]]
    modId="minecraft"
    mandatory=true
    versionRange="${minecraft_version_range}"
    ordering="NONE"
    side="BOTH"
```

`src/main/resources/pack.mcmeta`:
```json
{
  "pack": {
    "description": "${mod_name} resources",
    "pack_format": 15
  }
}
```

`src/main/resources/META-INF/accesstransformer.cfg` (temporary, the real line is added in Step 9):
```
# Access transformers for fishinggui
```

- [ ] **Step 7: Write the mod entry class**

`src/main/java/com/example/fishinggui/FishingGuiMod.java`:
```java
package com.example.fishinggui;

import net.minecraftforge.fml.common.Mod;

@Mod(FishingGuiMod.MOD_ID)
public class FishingGuiMod {
    public static final String MOD_ID = "fishinggui";

    public FishingGuiMod() {
    }
}
```

- [ ] **Step 8: Build once to let ForgeGradle download and set up Minecraft**

Run:
```bash
export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16"
cd /f/mod/FishingGUImod && ./gradlew build
```
Expected: `BUILD SUCCESSFUL` (first run takes several minutes: downloads and decompiles Minecraft). If the Gradle/ForgeGradle combination fails, compare `build.gradle` against `/tmp/mdk/mdk/build.gradle` and align with the MDK, keeping the `test` task and JUnit dependencies.

- [ ] **Step 9: Resolve the SRG name of `FishingHook.nibble` and add the AT line**

ForgeGradle 6 Access Transformers use SRG names (`f_NNNNN_`), not Mojang names. Find the SRG name of the field `nibble` in `net.minecraft.world.entity.projectile.FishingHook`:
1. Search the Gradle cache (`~/.gradle/caches/forge_gradle/`) for the mapping files created in Step 8 (the Mojang `client.txt`/official mappings and the `joined.tsrg` from MCPConfig), then map `FishingHook.nibble` -> obfuscated name -> `f_NNNNN_`.
2. If that is impractical, look the field up for 1.20.1 on a mappings site (for example linkie.shedaniel.dev, Mojang -> SRG, version 1.20.1) or in the decompiled-and-mapped sources jar, which keeps the SRG name in the AT comment of other mods.

Replace the content of `src/main/resources/META-INF/accesstransformer.cfg` with (substituting the real number):
```
public net.minecraft.world.entity.projectile.FishingHook f_NNNNN_ # nibble
```

- [ ] **Step 10: Verify the AT works**

Temporarily add to `FishingGuiMod` a method `static int probe(net.minecraft.world.entity.projectile.FishingHook h) { return h.nibble; }`, run `./gradlew compileJava`. Expected: compiles. Then **remove the probe method** and run `./gradlew compileJava` again. If the first compile fails with "nibble has private access", the SRG name is wrong; redo Step 9.

- [ ] **Step 11: Initialise git and commit**

```bash
cd /f/mod/FishingGUImod
git init
git add .
git commit -m "chore: scaffold Forge 1.20.1 mod with AT for FishingHook.nibble"
```
Expected: commit created (docs/ is included).

---

### Task 2: FishProfile and FightSession (pure Java, TDD)

**Files:**
- Create: `src/main/java/com/example/fishinggui/fight/FishProfile.java`
- Create: `src/main/java/com/example/fishinggui/fight/FightSession.java`
- Test: `src/test/java/com/example/fishinggui/fight/FightSessionTest.java`

**Interfaces:**
- Produces:
  - `record FishProfile(float stamina, int dashInterval, float dashPower, float tensionGain)` with `static final FishProfile DEFAULT = new FishProfile(100f, 70, 1.0f, 1.0f)`. `dashInterval` is the average ticks between dashes.
  - `class FightSession` with `enum Phase { ACTIVE, SUCCESS, FAILED }`; constants `public static final float MAX_TENSION = 100f, DANGER_TENSION = 70f`; package-private `TELEGRAPH_TICKS = 10`;
    `FightSession(FishProfile profile, java.util.Random random)`;
    `void setReeling(boolean)` (max 2 accepted per tick, resets silence timer); `void tick()`;
    getters `float tension()`, `float stamina()`, `float maxStamina()`, `boolean reeling()`, `boolean telegraphing()`, `boolean dashing()`, `Phase phase()`.

- [ ] **Step 1: Write the failing tests**

`src/test/java/com/example/fishinggui/fight/FightSessionTest.java`:
```java
package com.example.fishinggui.fight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class FightSessionTest {
    private static final FishProfile CALM = new FishProfile(100f, 1_000_000, 1f, 1f);

    private static FightSession session(FishProfile profile) {
        return new FightSession(profile, new Random(42));
    }

    private static void tick(FightSession s, int n) {
        for (int i = 0; i < n; i++) {
            s.tick();
        }
    }

    @Test
    void holdingReelWithoutRestBreaksTheLine() {
        FightSession s = session(CALM);
        s.setReeling(true);
        int ticks = 0;
        while (s.phase() == FightSession.Phase.ACTIVE && ticks < 200) {
            s.tick();
            ticks++;
        }
        assertEquals(FightSession.Phase.FAILED, s.phase());
        assertEquals(67, ticks);
        assertEquals(FightSession.MAX_TENSION, s.tension());
    }

    @Test
    void releasingLowersTensionAndRecoversStamina() {
        FightSession s = session(CALM);
        s.setReeling(true);
        tick(s, 10);
        assertEquals(15f, s.tension(), 1e-3f);
        assertEquals(90f, s.stamina(), 1e-3f);
        s.setReeling(false);
        tick(s, 10);
        assertEquals(0f, s.tension(), 1e-3f);
        assertEquals(93f, s.stamina(), 1e-3f);
        assertEquals(FightSession.Phase.ACTIVE, s.phase());
    }

    @Test
    void pulsingTheReelWinsTheFight() {
        FightSession s = session(CALM);
        boolean reeling = true;
        int guard = 0;
        while (s.phase() == FightSession.Phase.ACTIVE && guard++ < 5000) {
            if (reeling && s.tension() >= 60f) {
                reeling = false;
            } else if (!reeling && s.tension() <= 10f) {
                reeling = true;
            }
            s.setReeling(reeling);
            s.tick();
        }
        assertEquals(FightSession.Phase.SUCCESS, s.phase());
    }

    @Test
    void successWhenStaminaReachesZero() {
        FightSession s = session(new FishProfile(5f, 1_000_000, 1f, 1f));
        s.setReeling(true);
        tick(s, 5);
        assertEquals(FightSession.Phase.SUCCESS, s.phase());
        assertEquals(0f, s.stamina());
    }

    @Test
    void dashRaisesTensionEvenWhenReleased() {
        FightSession s = session(new FishProfile(100f, 1, 1f, 1f));
        s.setReeling(false);
        tick(s, 20);
        assertTrue(s.dashing());
        assertEquals(10f, s.tension(), 0.01f);
        assertEquals(100f, s.stamina(), 1e-3f);
    }

    @Test
    void reelingDuringDashDoesNotReduceStamina() {
        FightSession s = session(new FishProfile(100f, 1, 1f, 1f));
        s.setReeling(true);
        tick(s, 5);
        assertEquals(100f, s.stamina(), 1e-3f);
        assertEquals(20f, s.tension(), 1e-3f);
    }

    @Test
    void dashPowerScalesDashTension() {
        FightSession s = session(new FishProfile(100f, 1, 2f, 1f));
        s.setReeling(false);
        s.tick();
        assertEquals(3f, s.tension(), 1e-3f);
    }

    @Test
    void tensionGainScalesReeling() {
        FightSession s = session(new FishProfile(100f, 1_000_000, 1f, 2f));
        s.setReeling(true);
        s.tick();
        assertEquals(3f, s.tension(), 1e-3f);
    }

    @Test
    void telegraphComesTenTicksBeforeDash() {
        FightSession s = session(new FishProfile(100f, 100, 1f, 1f));
        int guard = 0;
        while (!s.telegraphing() && guard++ < 300) {
            s.tick();
        }
        assertTrue(s.telegraphing());
        assertFalse(s.dashing());
        int extra = 0;
        while (!s.dashing() && extra < 50) {
            s.tick();
            extra++;
        }
        assertEquals(FightSession.TELEGRAPH_TICKS, extra);
    }

    @Test
    void silenceReleasesTheReel() {
        FightSession s = session(CALM);
        s.setReeling(true);
        tick(s, 40);
        assertTrue(s.reeling());
        s.tick();
        assertFalse(s.reeling());
    }

    @Test
    void repeatedInputKeepsTheReelAlive() {
        FightSession s = session(CALM);
        for (int i = 0; i < 3; i++) {
            s.setReeling(true);
            tick(s, 20);
        }
        assertTrue(s.reeling());
    }

    @Test
    void inputFloodIsLimitedPerTick() {
        FightSession s = session(CALM);
        s.setReeling(true);
        s.setReeling(false);
        s.setReeling(true);
        assertFalse(s.reeling());
        s.tick();
        s.setReeling(true);
        assertTrue(s.reeling());
    }

    @Test
    void endedSessionIgnoresFurtherTicks() {
        FightSession s = session(CALM);
        s.setReeling(true);
        tick(s, 100);
        assertEquals(FightSession.Phase.FAILED, s.phase());
        float tension = s.tension();
        float stamina = s.stamina();
        s.tick();
        assertEquals(tension, s.tension());
        assertEquals(stamina, s.stamina());
    }
}
```

- [ ] **Step 2: Write the minimal types so the test compiles but fails**

`src/main/java/com/example/fishinggui/fight/FishProfile.java`:
```java
package com.example.fishinggui.fight;

public record FishProfile(float stamina, int dashInterval, float dashPower, float tensionGain) {
    public static final FishProfile DEFAULT = new FishProfile(100f, 70, 1.0f, 1.0f);
}
```

`src/main/java/com/example/fishinggui/fight/FightSession.java` (skeleton):
```java
package com.example.fishinggui.fight;

import java.util.Random;

public final class FightSession {
    public enum Phase { ACTIVE, SUCCESS, FAILED }

    public static final float MAX_TENSION = 100f;
    public static final float DANGER_TENSION = 70f;
    static final int TELEGRAPH_TICKS = 10;

    public FightSession(FishProfile profile, Random random) {
    }

    public void setReeling(boolean reeling) {
    }

    public void tick() {
    }

    public float tension() { return 0; }
    public float stamina() { return 0; }
    public float maxStamina() { return 0; }
    public boolean reeling() { return false; }
    public boolean telegraphing() { return false; }
    public boolean dashing() { return false; }
    public Phase phase() { return Phase.ACTIVE; }
}
```

- [ ] **Step 3: Run the tests and confirm they fail**

Run:
```bash
export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16"
cd /f/mod/FishingGUImod && ./gradlew test --tests "com.example.fishinggui.fight.FightSessionTest"
```
Expected: `BUILD FAILED`, with most of the 13 tests failing on assertions (not on compilation).

- [ ] **Step 4: Implement `FightSession`**

Replace `src/main/java/com/example/fishinggui/fight/FightSession.java` with:
```java
package com.example.fishinggui.fight;

import java.util.Random;

/** Server-side rules of one tension fight. Pure Java: no Minecraft types. */
public final class FightSession {
    public enum Phase { ACTIVE, SUCCESS, FAILED }

    public static final float MAX_TENSION = 100f;
    public static final float DANGER_TENSION = 70f;

    static final float REEL_TENSION = 1.5f;
    static final float REEL_STAMINA = 1.0f;
    static final float RELEASE_TENSION = 2.0f;
    static final float RELEASE_STAMINA_RECOVER = 0.3f;
    static final float DASH_TENSION = 2.5f;
    static final int TELEGRAPH_TICKS = 10;
    static final int DASH_MIN_TICKS = 20;
    static final int DASH_MAX_TICKS = 40;
    static final int INPUT_TIMEOUT_TICKS = 40;
    static final int MAX_INPUTS_PER_TICK = 2;

    private final FishProfile profile;
    private final Random random;

    private float tension;
    private float stamina;
    private boolean reeling;
    private int ticksUntilDash;
    private int dashTicksLeft;
    private int silentTicks;
    private int inputsThisTick;
    private Phase phase = Phase.ACTIVE;

    public FightSession(FishProfile profile, Random random) {
        this.profile = profile;
        this.random = random;
        this.stamina = profile.stamina();
        this.ticksUntilDash = nextDashInterval();
    }

    /** Applies a reel/release input. Inputs beyond {@link #MAX_INPUTS_PER_TICK} within one tick are ignored. */
    public void setReeling(boolean reeling) {
        if (inputsThisTick >= MAX_INPUTS_PER_TICK) {
            return;
        }
        inputsThisTick++;
        silentTicks = 0;
        this.reeling = reeling;
    }

    public void tick() {
        if (phase != Phase.ACTIVE) {
            return;
        }
        inputsThisTick = 0;
        if (++silentTicks > INPUT_TIMEOUT_TICKS) {
            reeling = false;
        }
        updateDash();
        boolean dashing = dashTicksLeft > 0;

        if (reeling) {
            tension += REEL_TENSION * profile.tensionGain();
            if (!dashing) {
                stamina -= REEL_STAMINA;
            }
        } else {
            tension -= RELEASE_TENSION;
            if (!dashing) {
                stamina = Math.min(profile.stamina(), stamina + RELEASE_STAMINA_RECOVER);
            }
        }
        if (dashing) {
            tension += DASH_TENSION * profile.dashPower();
        }
        tension = Math.max(0f, tension);

        if (tension >= MAX_TENSION) {
            tension = MAX_TENSION;
            phase = Phase.FAILED;
        } else if (stamina <= 0f) {
            stamina = 0f;
            phase = Phase.SUCCESS;
        }
    }

    private void updateDash() {
        if (dashTicksLeft > 0) {
            dashTicksLeft--;
            if (dashTicksLeft == 0) {
                ticksUntilDash = nextDashInterval();
            }
            return;
        }
        if (--ticksUntilDash <= 0) {
            dashTicksLeft = DASH_MIN_TICKS + random.nextInt(DASH_MAX_TICKS - DASH_MIN_TICKS + 1);
        }
    }

    private int nextDashInterval() {
        return Math.max(1, Math.round(profile.dashInterval() * (0.6f + 0.8f * random.nextFloat())));
    }

    public float tension() { return tension; }
    public float stamina() { return stamina; }
    public float maxStamina() { return profile.stamina(); }
    public boolean reeling() { return reeling; }
    public boolean dashing() { return dashTicksLeft > 0; }
    public boolean telegraphing() { return dashTicksLeft == 0 && ticksUntilDash <= TELEGRAPH_TICKS; }
    public Phase phase() { return phase; }
}
```

- [ ] **Step 5: Run the tests and confirm they pass**

Run: `./gradlew test --tests "com.example.fishinggui.fight.FightSessionTest"` (with `JAVA_HOME` set as above).
Expected: `BUILD SUCCESSFUL`, 13 tests passed. If a numeric assertion fails by a rounding hair, fix the test tolerance, not the rules; if an assertion fails by a whole tick, re-check the tick order against the code above.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/example/fishinggui/fight src/test
git commit -m "feat: add FightSession tension rules with unit tests"
```

---

### Task 3: Server fight runtime and network

**Files:**
- Create: `src/main/java/com/example/fishinggui/net/ModNetwork.java`, `FightStartPacket.java`, `FightStatePacket.java`, `ReelInputPacket.java`
- Create: `src/main/java/com/example/fishinggui/fight/FightManager.java`
- Create: `src/main/java/com/example/fishinggui/client/ClientFightHooks.java` (stub; Task 5 fills it)
- Create: `src/main/java/com/example/fishinggui/hook/CatchResolver.java` (minimal; Task 4 completes `success`)
- Modify: `src/main/java/com/example/fishinggui/FishingGuiMod.java`

**Interfaces:**
- Consumes: `FightSession`, `FishProfile` (Task 2).
- Produces:
  - `ModNetwork.CHANNEL` (`SimpleChannel`), `ModNetwork.register()`.
  - `FightStartPacket(float maxStamina)`; `FightStatePacket(float tension, float stamina, boolean telegraph, boolean dashing, FightSession.Phase phase)`; `ReelInputPacket(byte action)` with `ReelInputPacket.RELEASE = 0`, `REEL = 1`, `ABORT = 2`.
  - `FightManager.isFighting(Player)`, `FightManager.start(ServerPlayer, FishingHook, InteractionHand, FishProfile)`, `FightManager.handleInput(ServerPlayer, byte)`.
  - `ClientFightHooks.onStart(float maxStamina)`, `ClientFightHooks.onState(float tension, float stamina, boolean telegraph, boolean dashing, FightSession.Phase phase)`.
  - `CatchResolver.success(ServerPlayer, FishingHook, InteractionHand)`, `CatchResolver.failure(ServerPlayer, FishingHook)`.

- [ ] **Step 1: Write the three packets**

`src/main/java/com/example/fishinggui/net/FightStartPacket.java`:
```java
package com.example.fishinggui.net;

import com.example.fishinggui.client.ClientFightHooks;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server to client: a fight began, open the GUI. */
public record FightStartPacket(float maxStamina) {
    public static void encode(FightStartPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.maxStamina);
    }

    public static FightStartPacket decode(FriendlyByteBuf buf) {
        return new FightStartPacket(buf.readFloat());
    }

    public static void handle(FightStartPacket msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientFightHooks.onStart(msg.maxStamina));
    }
}
```

`src/main/java/com/example/fishinggui/net/FightStatePacket.java`:
```java
package com.example.fishinggui.net;

import com.example.fishinggui.client.ClientFightHooks;
import com.example.fishinggui.fight.FightSession;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server to client: per-tick fight state. A phase other than ACTIVE closes the GUI. */
public record FightStatePacket(float tension, float stamina, boolean telegraph, boolean dashing,
                               FightSession.Phase phase) {
    public static void encode(FightStatePacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.tension);
        buf.writeFloat(msg.stamina);
        buf.writeBoolean(msg.telegraph);
        buf.writeBoolean(msg.dashing);
        buf.writeEnum(msg.phase);
    }

    public static FightStatePacket decode(FriendlyByteBuf buf) {
        return new FightStatePacket(buf.readFloat(), buf.readFloat(), buf.readBoolean(), buf.readBoolean(),
                buf.readEnum(FightSession.Phase.class));
    }

    public static void handle(FightStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientFightHooks.onState(
                msg.tension, msg.stamina, msg.telegraph, msg.dashing, msg.phase));
    }
}
```

`src/main/java/com/example/fishinggui/net/ReelInputPacket.java`:
```java
package com.example.fishinggui.net;

import com.example.fishinggui.fight.FightManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Client to server: reel state. Sent on change and as a periodic keep-alive while the GUI is open. */
public record ReelInputPacket(byte action) {
    public static final byte RELEASE = 0;
    public static final byte REEL = 1;
    public static final byte ABORT = 2;

    public static void encode(ReelInputPacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.action);
    }

    public static ReelInputPacket decode(FriendlyByteBuf buf) {
        return new ReelInputPacket(buf.readByte());
    }

    public static void handle(ReelInputPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null) {
            FightManager.handleInput(sender, msg.action);
        }
    }
}
```

- [ ] **Step 2: Write `ModNetwork`**

`src/main/java/com/example/fishinggui/net/ModNetwork.java`:
```java
package com.example.fishinggui.net;

import com.example.fishinggui.FishingGuiMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FishingGuiMod.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private ModNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(FightStartPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FightStartPacket::encode).decoder(FightStartPacket::decode)
                .consumerMainThread(FightStartPacket::handle).add();
        CHANNEL.messageBuilder(FightStatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FightStatePacket::encode).decoder(FightStatePacket::decode)
                .consumerMainThread(FightStatePacket::handle).add();
        CHANNEL.messageBuilder(ReelInputPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ReelInputPacket::encode).decoder(ReelInputPacket::decode)
                .consumerMainThread(ReelInputPacket::handle).add();
    }
}
```
If `consumerMainThread` is not available in this Forge build, use `.consumerNetworkThread(...)` wrapping the body in `ctx.get().enqueueWork(...)` and `ctx.get().setPacketHandled(true)`.

- [ ] **Step 3: Register the network on mod setup**

Replace `src/main/java/com/example/fishinggui/FishingGuiMod.java`:
```java
package com.example.fishinggui;

import com.example.fishinggui.net.ModNetwork;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FishingGuiMod.MOD_ID)
public class FishingGuiMod {
    public static final String MOD_ID = "fishinggui";

    public FishingGuiMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
```

- [ ] **Step 4: Write the client hook stub**

`src/main/java/com/example/fishinggui/client/ClientFightHooks.java` (Task 5 replaces the bodies):
```java
package com.example.fishinggui.client;

import com.example.fishinggui.fight.FightSession;

/** Client entry points called by the S2C packets. Bodies are implemented in Task 5. */
public final class ClientFightHooks {
    private ClientFightHooks() {
    }

    public static void onStart(float maxStamina) {
    }

    public static void onState(float tension, float stamina, boolean telegraph, boolean dashing,
                               FightSession.Phase phase) {
    }
}
```

- [ ] **Step 5: Write `CatchResolver` with a failure path and a placeholder-free success path**

`src/main/java/com/example/fishinggui/hook/CatchResolver.java`:
```java
package com.example.fishinggui.hook;

import java.util.List;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Resolves a finished fight: vanilla loot on success, nothing on failure. */
public final class CatchResolver {
    private CatchResolver() {
    }

    public static void success(ServerPlayer player, FishingHook hook, InteractionHand hand) {
        ServerLevel level = player.serverLevel();
        ItemStack rod = player.getItemInHand(hand);
        float luck = EnchantmentHelper.getFishingLuckBonus(rod) + player.getLuck();

        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, hook.position())
                .withParameter(LootContextParams.TOOL, rod)
                .withParameter(LootContextParams.THIS_ENTITY, hook)
                .withParameter(LootContextParams.KILLER_ENTITY, player)
                .withLuck(luck)
                .create(LootContextParamSets.FISHING);
        LootTable table = level.getServer().getLootData().getLootTable(BuiltInLootTables.FISHING);
        List<ItemStack> loot = table.getRandomItems(params);

        ItemFishedEvent event = new ItemFishedEvent(loot, hook.onGround() ? 2 : 1, hook);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            hook.discard();
            return;
        }

        CriteriaTriggers.FISHING_ROD_HOOKED.trigger(player, rod, hook, loot);
        for (ItemStack stack : loot) {
            ItemEntity item = new ItemEntity(level, hook.getX(), hook.getY(), hook.getZ(), stack);
            double dx = player.getX() - hook.getX();
            double dy = player.getY() - hook.getY();
            double dz = player.getZ() - hook.getZ();
            item.setDeltaMovement(dx * 0.1, dy * 0.1 + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08,
                    dz * 0.1);
            level.addFreshEntity(item);
            level.addFreshEntity(new ExperienceOrb(level, player.getX(), player.getY() + 0.5,
                    player.getZ() + 0.5, player.getRandom().nextInt(6) + 1));
            if (stack.is(ItemTags.FISHES)) {
                player.awardStat(Stats.FISH_CAUGHT, 1);
            }
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.FISHING_ROD_REEL_IN, hook.position());
        rod.hurtAndBreak(event.getRodDamage(), player, p -> p.broadcastBreakEvent(hand));
        hook.discard();
    }

    public static void failure(ServerPlayer player, FishingHook hook) {
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 1.0F, 0.6F);
        hook.discard();
    }
}
```

- [ ] **Step 6: Write `FightManager`**

`src/main/java/com/example/fishinggui/fight/FightManager.java`:
```java
package com.example.fishinggui.fight;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.hook.CatchResolver;
import com.example.fishinggui.net.FightStartPacket;
import com.example.fishinggui.net.FightStatePacket;
import com.example.fishinggui.net.ModNetwork;
import com.example.fishinggui.net.ReelInputPacket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/** Owns one {@link FightSession} per fighting player and advances them on the server thread. */
@Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID)
public final class FightManager {
    private record ActiveFight(FightSession session, FishingHook hook, InteractionHand hand) {
    }

    private static final Map<UUID, ActiveFight> FIGHTS = new HashMap<>();

    private FightManager() {
    }

    public static boolean isFighting(Player player) {
        return FIGHTS.containsKey(player.getUUID());
    }

    public static void start(ServerPlayer player, FishingHook hook, InteractionHand hand, FishProfile profile) {
        FIGHTS.put(player.getUUID(), new ActiveFight(new FightSession(profile, new Random()), hook, hand));
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new FightStartPacket(profile.stamina()));
    }

    public static void handleInput(ServerPlayer player, byte action) {
        ActiveFight fight = FIGHTS.get(player.getUUID());
        if (fight == null) {
            return;
        }
        switch (action) {
            case ReelInputPacket.REEL -> fight.session().setReeling(true);
            case ReelInputPacket.RELEASE -> fight.session().setReeling(false);
            case ReelInputPacket.ABORT -> end(player, fight, FightSession.Phase.FAILED);
            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        ActiveFight fight = FIGHTS.get(player.getUUID());
        if (fight == null) {
            return;
        }
        if (!stillValid(player, fight)) {
            end(player, fight, FightSession.Phase.FAILED);
            return;
        }
        FightSession session = fight.session();
        session.tick();
        if (session.phase() != FightSession.Phase.ACTIVE) {
            end(player, fight, session.phase());
            return;
        }
        sendState(player, session, FightSession.Phase.ACTIVE);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        FIGHTS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        FIGHTS.clear();
    }

    private static boolean stillValid(ServerPlayer player, ActiveFight fight) {
        return player.isAlive()
                && !fight.hook().isRemoved()
                && player.fishing == fight.hook()
                && player.getItemInHand(fight.hand()).getItem() instanceof FishingRodItem;
    }

    private static void end(ServerPlayer player, ActiveFight fight, FightSession.Phase phase) {
        FIGHTS.remove(player.getUUID());
        sendState(player, fight.session(), phase);
        if (phase == FightSession.Phase.SUCCESS) {
            CatchResolver.success(player, fight.hook(), fight.hand());
        } else {
            CatchResolver.failure(player, fight.hook());
        }
    }

    private static void sendState(ServerPlayer player, FightSession s, FightSession.Phase phase) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new FightStatePacket(s.tension(), s.stamina(), s.telegraphing(), s.dashing(), phase));
    }
}
```

- [ ] **Step 7: Compile**

Run:
```bash
export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16"
cd /f/mod/FishingGUImod && ./gradlew compileJava test
```
Expected: `BUILD SUCCESSFUL`. Common fix-ups if it does not compile: some `SoundEvents.*` / `GameEvent.*` constants are `Holder` or `GameEvent` typed differently in 1.20.1 (use `.value()` / the 1.20.1 type that the compiler asks for); `LootContextParams.KILLER_ENTITY`/`getLootData()` exact names must match 1.20.1 (read the decompiled `FishingHook.retrieve` in the Gradle-cached mapped sources and mirror it).

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat: add network packets, FightManager and CatchResolver"
```

---

### Task 4: Fishing hook interception

**Files:**
- Create: `src/main/java/com/example/fishinggui/hook/FishingHookHandler.java`

**Interfaces:**
- Consumes: `FightManager.isFighting`, `FightManager.start`, `FishProfile.DEFAULT` (Tasks 2-3); AT-exposed `FishingHook.nibble` (Task 1).

- [ ] **Step 1: Write the handler**

`src/main/java/com/example/fishinggui/hook/FishingHookHandler.java`:
```java
package com.example.fishinggui.hook;

import com.example.fishinggui.FishingGuiMod;
import com.example.fishinggui.fight.FightManager;
import com.example.fishinggui.fight.FishProfile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Turns the right-click that would instantly reel in a biting fish into the start of a tension fight. */
@Mod.EventBusSubscriber(modid = FishingGuiMod.MOD_ID)
public final class FishingHookHandler {
    private FishingHookHandler() {
    }

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!(event.getItemStack().getItem() instanceof FishingRodItem)) {
            return;
        }
        if (FightManager.isFighting(player)) {
            consume(event);
            return;
        }
        FishingHook hook = player.fishing;
        if (hook == null || hook.nibble <= 0) {
            return;
        }
        consume(event);
        FightManager.start(player, hook, event.getHand(), FishProfile.DEFAULT);
    }

    private static void consume(PlayerInteractEvent.RightClickItem event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
```

- [ ] **Step 2: Compile and run unit tests**

Run: `./gradlew compileJava test` (with `JAVA_HOME` set).
Expected: `BUILD SUCCESSFUL`. A "nibble has private access" error means the AT from Task 1 is wrong or not picked up: fix Task 1 Step 9, then run `./gradlew --refresh-dependencies compileJava` once.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat: start a fight on the bite right-click"
```

---

### Task 5: Client GUI

**Files:**
- Create: `src/main/java/com/example/fishinggui/client/FightScreen.java`
- Modify: `src/main/java/com/example/fishinggui/client/ClientFightHooks.java`

**Interfaces:**
- Consumes: `ModNetwork.CHANNEL`, `ReelInputPacket` constants (Task 3); `FightSession.MAX_TENSION`, `FightSession.DANGER_TENSION`, `FightSession.Phase` (Task 2).
- Produces: `FightScreen(float maxStamina)` with `void apply(float tension, float stamina, boolean telegraph, boolean dashing, FightSession.Phase phase)`.

- [ ] **Step 1: Write the screen**

`src/main/java/com/example/fishinggui/client/FightScreen.java`:
```java
package com.example.fishinggui.client;

import com.example.fishinggui.fight.FightSession;
import com.example.fishinggui.net.ModNetwork;
import com.example.fishinggui.net.ReelInputPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

/** Non-pausing tension gauge. Hold the right mouse button to reel. */
public class FightScreen extends Screen {
    private static final int KEEP_ALIVE_TICKS = 10;
    private static final int GAUGE_W = 16;
    private static final int GAUGE_H = 120;
    private static final int STAMINA_W = 100;

    private final float maxStamina;
    private float tension;
    private float stamina;
    private boolean telegraph;
    private boolean dashing;
    private boolean wasTelegraph;
    private boolean reeling;
    private boolean finished;
    private int tickCount;
    private int sinceSend;

    public FightScreen(float maxStamina) {
        super(Component.literal("Fishing"));
        this.maxStamina = maxStamina;
        this.stamina = maxStamina;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        tickCount++;
        long window = Minecraft.getInstance().getWindow().getWindow();
        boolean down = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (down != reeling || ++sinceSend >= KEEP_ALIVE_TICKS) {
            reeling = down;
            sinceSend = 0;
            ModNetwork.CHANNEL.sendToServer(new ReelInputPacket(down ? ReelInputPacket.REEL : ReelInputPacket.RELEASE));
        }
        if (tension >= FightSession.DANGER_TENSION && tickCount % 10 == 0) {
            playUi(SoundEvents.DISPENSER_FAIL, 1.5f);
        }
    }

    /** Applies a server state; any phase other than ACTIVE closes the screen. */
    public void apply(float tension, float stamina, boolean telegraph, boolean dashing, FightSession.Phase phase) {
        this.tension = tension;
        this.stamina = stamina;
        this.telegraph = telegraph;
        this.dashing = dashing;
        if (telegraph && !wasTelegraph) {
            playUi(SoundEvents.FISHING_BOBBER_SPLASH, 1.0f);
        }
        wasTelegraph = telegraph;
        if (phase != FightSession.Phase.ACTIVE) {
            finished = true;
            onClose();
        }
    }

    @Override
    public void removed() {
        if (!finished) {
            finished = true;
            ModNetwork.CHANNEL.sendToServer(new ReelInputPacket(ReelInputPacket.ABORT));
        }
        ClientFightHooks.clear(this);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int shake = telegraph ? ((tickCount & 1) == 0 ? 2 : -2) : 0;
        int x = cx - GAUGE_W / 2 + shake;
        int top = cy - GAUGE_H / 2;
        int bottom = top + GAUGE_H;

        g.fill(x - 2, top - 2, x + GAUGE_W + 2, bottom + 2, 0xCC000000);
        int dangerTop = bottom - (int) (GAUGE_H * FightSession.DANGER_TENSION / FightSession.MAX_TENSION);
        g.fill(x, top, x + GAUGE_W, dangerTop, 0x66AA2222);
        int filled = (int) (GAUGE_H * tension / FightSession.MAX_TENSION);
        int color = tension >= FightSession.DANGER_TENSION ? 0xFFFF4444 : (dashing ? 0xFFFFAA00 : 0xFF44CC44);
        g.fill(x, bottom - filled, x + GAUGE_W, bottom, color);
        g.fill(x - 3, dangerTop, x + GAUGE_W + 3, dangerTop + 1, 0xFFFF8888);

        int barX = cx - STAMINA_W / 2;
        int barY = bottom + 10;
        g.fill(barX - 1, barY - 1, barX + STAMINA_W + 1, barY + 7, 0xCC000000);
        int staminaW = (int) (STAMINA_W * Math.max(0f, stamina) / maxStamina);
        g.fill(barX, barY, barX + staminaW, barY + 6, 0xFF4488FF);

        g.drawCenteredString(this.font, Component.literal((int) tension + " / 100"), cx, top - 14, 0xFFFFFF);
        if (telegraph) {
            g.drawCenteredString(this.font, Component.literal("!"), cx + GAUGE_W + 12, cy, 0xFFFF55);
        }
        g.drawCenteredString(this.font, Component.literal("Hold right-click to reel"), cx, barY + 12, 0xAAAAAA);
    }

    private static void playUi(net.minecraft.sounds.SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
    }
}
```

- [ ] **Step 2: Fill in `ClientFightHooks`**

Replace `src/main/java/com/example/fishinggui/client/ClientFightHooks.java`:
```java
package com.example.fishinggui.client;

import com.example.fishinggui.fight.FightSession;
import net.minecraft.client.Minecraft;

/** Client entry points called by the S2C packets. */
public final class ClientFightHooks {
    private static FightScreen screen;

    private ClientFightHooks() {
    }

    public static void onStart(float maxStamina) {
        screen = new FightScreen(maxStamina);
        Minecraft.getInstance().setScreen(screen);
    }

    public static void onState(float tension, float stamina, boolean telegraph, boolean dashing,
                               FightSession.Phase phase) {
        if (screen != null) {
            screen.apply(tension, stamina, telegraph, dashing, phase);
        }
    }

    static void clear(FightScreen closed) {
        if (screen == closed) {
            screen = null;
        }
    }
}
```

- [ ] **Step 3: Compile**

Run: `./gradlew compileJava test` (with `JAVA_HOME` set). Expected: `BUILD SUCCESSFUL`. If `SoundEvents.*` constants are `Holder`s in 1.20.1, append `.get()` / `.value()` as the compiler requires.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: add FightScreen tension gauge"
```

---

### Task 6: Manual smoke test and spec reconciliation

**Files:**
- Modify: `docs/superpowers/specs/2026-11-02-fishing-fight-design.md`

- [ ] **Step 1: Run the client**

Run: `./gradlew runClient` (with `JAVA_HOME` set). In a creative world: `/give @s fishing_rod`, stand at water, cast.

- [ ] **Step 2: Walk through this checklist and record each result (pass/fail)**

1. Right-click with the rod while nothing is biting: vanilla behaviour (cast / reel in).
2. After the bite splash, right-click: the GUI opens, the bobber stays in the water.
3. Hold right-click: tension rises, stamina bar falls. Release: tension falls, stamina recovers slowly.
4. A dash gives a shake + `!` + splash sound ~0.5 s before tension surges.
5. Holding until the gauge is full: line cuts, GUI closes, bobber disappears, no loot.
6. Pulsing successfully: GUI closes, loot flies to the player, XP orbs appear, rod loses durability.
7. ESC during a fight: GUI closes, bobber disappears (counts as failure).
8. Switch hotbar slot during a fight: GUI closes, fight ends.

- [ ] **Step 3: Verify on a dedicated server**

Run `./gradlew runServer` once (accept `eula.txt` in `run/`), join it with `runClient` (`localhost`), repeat checklist items 2, 3, 6. Expected: identical behaviour; the server log shows no exceptions.

- [ ] **Step 4: Reconcile the spec with what was built**

Edit `docs/superpowers/specs/2026-11-02-fishing-fight-design.md`:
- §6 table, `net` row: the `ReelInput` packet carries a one-byte action (`RELEASE`/`REEL`/`ABORT`) and is also resent as a keep-alive every 10 ticks; there are still 3 packets. `FightState` carries a phase, and a non-ACTIVE phase closes the GUI.
- §7: the server releases the reel after 40 ticks without input and accepts at most 2 inputs per tick.
- §3: default `FishProfile` is `stamina 100, dashInterval 70 ticks (avg), dashPower 1.0, tensionGain 1.0`.
- §8: GameTest is not part of Phase 1 (unit tests plus the manual checklist above); revisit in Phase 2.
- Add a "Known limitations" note: the client also plays the vanilla retrieve sound on the bite right-click because it cannot see the server-only `nibble` value.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "docs: record smoke test results and reconcile spec with Phase 1"
```

---

## Self-Review

**Spec coverage (Phase 1 scope):** flow §2 (cast/bite/fight/finish/abort) -> Tasks 3-5; tension model §3 -> Task 2; GUI §4 -> Task 5; technical §6 (FightSession, FightManager, HookHandler, CatchResolver, packets, FightScreen) -> Tasks 2-5; server checks §7 -> Task 2 (rate limit, silence) and Task 3 (`stillValid`); tests §8 -> Task 2 + Task 6 (GameTest deferred, recorded in Task 6). Baits (§5), datapack `FishProfileLoader`, failure penalty and custom fish are Phase 2/3 by design.

**Placeholders:** the only value not fixed in the plan is the SRG number in Task 1 Step 9, which has an explicit procedure and a compile-based check (Step 10).

**Type consistency:** `FightSession.Phase`, `FishProfile.DEFAULT`, `ReelInputPacket.{RELEASE,REEL,ABORT}`, `FightManager.{isFighting,start,handleInput}`, `CatchResolver.{success,failure}`, `ClientFightHooks.{onStart,onState,clear}` and `FightScreen.apply` signatures match across Tasks 2-5.
