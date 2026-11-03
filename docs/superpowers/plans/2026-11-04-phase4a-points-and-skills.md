# Phase 4-A ポイント変換とスキル Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 釣果画面の第3の選択肢「ポイントに変換」、ポイントとスキルの保存、Jキーのスキル画面、スキル「食いつき短縮」(最大Lv5)を作る。

**Architecture:** 純 Java の核(`PointRules`、`SkillState`、`SkillDef`、`SkillParser`、`SkillEffects`)を単体テストし、サーバー側(`SkillLoader`、`SkillData`、`SkillService`、パケット2種)とクライアント側(`ClientSkills`、`SkillScreen`、Jキー)を被せる。サーバーが全てを判定する。

**Tech Stack:** Minecraft 1.20.1 / Forge 47.3.0 / ForgeGradle 6 / 公式マッピング / Java 17 / JUnit 5 / Gson。

設計書: `docs/superpowers/specs/2026-11-04-phase4a-points-and-skills-design.md`(必ず先に読む)。

## Global Constraints

- パッケージは `com.example.fishinggui`、Java は `src/main/java/com/example/fishinggui/`、テストは `src/test/java/com/example/fishinggui/`。
- ビルド: `export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16"`(既定の java は JRE 8 で失敗する)してから `./gradlew compileJava test build`。
- python3 は無い。長いファイルは Write ツールで作る(巨大な bash heredoc は失敗しやすい)。
- ポイント: common=1、uncommon=2、rare=3、epic=4。重量は使わない。釣り経験値は入らない。
- スキルのレベルは保存する(4-C のリセットで返すポイントを計算するため)。
- 食いつき短縮: レベル L で `0.1 × L`(Lv5 = 0.5)。コスト `[1,2,3,4,5]`。
- 既存の保存先は `Player.PERSISTED_NBT_TAG` → `fishinggui`。**`FishingData.set` は現在このタグを丸ごと作り直すので、`points`/`skills` が消える。Task 2 で必ず直す。**
- ネットワークは `ModNetwork` に追加登録(既存の最後は `FishingProgressPacket`)。`consumerMainThread`、クライアント側呼び出しは `DistExecutor.unsafeRunWhenOn`(既存パケットと同じ形)。
- コミットメッセージの末尾: `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`。Git の LF/CRLF 警告は無視してよい。

---

### Task 1: 純 Java の核(ポイント式、スキル状態、定義、パーサー、効果)

**Files:**
- Create: `skill/PointRules.java`, `skill/SkillState.java`, `skill/SkillDef.java`, `skill/SkillEffects.java`, `data/SkillParser.java`
- Test: `skill/PointRulesTest.java`, `skill/SkillStateTest.java`, `skill/SkillEffectsTest.java`, `data/SkillParserTest.java`

**Interfaces:**
- Produces:
  - `PointRules.pointsFor(Rarity)`: int。`Rarity` は既存の `com.example.fishinggui.fight.Rarity`(`COMMON, UNCOMMON, RARE, EPIC` の順。ordinal を使わず switch で書く)。
  - `SkillDef(String id, int maxLevel, List<Integer> cost)`: `int nextCost(int level)`(最大なら -1)。
  - `SkillState(int points, Map<String,Integer> skills)`: `START`、`addPoints(int)`、`levelOf(String)`、`canBuy(SkillDef)`、`buy(SkillDef)`(買えなければ `IllegalStateException`)、`withPoints(int)`。値は不変(コピーして返す)。`points` は 0 以上。
  - `SkillParser.parse(String id, JsonObject)`: `SkillDef`。不正なら `IllegalArgumentException`。
  - `SkillEffects.BITE_SPEED = "bite_speed"`、`SkillEffects.biteSpeedBonus(int level)`: double(`min(5,max(0,level)) × 0.1`)。

- [ ] **Step 1: テストを書く**(下は必須ケース。各テストクラスに入れる)
  - `PointRulesTest`: COMMON→1、UNCOMMON→2、RARE→3、EPIC→4。
  - `SkillStateTest`: `START` はポイント0・スキルなし。`addPoints(3)` で3。`buy` はコストを引いてレベル+1(コスト `[1,2,3]` の def、ポイント10 でレベル0→1→2、残り 10-1-2=7)。ポイント不足は `canBuy` false で `buy` が `IllegalStateException`。最大レベルでも同じ。元の状態は変わらない。
  - `SkillEffectsTest`: レベル0→0.0、3→0.3(誤差 1e-9)、5→0.5、9→0.5(上限)、-1→0.0。
  - `SkillParserTest`: 正常(`{"max_level":3,"cost":[1,2,3]}`)、`cost` の長さが `max_level` と違う、`cost` に 0 以下、`max_level` が無い・0 以下、`cost` が無い。
