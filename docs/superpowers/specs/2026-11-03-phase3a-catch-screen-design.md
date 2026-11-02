# Phase 3-A: 釣果 GUI 設計書

- 親設計書: `2026-11-02-fishing-fight-design.md`、`2026-11-03-phase2b-difficulty-penalty-design.md`
- 日付: 2026-11-03
- 全体の順序: 3-A 釣果 GUI → 3-B 釣りレベルと経験値(保存・表示) → 3-C レベルで変わるエサと釣れる魚 → オリジナル魚
- 対象外: 経験値の加算と保存、レベル、レベルによる制限、価格、魚の3D表示、オリジナル魚

## 1. 流れ

1. ファイト成功(`FightSession.Phase.SUCCESS`)で、サーバーは釣果をその場で配らず「待ち状態」にし、クライアントに釣果 GUI を開かせる。
2. プレイヤーが選ぶ:
   - **キープする**: 待ち状態の釣果のアイテムを配る(従来どおりウキの位置から飛ばす)。バニラの経験値オーブも出す。魚の釣りレベル用経験値は入らない。
   - **リリースする**: アイテムは配らない。経験値は入る(3-A では表示のみ。実際の加算は 3-B)。バニラの経験値オーブは出さない。
3. GUI を ESC で閉じた場合はキープ扱い。待ち中にログアウトした場合もキープ扱いで、アイテムはウキの位置に落とす。サーバー停止時は破棄する。
4. 待ち状態の釣果がないのに選択パケットが届いた場合は無視する。

成功した瞬間に行う処理(従来どおり): `ItemFishedEvent` の発行(キャンセル時は竿のダメージのみで終了)、`FISHING_ROD_HOOKED` 進捗トリガー、`FISH_CAUGHT` 統計、竿の耐久消費、回収音、ウキの破棄。**アイテム実体の生成とバニラ経験値オーブだけをキープまで遅らせる。**

## 2. GUI(`CatchScreen`)

- 上: 魚名(アイテム名、大きく)、その下に角丸のレア度バッジ(コモン / アンコモン / レア / エピック)。
- 中央: アイテムの絵を拡大表示(8 倍程度)。
- 下: 「重量」「経験値」の枠。その下に「キープする」「リリースする」ボタン。
- ゲームは止めない(非ポーズ)。マウスカーソルを出し、ボタンをクリックで選ぶ。
- 文字は言語ファイル(`gui.fishinggui.catch.*`、ja_jp / en_us)。レア度名も言語ファイル。

## 3. 魚ごとのデータ(`FishInfo`)

難易度 JSON(`fish_profiles`)に任意項目を追加する。

| 項目 | 型 | 既定 | 制約 |
|---|---|---|---|
| `rarity` | `"common"` / `"uncommon"` / `"rare"` / `"epic"` | `common` | 不正な文字列は拒否 |
| `weight_min` | 数値(kg) | 0.1 | > 0 |
| `weight_max` | 数値(kg) | 1.0 | >= `weight_min` |
| `xp` | 整数 | 10 | >= 0 |

- 純 Java の `FishInfo(Rarity rarity, float weightMin, float weightMax, int xp)` と `Rarity` enum を `fight` パッケージに置く。`FishInfo.DEFAULT` を持つ。
- `FishProfileParser.Entry` は `(item, profile, info)` にする。
- `FishProfileLoader` は `profileFor` に加えて `infoFor(List<ItemStack>)` を持つ(最初のスタックで検索。なければ `DEFAULT`)。
- 重量はファイト開始ではなく**成功時**に `FishInfo.rollWeight(Random)`(範囲内の一様乱数、小数1桁に丸め)で決める。
- 同梱データに値を入れる(重量・経験値・レア度は実装時に決める。弱い魚ほど軽く低レア、宝物は高レア・高経験値)。

## 4. 通信

- S→C `CatchResultPacket(ItemStack stack, byte rarity, float weight, int xp)`: GUI を開く。
- C→S `CatchChoicePacket(boolean keep)`: 選択。`ReelInputPacket` と同じ登録方法(`ModNetwork`)。
- サーバーは `PendingCatchManager` で `UUID → PendingCatch(loot, hook 位置, 経験値)` を保持する。1人につき待ち状態は1つ。
- クライアントへの GUI 表示は `DistExecutor` 経由(`ClientFightHooks` と同じ手法)。

## 5. テスト

- `FishProfileParser`: 新項目の正常、省略値、不正値(rarity、weight の大小、xp が負)。
- `FishInfo.rollWeight`: 範囲内、1桁丸め、min == max。
- 実機: 釣果 GUI の表示、キープでアイテム入手、リリースでアイテムなし、ESC でキープ、ログアウト時のドロップ。

## 6. 変更するファイル

- 新規: `fight/Rarity.java`、`fight/FishInfo.java`、`hook/PendingCatchManager.java`、`net/CatchResultPacket.java`、`net/CatchChoicePacket.java`、`client/CatchScreen.java`、`FishInfoTest`
- 変更: `FishProfileParser`(+テスト)、`FishProfileLoader`、`CatchResolver`(アイテム生成の分離)、`FightManager`(成功時に待ち状態へ)、`ModNetwork`、`ClientFightHooks`、同梱 JSON、lang ファイル
