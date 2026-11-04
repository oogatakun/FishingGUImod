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

## 3. リセット(スキル 1 つずつ)

- 1 個の `reset_drop` で、選んだスキル 1 つだけを完全に Lv0 に戻す。他のスキルは変えない。
- 純 Java の `SkillState`:
  - `refundOfSkill(SkillDef def)`: `cost` の先頭から現在のレベルの数だけ足した合計(`cost` の長さを超える分は打ち切り)。
  - `resetSkill(SkillDef def)`: そのスキルを外し、ポイント = 現在 + 戻す分。そのスキルのレベルが 0 なら `this` をそのまま返す。
- `SkillService.reset(ServerPlayer, String skillId)`(サーバー側の検証):
  - スキル ID が存在しなければ何もしない。
  - そのスキルのレベルが 0 なら何もしない(アイテム消費なし)。
  - メインインベントリ・ホットバー・オフハンドに `reset_drop` が無ければ何もしない。
  - あれば丁度 1 個消費し、`SkillData.set` で保存、`sync`。チャットで「<スキル名>をリセットした(+N ポイント)」を表示。
- パケット `SkillResetPacket(String skillId)`(クライアント→サーバー)。サーバーがすべて検証する。

## 4. スキル画面

- 画面下の単一のリセットボタンは無し。各スキル行の「取得 / MAX」ボタンの右に、小さな赤字の「リセット」ボタン(幅 60)を置く。行の幅 `ROW_W` は 420。説明文はボタンの左側の幅に収まるよう折り返す。
- クライアントが `reset_drop` を持っていない(メインインベントリ + オフハンド。防具は見ない)ときは、ボタンを一切表示しない。
- 持っているときは全行に表示し、そのスキルのレベルが 0 の行は押せない状態にする。
- 押すと `ConfirmScreen`(スキル名入り、はい / いいえ)を出し、はいで `SkillResetPacket(skillId)` を送る。確認後は SkillScreen に戻る。
- SkillSync のたびに画面を更新(既存の `refresh()`)。

## 5. 言語ファイル(ja / en)

`item.fishinggui.reset_drop`、`item.fishinggui.reset_drop.desc`(ツールチップの灰色の説明。`FishTooltipHandler` で追加)、`gui.fishinggui.skills.reset`、`gui.fishinggui.skills.reset.confirm`(引数: スキル名)、`gui.fishinggui.skills.reset.done`(引数: スキル名、戻したポイント)、`gui.fishinggui.reset_drop.found`。`need_item` は廃止。

## 6. テスト

- `SkillStateTest`: `refundOfSkill` と `resetSkill`(1 スキルの返却、レベル 0 は変更なし、他スキルは不変、打ち切り)。
- `ResetRulesTest`: `rollsDrop` の境界。
- ビルド: `./gradlew compileJava test build`。