- [ ] **Step 2: テストを実行して失敗を確認**(`./gradlew test`、コンパイルエラーでよい)
- [ ] **Step 3: 実装**(`SkillParser` は `FishProfileParser` と同じ流儀、`JsonObject` から `getAsInt`/`getAsJsonArray`。必須項目が無いときは `IllegalArgumentException("missing max_level")` のように分かるメッセージを付ける)
- [ ] **Step 4: `./gradlew compileJava test` で全て成功を確認**
- [ ] **Step 5: コミット** `feat: points and skill core with tests`

---

### Task 2: サーバー側のスキルとポイント(保存、同期、取得、コマンド、データ)

**Files:**
- Create: `data/SkillLoader.java`, `skill/SkillData.java`, `skill/SkillService.java`, `net/SkillSyncPacket.java`, `net/SkillBuyPacket.java`, `src/main/resources/data/fishinggui/fishinggui/skills/bite_speed.json`
- Modify: `level/FishingData.java`, `net/ModNetwork.java`, `command/FishingCommand.java`, `src/main/resources/assets/fishinggui/lang/ja_jp.json` と `en_us.json`
- Test: `skill/SkillDataTest.java`(NBT を使うので、Minecraft クラスが単体テストで読めない場合は省略してよい。その場合は理由を報告に書く)

**Interfaces:**
- Consumes: Task 1 の型。
- Produces:
  - `SkillLoader.skills()`: `Map<String, SkillDef>`(読み込み順は ID の辞書順を保つ `LinkedHashMap`、`volatile` な不変コピー)。`FishProfileLoader` と同じ `SimpleJsonResourceReloadListener`、フォルダ `fishinggui/skills`、`AddReloadListenerEvent` で登録。ID はファイル名(`ResourceLocation.getPath()`)。不正ファイルは警告ログで読み飛ばす。
  - `SkillData.get(Player)`: `SkillState`、`SkillData.set(Player, SkillState)`。`fishinggui` タグ内に `points`(int)と `skills`(CompoundTag、ID→int)を書く。**既存の `level`/`xp` を消さない。**
  - `SkillService.addPoints(ServerPlayer, int)`(加算して同期)、`SkillService.buy(ServerPlayer, String skillId)`(検証して購入、同期)、`SkillService.sync(ServerPlayer)`。ログインとリスポーンで `sync`(`@Mod.EventBusSubscriber` + `PlayerLoggedInEvent`/`PlayerRespawnEvent`)。
  - `SkillSyncPacket(int points, List<Entry> skills)`、`Entry(String id, int maxLevel, List<Integer> cost, int level)`。S2C。
  - `SkillBuyPacket(String id)`。C2S。`ModNetwork` の末尾に登録。

- [ ] **Step 1: `FishingData.set` を直す** — 新しい `CompoundTag` を作らず、`root.getCompound(KEY)` を取り出して `level`/`xp` だけを上書きして `root.put(KEY, tag)`。同様に `SkillData.set` も既存タグを保つ。
- [ ] **Step 2: `bite_speed.json`** `{"max_level":5,"cost":[1,2,3,4,5]}`
- [ ] **Step 3: `SkillLoader`、`SkillData`、`SkillService`、パケット2つ、`ModNetwork` 登録を実装**
  - `buy` は: 定義が無い→何もしない。`SkillState.canBuy` が false→何もしない。それ以外は購入して保存して同期。
  - `SkillBuyPacket.handle` は `ctx.get().getSender()` が null でなければ `SkillService.buy`。ID が長すぎる悪用を避けるため `buf.readUtf(64)`。
