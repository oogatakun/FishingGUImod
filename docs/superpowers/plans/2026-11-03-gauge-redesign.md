# Fight GUI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the single vertical gauge in `FightScreen` with a two-bar layout (landing progress + line tension) with localized state labels, as approved by the user from a reference screenshot.

**Architecture:** A pure-Java `GaugeState` computes progress and label states (unit-tested). `FightScreen.render` draws the icon, two bars and six label pills from it. Strings come from lang files.

**Tech Stack:** Minecraft 1.20.1, Forge 47.3.0, Java 17, official mappings, JUnit 5. Client-only code stays under `com.example.fishinggui.client`.

## Global Constraints

- Every Gradle command runs with JDK 17: `export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16"`.
- `GaugeState` imports nothing from `net.minecraft` / `net.minecraftforge`.
- Layout (both bars grow from bottom to top):
  - Left bar = landing progress `1 - stamina/maxStamina` (white). Labels top→bottom: "間もなく釣り上げます" (ALMOST), "魚とバトル中!" (BATTLE), "釣り始め" (START).
  - Right bar = tension (green→yellow→red gradient, tip marker). Labels top→bottom: "糸が切れるかも!" (DANGER), "安全な張り具合" (SAFE, with ◀▶ prefix), "糸の張りが弱い" (SLACK).
- Thresholds: progress < 0.2 → START, >= 0.8 → ALMOST, else BATTLE. Tension < 25 → SLACK, >= `FightSession.DANGER_TENSION` (70) → DANGER, else SAFE.
- Active label: white text on `0xCC000000` pill; inactive: `0x777777` text on `0x88000000` pill.
- Keep: telegraph shake (±2 px alternating by tick), "!" telegraph mark, dash tint (orange `0xFFFFAA00` marker), danger warning sound, input handling and keep-alive logic in `FightScreen` (do not touch `tick`, `apply`, `removed`, `markFinished`).
- Remove the numeric "N / 100" text. Keep a small gray hint line under the bars.
- Strings via `Component.translatable`; add `ja_jp.json` and extend `en_us.json`.

## File Structure

- Create `src/main/java/com/example/fishinggui/client/GaugeState.java`
- Create `src/test/java/com/example/fishinggui/client/GaugeStateTest.java`
- Modify `src/main/java/com/example/fishinggui/client/FightScreen.java` (constants + `render` + helpers only)
- Create `src/main/resources/assets/fishinggui/lang/ja_jp.json`; modify `en_us.json`

---

### Task 1: GaugeState (pure Java)

**Files:**
- Create: `src/main/java/com/example/fishinggui/client/GaugeState.java`
- Test: `src/test/java/com/example/fishinggui/client/GaugeStateTest.java`

**Interfaces:**
- Produces: `GaugeState.progress(float stamina, float maxStamina) -> float` (0..1, 0 if max <= 0); `GaugeState.progressLabel(float progress) -> Progress`; `GaugeState.lineLabel(float tension, float danger) -> Line`; enums `Progress {START, BATTLE, ALMOST}`, `Line {SLACK, SAFE, DANGER}`.

- [ ] **Step 1: Write the failing test**

```java
package com.example.fishinggui.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GaugeStateTest {
    @Test
    void progressIsInverseOfStaminaAndClamped() {
        assertEquals(0f, GaugeState.progress(100f, 100f), 1e-6f);
        assertEquals(0.5f, GaugeState.progress(50f, 100f), 1e-6f);
        assertEquals(1f, GaugeState.progress(0f, 100f), 1e-6f);
        assertEquals(1f, GaugeState.progress(-5f, 100f), 1e-6f);
        assertEquals(0f, GaugeState.progress(120f, 100f), 1e-6f);
        assertEquals(0f, GaugeState.progress(10f, 0f), 1e-6f);
    }

    @Test
    void progressLabelBoundaries() {
        assertEquals(GaugeState.Progress.START, GaugeState.progressLabel(0f));
        assertEquals(GaugeState.Progress.START, GaugeState.progressLabel(0.19f));
        assertEquals(GaugeState.Progress.BATTLE, GaugeState.progressLabel(0.2f));
        assertEquals(GaugeState.Progress.BATTLE, GaugeState.progressLabel(0.79f));
        assertEquals(GaugeState.Progress.ALMOST, GaugeState.progressLabel(0.8f));
        assertEquals(GaugeState.Progress.ALMOST, GaugeState.progressLabel(1f));
    }

    @Test
    void lineLabelBoundaries() {
        assertEquals(GaugeState.Line.SLACK, GaugeState.lineLabel(0f, 70f));
        assertEquals(GaugeState.Line.SLACK, GaugeState.lineLabel(24.9f, 70f));
        assertEquals(GaugeState.Line.SAFE, GaugeState.lineLabel(25f, 70f));
        assertEquals(GaugeState.Line.SAFE, GaugeState.lineLabel(69.9f, 70f));
        assertEquals(GaugeState.Line.DANGER, GaugeState.lineLabel(70f, 70f));
        assertEquals(GaugeState.Line.DANGER, GaugeState.lineLabel(100f, 70f));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16" && ./gradlew test --tests '*GaugeStateTest'`
