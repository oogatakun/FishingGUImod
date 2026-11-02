# Phase 2-B: 難易度 JSON と失敗ペナルティ 設計書

- 親設計書: `2026-11-02-fishing-fight-design.md`(§3 FishProfile、§9 Phase 2)、`2026-11-02-phase2a-bait-design.md`
- 日付: 2026-11-03
- 対象外: エサの入手手段、GUI の魚名表示、最大ファイト時間、GameTest

## 1. 釣果の先行抽選

- ファイト開始時(`FishingHookHandler` → `FightManager.start`)に、バニラの釣りルートテーブルから釣果(`List<ItemStack>`)を抽選する。幸運値は `竿のエンチャント + player.getLuck() + BaitEffect.luckBonus(bait)`。
- 抽選処理は `CatchResolver.roll(player, hook, rod, bonusLuck)` に切り出す。`ActiveFight` が結果を保持する。
- 成功時は保持した釣果を渡す(`CatchResolver.success` は釣果を引数で受け取る)。ItemFishedEvent、進捗トリガー、経験値、統計、竿の耐久消費は従来どおり。
- 失敗(糸切れ、ESC、持ち替え、ウキ消滅、切断)では釣果を破棄する。
- 抽選結果が空の場合は `FishProfile.DEFAULT` で戦う。

## 2. 難易度 JSON

- 場所: `data/<namespace>/fishinggui/fish_profiles/<name>.json`。`SimpleJsonResourceReloadListener` で読み込み、`/reload` で反映する(`AddReloadListenerEvent`)。
- 形式:
  ```json
  { "item": "minecraft:pufferfish", "stamina": 60, "dash_interval": 40, "dash_power": 1.5, "tension_gain": 1.0 }
  ```
  `item` は必須。他は省略可で、省略時は `FishProfile.DEFAULT` の値。不正な値(数値でない、範囲外)のファイルはログに警告を出して読み飛ばす。
- 範囲: `stamina` > 0、`dash_interval` >= 1、`dash_power` >= 0、`tension_gain` > 0。
- 検索: 釣果の**最初のスタック**のアイテム ID で引く。該当がなければ `DEFAULT`。同じ `item` が複数あれば後から読み込まれたものが優先。
- 同梱データ(数値は実装時に決める。既定より弱い/強いの方向のみ指定):
  - 弱い: `cod`、ゴミ類(`bowl`、`stick`、`string`、`bone` ほか)
  - 少し強い: `pufferfish`
  - 強い(宝物): `enchanted_book`、`name_tag`、`saddle`、`nautilus_shell`、`fishing_rod`
- 読み込みと検証は純 Java の `FishProfileParser`(`JsonObject` → `Optional<Entry>`)に分け、単体テストする。

## 3. エサとの合成

`profile = BaitEffect.apply(bait, jsonProfile)`。ファイト開始時に 1 回だけ計算する。

## 4. 失敗ペナルティ

- テンション 100 の**糸切れ**のときだけ、竿の耐久度を追加で 5 減らす(定数 `LINE_BREAK_ROD_DAMAGE`)。ESC 中断、持ち替え、ウキ消滅、切断は追加消費なし。
- `FightManager.end` で糸切れ(セッション自身が `FAILED` になった場合)と中断を区別する。竿はファイト開始時の手のスタックで、競合を避けるためにまだ竿を持っている場合のみ減らす。
- エサは従来どおり、ファイト終了時にどの結果でも 1 個消費する(2-A)。

## 5. テスト

- `FishProfileParser`: 正常、省略値、不正値(範囲外、型違い、`item` なし)。
- 実機: 魚ごとの強さの違い、糸切れ時の耐久減少(5)、ESC では追加消費なし、`/reload` で数値変更が反映される。

## 6. 変更するファイル

- 新規: `data/FishProfileParser.java`、`data/FishProfileLoader.java`(リロードリスナ + 検索 `FishProfileLoader.lookup(Item)`)、`FishProfileParserTest`、`src/main/resources/data/fishinggui/fishinggui/fish_profiles/*.json`
- 変更: `CatchResolver`(`roll` の切り出し、`success` が釣果を受け取る)、`FightManager`(釣果保持、糸切れの区別、追加ダメージ)、`FishingHookHandler`(抽選とプロファイル決定)、`FishingGuiMod`(リロードリスナ登録)
