# Phase 5: 七輪と焼き魚 実装計画

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development. 要件はすべて `docs/superpowers/specs/2026-11-05-phase5-cooking-design.md`。各タスクは、そこの該当節が要件。

**Goal:** 七輪ブロックで魚を焼き、レベルから決まる効果の焼き魚を食べられるようにする。

**Architecture:** 数値と判定は純 Java(`FoodRules`、`CookingRules`)で単体テストし、アイテム登録、ブロック、ブロックエンティティ、レンダラーは Forge 側に薄く載せる。

**Tech Stack:** Minecraft 1.20.1 / Forge 47.3.0 / Java 17 / JUnit 5。パッケージ `com.example.fishinggui`。

## Global Constraints

- 魚の重量は焼き魚の効果に関係しない。
- 画像は、新アイテムはキット(`F:\mc-model-kit-v3.0\mc-model-kit-v3.0\claude-code\mc-model-kit`、CLAUDE.md を読む。`core/` は変更しない)で作り、出力は `fishinggui_items/<名前>/` にまとめる。完成した PNG は MOD の `src/main/resources/assets/fishinggui/textures/` にコピーする。
- ビルド: `export JAVA_HOME="/c/Users/4jskk/.jdks/corretto-17.0.16"`、`./gradlew compileJava test build` が BUILD SUCCESSFUL。`runClient` は実行しない。
- コミットメッセージ末尾: `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`
- 言語ファイル: `assets/fishinggui/lang/ja_jp.json`、`en_us.json`。有効な JSON を保つ。en の `%` は `%%`。

## Task 1: 純 Java(FoodRules、CookingRules、パーサー拡張)

**Files:** 作成 `food/FoodRules.java`、`food/CookingRules.java`(パッケージ `com.example.fishinggui.food`)と、そのテスト。変更 `fight/FishInfo.java`(任意の `food` 上書き)、`data/FishProfileParser.java` とそのテスト。

**Interfaces:**
- Produces: `FoodRules.nutrition(int level)`、`FoodRules.saturationModifier(int level)`、`FoodRules.heal(Rarity rarity)`(半ハート単位)。`FishInfo` から魚ごとの上書きを取れる(`Optional`相当の形で、無ければ自動)。
- Produces: `CookingRules.cookTicks(int level)`、`CookingRules.BURN_TICKS = 400`、`CookingRules.FUEL_TICKS = 1600`、`CookingRules.MAX_FUEL = 4`、`CookingRules.cookedIdFor(String rawId)`(焼けない魚は `null`、オリジナルは `cooked_<id>`、バニラのタラ・サケは `minecraft:cooked_cod` / `minecraft:cooked_salmon`)、`CookingRules.isGrillable(String rawId)`。

- [ ] テストを先に書く(spec §2、§3 の式・境界、焼けない魚=熱帯魚・フグ、パーサーの `food` あり/なし)。
- [ ] 実装。`./gradlew test` が通る。
- [ ] コミット。

## Task 2: 焼き魚と焦げた魚のアイテム

**Files:** 変更 `item/ModItems.java`(`COOKED`: 魚 ID→アイテム、`BURNT_FISH`)、作成 `item/CookedFishItem.java`(食べ終わりに `heal`)、`item/FishLevels` は参照のみ。モデル JSON(`models/item/cooked_<id>.json` 56 件と `burnt_fish.json`、すべて共通の `cooked_fish` / `burnt_fish` テクスチャを参照)、言語ファイル、`client/FishTooltipHandler.java`(焼き魚に「体力 +N」と魚のレベル)。

**Interfaces:**
- Consumes: Task 1 の `FoodRules`、`FishInfo`(魚ごとの上書き)、`FishProfileLoader`(レア度の取得はサーバー起動後のデータから)。`FoodProperties` はアイテム登録時に確定する必要があるので、満腹度・飽和度は `FishLevels` からの自動値で作り、`food` の上書きはアイテムの食べ終わりのときに反映する形にするか、上書きを `FishLevels` 相当の表で持つ。難しい場合は、上書きを無視して、その旨を報告する。
- Produces: `ModItems.COOKED`(`Map<String, RegistryObject<Item>>`)、`ModItems.BURNT_FISH`。

- [ ] 共通の仮画像 `cooked_fish.png`(16x16、焼き色の魚)と `burnt_fish.png`(キットの 2D ルートで作る)を用意する。
- [ ] アイテム登録、モデル、言語、クリエイティブタブ、ツールチップ。
- [ ] ビルドと専用サーバー起動(ログに `Done`)、コミット。

## Task 3: 七輪ブロック

**Files:** 作成 `block/ShichirinBlock.java`、`block/ShichirinBlockEntity.java`、`client/ShichirinRenderer.java`、`block/ModBlocks.java`(ブロック・ブロックアイテム・ブロックエンティティタイプの登録)。リソース: `blockstates/shichirin.json`(`lit` による 2 つのモデル)、`models/block/shichirin.json`・`shichirin_lit.json`、`models/item/shichirin.json`、クラフトレシピ(`data/fishinggui/recipes/shichirin.json`)、ルートテーブル(自分自身を落とす)、言語ファイル。

**Interfaces:**
- Consumes: Task 1 の `CookingRules`、Task 2 の `ModItems.COOKED` と `ModItems.BURNT_FISH`。
- 振る舞いは spec §3 のとおり(右クリックで燃料・魚・取り出し、焼き時間、焦げ、消灯、サーバー側処理、ブロックエンティティの同期、置いた魚の表示)。

- [ ] 3D ブロックモデルと画像をキットで作る(軸に揃えたキューブ、点火時は赤く光る)。`java-vanilla-resourcepack` の出力から、必要な JSON とテクスチャを取り込み、名前空間を `fishinggui` に直す。
- [ ] ブロック、ブロックエンティティ(NBT の保存と同期)、サーバー側の tick、右クリック処理。
- [ ] 魚の表示用レンダラー(ブロックエンティティレンダラー)。
- [ ] ビルドと専用サーバー起動(ログに `Done`)、コミット。
