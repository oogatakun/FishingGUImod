# Phase 4-C: スキルのリセットアイテム 設計書

- 関連: `docs/superpowers/backlog.md` の項目 8
- 日付: 2026-11-04

## 1. アイテム

- ID `fishinggui:reset_drop`(名前「忘却の雫」/ Drop of Oblivion)。スタック 64。クリエイティブタブにも入れる。
- テクスチャは仮のもの(青い水滴)。`models/item/reset_drop.json` は `item/generated`。

## 2. 入手

- 魚の釣り上げ判定とは別の抽選。`PendingCatchManager.open` で、確率 `RESET_DROP_CHANCE = 0.015` で当たりを決め、`Pending` に `bonus`(ItemStack、なしは空)として持つ。
- 「ポイントに変換」以外(KEEP / RELEASE / ログアウト時の KEEP)で、`CatchResolver.deliver` と同じ方法でプレイヤーに渡す。CONVERT では渡さない。
- 当たったら、受け取り時にチャットで知らせる(`gui.fishinggui.reset_drop.found`)。
- 確率は純 Java の定数にし、抽選は `ResetRules.rollsDrop(double random)`(`random < chance`)で単体テストする。

## 3. リセット

- 純 Java の `SkillState.reset(Map<String,SkillDef> defs)`:
  - 戻すポイント = 各スキルについて、`cost` の先頭から現在のレベルの数だけ足した合計。
  - 結果は、ポイント = 現在 + 戻す分、スキルは空。
  - `defs` に無いスキル ID は、戻すポイント 0 で捨てる。
  - `SkillState.refundOf(defs)` で戻すポイントだけも返す(単体テスト)。
- `SkillService.reset(ServerPlayer)`:
  - スキルが 1 つも無い(戻すポイントが 0 で、スキル空)なら何もしない(アイテム消費なし)。
  - インベントリ(メイン、ホットバー、オフハンド)に `reset_drop` が無ければ何もしない。
  - あれば 1 個消費し、`SkillData.set` で保存、`sync`。チャットで「スキルをリセットした(+N ポイント)」を表示。
- パケット `SkillResetPacket`(中身なし、クライアント→サーバー)。サーバーがすべて検証する。

## 4. スキル画面

- 画面の下に「リセット」ボタン(赤字)。押すと確認画面(「本当にリセットしますか？」はい / いいえ)を出し、はいで `SkillResetPacket` を送る。
- 押せる条件(クライアント側の見た目だけ): プレイヤーが `reset_drop` を持っている、かつスキルを 1 つ以上取得している。持っていなければ押せない状態で、ツールチップに「忘却の雫が必要」。
- 確認画面は `ConfirmScreen` を使ってよい。確認後は SkillScreen に戻る。
- SkillSync のたびに画面を更新(既存の `refresh()`)。

## 5. 言語ファイル(ja / en)

`item.fishinggui.reset_drop`、`gui.fishinggui.skills.reset`、`gui.fishinggui.skills.reset.need_item`、`gui.fishinggui.skills.reset.confirm`、`gui.fishinggui.skills.reset.done`(引数: 戻したポイント)、`gui.fishinggui.reset_drop.found`。

## 6. テスト

- `SkillStateTest`: `refundOf` と `reset`(複数スキル、レベル 0、未知 ID)。
- `ResetRulesTest`: `rollsDrop` の境界。
- ビルド: `./gradlew compileJava test build`。