Expected: compilation FAIL (`GaugeState` not found).

- [ ] **Step 3: Write minimal implementation**

```java
package com.example.fishinggui.client;

/** Pure-Java mapping from fight values to gauge fractions and label states. */
public final class GaugeState {
    public enum Progress { START, BATTLE, ALMOST }

    public enum Line { SLACK, SAFE, DANGER }

    static final float START_BELOW = 0.2f;
    static final float ALMOST_FROM = 0.8f;
    static final float SLACK_BELOW = 25f;

    private GaugeState() {
    }

    public static float progress(float stamina, float maxStamina) {
        if (maxStamina <= 0f) {
            return 0f;
        }
        return Math.max(0f, Math.min(1f, 1f - stamina / maxStamina));
    }

    public static Progress progressLabel(float progress) {
        if (progress < START_BELOW) {
            return Progress.START;
        }
        return progress >= ALMOST_FROM ? Progress.ALMOST : Progress.BATTLE;
    }

    public static Line lineLabel(float tension, float danger) {
        if (tension < SLACK_BELOW) {
            return Line.SLACK;
        }
        return tension >= danger ? Line.DANGER : Line.SAFE;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16" && ./gradlew test`
Expected: PASS (all tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/fishinggui/client/GaugeState.java src/test/java/com/example/fishinggui/client/GaugeStateTest.java
git commit -m "feat: add GaugeState for the redesigned fight gauge"
```

---

### Task 2: Redesigned FightScreen and lang files

**Files:**
- Modify: `src/main/java/com/example/fishinggui/client/FightScreen.java`
- Create: `src/main/resources/assets/fishinggui/lang/ja_jp.json`; modify `en_us.json`

**Interfaces:**
- Consumes: `GaugeState` (Task 1), `FightSession.DANGER_TENSION`, `FightSession.MAX_TENSION`.

- [ ] **Step 1: Lang files**

Add to `en_us.json` (keep existing item keys):

```json
  "gui.fishinggui.progress.start": "Casting",
  "gui.fishinggui.progress.battle": "Fighting!",
  "gui.fishinggui.progress.almost": "Landing soon",
  "gui.fishinggui.line.slack": "Line is slack",
  "gui.fishinggui.line.safe": "Safe tension",
  "gui.fishinggui.line.danger": "Line may snap!",
  "gui.fishinggui.hint": "Hold right-click to reel"
```

Create `ja_jp.json` with the item keys translated (`"item.fishinggui.worm": "ミミズ"`, `"item.fishinggui.glow_bug": "光る虫"`, `"item.fishinggui.sturdy_paste": "丈夫な練りエサ"`) plus:

```json
  "gui.fishinggui.progress.start": "釣り始め",
  "gui.fishinggui.progress.battle": "魚とバトル中!",
  "gui.fishinggui.progress.almost": "間もなく釣り上げます",
  "gui.fishinggui.line.slack": "糸の張りが弱い",
  "gui.fishinggui.line.safe": "安全な張り具合",
  "gui.fishinggui.line.danger": "糸が切れるかも!",
  "gui.fishinggui.hint": "右クリック長押しで巻く"
```

Both files must be valid JSON (UTF-8, no BOM).

- [ ] **Step 2: FightScreen constants** — replace `GAUGE_W`, `GAUGE_H`, `STAMINA_W` with:

```java
    private static final int BAR_W = 9;
    private static final int BAR_H = 140;
    private static final int BAR_GAP = 6;
    private static final int LABEL_GAP = 6;
    private static final int PILL_H = 14;
    private static final int ICON_R = 17;
```

- [ ] **Step 3: Replace `render` and add helpers**

```java
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int shake = telegraph ? ((tickCount & 1) == 0 ? 2 : -2) : 0;
        int top = cy - BAR_H / 2;
        int bottom = top + BAR_H;
        int leftX = cx - BAR_GAP / 2 - BAR_W + shake;
        int rightX = cx + BAR_GAP / 2 + shake;

        // icon
        int iconCy = top - 8;
        fillCircle(g, cx, iconCy, ICON_R, 0xDD000000);
        g.renderItem(new ItemStack(Items.FISHING_ROD), cx - 8, iconCy - 8);

        // left bar: landing progress
        float progress = GaugeState.progress(stamina, maxStamina);
        drawTrack(g, leftX, top, bottom);
        int progressH = (int) (BAR_H * progress);
        g.fill(leftX, bottom - progressH, leftX + BAR_W, bottom, 0xFFFFFFFF);
        g.fill(leftX - 1, bottom - progressH - 1, leftX + BAR_W + 1, bottom - progressH + 1, 0xFFFFFFFF);

        // right bar: tension
        drawTrack(g, rightX, top, bottom);
        int tensionH = (int) (BAR_H * Math.min(1f, tension / FightSession.MAX_TENSION));
        for (int i = 0; i < tensionH; i++) {
            g.fill(rightX, bottom - i - 1, rightX + BAR_W, bottom - i, tensionColor((float) i / BAR_H));
        }
        int dangerY = bottom - (int) (BAR_H * FightSession.DANGER_TENSION / FightSession.MAX_TENSION);
        g.fill(rightX - 2, dangerY, rightX + BAR_W + 2, dangerY + 1, 0x88FF6666);
        int tipColor = dashing ? 0xFFFFAA00 : 0xFFFFFFFF;
        g.fill(rightX - 2, bottom - tensionH - 1, rightX + BAR_W + 2, bottom - tensionH + 1, tipColor);

        // labels
        GaugeState.Progress p = GaugeState.progressLabel(progress);
        GaugeState.Line l = GaugeState.lineLabel(tension, FightSession.DANGER_TENSION);
        int[] ys = {top + 2, cy - PILL_H / 2, bottom - PILL_H - 2};
        int leftEdge = leftX - LABEL_GAP;
        int rightEdge = rightX + BAR_W + LABEL_GAP;
        drawLabel(g, "gui.fishinggui.progress.almost", leftEdge, ys[0], true, p == GaugeState.Progress.ALMOST);
        drawLabel(g, "gui.fishinggui.progress.battle", leftEdge, ys[1], true, p == GaugeState.Progress.BATTLE);
        drawLabel(g, "gui.fishinggui.progress.start", leftEdge, ys[2], true, p == GaugeState.Progress.START);
        drawLabel(g, "gui.fishinggui.line.danger", rightEdge, ys[0], false, l == GaugeState.Line.DANGER);
        drawLabel(g, "gui.fishinggui.line.safe", rightEdge, ys[1], false, l == GaugeState.Line.SAFE);
        drawLabel(g, "gui.fishinggui.line.slack", rightEdge, ys[2], false, l == GaugeState.Line.SLACK);

        if (telegraph) {
            g.drawCenteredString(this.font, Component.literal("!"), cx, bottom + 4, 0xFFFF55);
        }
        g.drawCenteredString(this.font, Component.translatable("gui.fishinggui.hint"), cx, bottom + 16, 0xAAAAAA);
    }

    private static void drawTrack(GuiGraphics g, int x, int top, int bottom) {
        g.fill(x - 1, top - 1, x + BAR_W + 1, bottom + 1, 0xCC000000);
        g.fill(x, top, x + BAR_W, bottom, 0xFF202020);
    }

    /** Green (low) → yellow → red (high) over the bar height. */
    private static int tensionColor(float f) {
        int r;
        int gr;
        if (f < 0.5f) {
            r = (int) (0x44 + (0xFF - 0x44) * (f / 0.5f));
            gr = 0xCC;
        } else {
            r = 0xFF;
            gr = (int) (0xCC - (0xCC - 0x44) * ((f - 0.5f) / 0.5f));
        }
        return 0xFF000000 | (r << 16) | (gr << 8) | 0x33;
    }

    private void drawLabel(GuiGraphics g, String key, int edge, int y, boolean alignRight, boolean active) {
        Component text = Component.translatable(key);
        String prefix = key.endsWith("line.safe") ? "◀▶ " : "";
        Component shown = prefix.isEmpty() ? text : Component.literal(prefix).append(text);
        int w = this.font.width(shown) + 10;
        int x1 = alignRight ? edge - w : edge;
        fillRounded(g, x1, y, x1 + w, y + PILL_H, active ? 0xCC000000 : 0x88000000);
        g.drawString(this.font, shown, x1 + 5, y + 3, active ? 0xFFFFFF : 0x777777, false);
    }

    private static void fillRounded(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1 + 1, y1, x2 - 1, y2, color);
        g.fill(x1, y1 + 1, x2, y2 - 1, color);
    }

    private static void fillCircle(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.sqrt((double) r * r - (double) dy * dy);
            g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }
```

Add imports `net.minecraft.world.item.ItemStack` and `net.minecraft.world.item.Items`. Do not modify `tick`, `apply`, `removed`, `markFinished`, or the `playUi` helper.

- [ ] **Step 4: Build and test**

Run: `export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16" && ./gradlew compileJava test build`
Expected: BUILD SUCCESSFUL; all tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main
git commit -m "feat: redesign fight GUI with progress and tension bars"
```

- [ ] **Step 6: Manual check (human, `./gradlew runClient`)**

1. Both bars and six labels render, JP labels with a Japanese client language, English otherwise (no tofu squares).
2. Holding right-click: right bar grows with a green→yellow→red gradient; left white bar grows as the fish tires.
3. The active label on each side is white; others dim.
4. Dash telegraph still shakes the bars; "!" appears; the tip turns orange during a dash.
5. The pills do not overlap the bars at GUI scale 2 and 3.