- [ ] **Step 4: コマンド**: `/fishinggui points set <players> <int>`(0以上)、`points add <players> <int>`(負の値も可、結果は 0 未満にならない)。`query` の出力にポイントを足す(`commands.fishinggui.query` の書式に「ポイント %s」を足す: ja `"%s: 釣りLv.%s（経験値 %s/%s）ポイント %s"`、en 同様)。`FishingCommand` の既存 `change` ヘルパは `FishingProgress` 用なので、ポイント用に別メソッドを作る。
- [ ] **Step 5: 言語ファイル**(ja/en 両方、キー名は固定):
  - `skill.fishinggui.bite_speed` = ja `食いつき短縮` / en `Quick Bite`
  - `skill.fishinggui.bite_speed.desc` = ja `魚が食いつくまでの時間を短縮する（1レベルにつき10%）` / en `Shortens the wait for a bite (10% per level)`
  - `gui.fishinggui.points.gained` = ja `ポイント +%s（合計 %s）` / en `Points +%s (total %s)`
  - `commands.fishinggui.updated` は既存のものを使う。
  - JSON の末尾カンマ・重複カンマに注意(壊れると全文字列が出なくなる)。
- [ ] **Step 6: `./gradlew compileJava test build` が成功すること。専用サーバー起動の煙テストで「Loaded N skills」のログ(ログ文言は `Loaded {} skills`)を確認**(`SkillLoader` が出す)。
- [ ] **Step 7: コミット** `feat: points, skill storage, sync, buy and commands`

---

### Task 3: ポイントに変換(釣果画面の第3ボタン)

**Files:**
- Modify: `net/CatchChoicePacket.java`, `hook/PendingCatchManager.java`, `client/CatchScreen.java`, `lang/ja_jp.json`, `lang/en_us.json`
- Test: 既存の単体テストが通ること(新規テストは `PointRulesTest` で済み)

**Interfaces:**
- Consumes: Task 1 の `PointRules`、Task 2 の `SkillService.addPoints`。
- Produces: `CatchChoicePacket(byte choice)` と定数 `KEEP = 0`、`RELEASE = 1`、`CONVERT = 2`。`PendingCatchManager.choose(ServerPlayer, int choice)`。

- [ ] **Step 1: `CatchChoicePacket`** を `byte` 1 つにする(`encode`: `writeByte`、`decode`: `readByte`)。範囲外の値は `handle` で KEEP として扱う。
- [ ] **Step 2: `PendingCatchManager`**: `Pending` に `Rarity rarity` を足す(`open` で `info.rarity()` を保存)。`choose(player, choice)`:
  - KEEP: 従来どおり `CatchResolver.deliver(..., true)`。
  - RELEASE: 従来どおり `FishingService.award(player, xp)`。
  - CONVERT: **経験値は入れない**。`SkillService.addPoints(player, PointRules.pointsFor(rarity))` を呼び、チャットに `gui.fishinggui.points.gained`(増えた量と合計)を出す(`player.sendSystemMessage`)。
  - ログアウト時とサーバー内の旧 Pending の押し出しは KEEP のまま(`choose(player, KEEP)`)。
- [ ] **Step 3: `CatchScreen`**: ボタンを3つにする。幅 100、`y = height - 44`、x は `cx-155`、`cx-50`、`cx+55`(順に キープ、リリース、変換)。変換ボタンの文言は `Component.translatable("gui.fishinggui.catch.convert", PointRules.pointsFor(rarity))`。`choose(boolean)` を `choose(byte)` にし、ESC と画面を閉じたときは KEEP を送る(既存の `removed()` を同様に直す)。
- [ ] **Step 4: 言語**: `gui.fishinggui.catch.convert` = ja `ポイントに変換 (+%s)` / en `Convert (+%s pt)`。
- [ ] **Step 5: `./gradlew compileJava test build` 成功**
- [ ] **Step 6: コミット** `feat: convert catch to points on the catch screen`

---

### Task 4: スキル画面(Jキー)

**Files:**
- Create: `client/ClientSkills.java`, `client/SkillScreen.java`, `client/SkillKey.java`
- Modify: `net/SkillSyncPacket.java`(ハンドラからクライアントへ渡す), `client/ClientEvents.java`(キー登録), 言語ファイル

**Interfaces:**
- Consumes: Task 2 の `SkillSyncPacket`、`SkillBuyPacket`、`ModNetwork.CHANNEL`。
- Produces: `ClientSkills.apply(int points, List<SkillSyncPacket.Entry> skills)`、`ClientSkills.points()`、`ClientSkills.skills()`。

- [ ] **Step 1: `ClientSkills`**: 静的に points とスキル一覧を持つ。`apply` で更新し、開いている画面が `SkillScreen` なら `rebuildWidgets()` を呼ぶ。`SkillSyncPacket.handle` から `DistExecutor.unsafeRunWhenOn` で呼ぶ。
- [ ] **Step 2: `SkillScreen`**(`Screen` を継承、`isPauseScreen` は false):
  - 上部に「スキル」の見出しと「所持ポイント: N」。
  - スキルごとに 1 行(縦に積む。行の高さ 36): 名前(`skill.fishinggui.<id>`)、説明(`.desc`、灰色)、`Lv.現在/最大`、右端に「取得（コスト N）」ボタン。最大レベルなら「MAX」を出してボタンは無効。ポイント不足ならボタンは無効(`active = false`)。
  - ボタンを押したら `ModNetwork.CHANNEL.sendToServer(new SkillBuyPacket(id))`。画面の更新はサーバーの同期で行う(クライアントで先に引かない)。
  - `init()` でボタンを作り直す形にして、`rebuildWidgets()` で再描画できるようにする。
  - ESC か J で閉じる(`keyPressed` で J キーを検出して `onClose`)。
- [ ] **Step 3: `SkillKey`**: `KeyMapping("key.fishinggui.skills", GLFW.GLFW_KEY_J, "key.categories.fishinggui")`。`RegisterKeyMappingsEvent`(mod バス、`ClientEvents` か同じ形で登録)で登録。`ClientTickEvent`(Forge バス、`Dist.CLIENT`)で `consumeClick()` が true かつ `mc.screen == null` かつ `mc.player != null` のとき `new SkillScreen()` を開く。
- [ ] **Step 4: 言語**: `key.fishinggui.skills` = ja `スキル画面を開く` / en `Open Skills`、`key.categories.fishinggui` = ja `釣りGUI` / en `Fishing GUI`、`gui.fishinggui.skills.title` = ja `スキル` / en `Skills`、`gui.fishinggui.skills.points` = ja `所持ポイント: %s` / en `Points: %s`、`gui.fishinggui.skills.buy` = ja `取得（%s）` / en `Learn (%s)`、`gui.fishinggui.skills.max` = `MAX`。
- [ ] **Step 5: `./gradlew compileJava test build` 成功**
- [ ] **Step 6: コミット** `feat: skill screen with J key`

---

### Task 5: 食いつき短縮の効果

**Files:**
- Create: `hook/BiteSpeedHandler.java`
- Modify: `src/main/resources/META-INF/accesstransformer.cfg`(`FishingHook` の `timeUntilLured` と `timeUntilHooked` を public にする。既存の `nibble` の行(`f_37089_`)の形に合わせる。**SRG 名は推測せず、Gradle キャッシュのマッピング(例: `~/.gradle` 配下の `net.minecraftforge` の `mcp_config` や `createMcpToSrg` の出力)で `timeUntilLured`、`timeUntilHooked` の `f_` 名を確認する**)

**Interfaces:**
- Consumes: Task 1 の `SkillEffects.biteSpeedBonus`、Task 2 の `SkillData.get(player).levelOf(SkillEffects.BITE_SPEED)`、既存の `FightManager.isFighting(player)`。

- [ ] **Step 1: AT を追加し `./gradlew compileJava` が通ることを確認**
- [ ] **Step 2: `BiteSpeedHandler`**: `TickEvent.PlayerTickEvent`(END、`ServerPlayer`、`@Mod.EventBusSubscriber`)。`player.fishing != null`、ファイト中でない、`hook.nibble == 0`(待機中)、釣りレベルのスキル `bonus > 0` のとき、`player.getRandom().nextDouble() < bonus` なら、`timeUntilLured > 0` ならそれを 1 減らし、そうでなく `timeUntilHooked > 0` ならそれを 1 減らす。
- [ ] **Step 3: `./gradlew compileJava test build` 成功**
- [ ] **Step 4: 専用サーバー起動の煙テスト(エラーなしで起動すること)**
- [ ] **Step 5: コミット** `feat: bite speed skill`

---

## Self-Review メモ

- 設計書の 1(変換)= Task 3、2(保存)= Task 1・2、3(データ)= Task 1・2、4(画面)= Task 4、5(コマンド)= Task 2、食いつき短縮 = Task 5。
- 型名の一貫性: `SkillState.buy/canBuy/levelOf`、`SkillService.addPoints/buy/sync`、`CatchChoicePacket.KEEP/RELEASE/CONVERT`、`SkillSyncPacket.Entry(id, maxLevel, cost, level)`。
