# Phase 2-A: エサアイテム 設計書

- 親設計書: `2026-11-02-fishing-fight-design.md`(5章「エサ」の具体化)
- 日付: 2026-11-02
- 対象外: データパック難易度 JSON、失敗時の竿の追加ダメージ、エサの入手手段、専用テクスチャ(→ 後続の Phase 2-B/C)

## 1. アイテム

| ID | 名前 | 効果 |
|---|---|---|
| `worm` | ミミズ | 効果なし(消費のみ) |
| `glow_bug` | 光る虫 | 釣りルートテーブルの幸運値 +2 |
| `sturdy_paste` | 練りエサ | `dashPower` を 0.6 倍 |

- 通常アイテム(スタック 64)。クリエイティブタブ「道具」にのみ登録する。入手手段は後の Phase で決める。
- テクスチャ・モデルは仮(バニラのアイテムテクスチャを流用)。

## 2. 装着と効果

- キャストの操作は変えない。
- ファイト開始時(`FishingHookHandler` → `FightManager.start`)に、**オフハンド**のエサを読む。竿をオフハンドで持っている場合、またはエサがない場合は「エサなし」(従来の効果)。
- `BaitEffect`(純 Java、MC 非依存)が `FishProfile` を調整する。`BaitType` の enum(`NONE`, `WORM`, `GLOW_BUG`, `STURDY_PASTE`)から、`apply(FishProfile)` と `luckBonus()` を引く。数値は `BaitEffect` の定数 1 か所に集約する。
- 光る虫の幸運値は `CatchResolver.success` の `luck` に加算する。`ActiveFight` が `BaitType` を保持する。

## 3. 消費

- ファイト**終了時**(成功・失敗・ESC 中断・ウキ消滅を含む、`FightManager.end`)に、オフハンドの同種エサを 1 個減らす。
- オフハンドのスタックがファイト開始時と違う種類なら(持ち替えた等)減らさない。
- クリエイティブモードでは減らさない。
- ファイトを始める前に竿を回収した場合は消費しない(ファイトが始まらないため)。
- ログアウト・サーバー停止ではセッション破棄のみで消費しない(現状どおり)。

## 4. テスト

- `BaitEffect`: 各 `BaitType` の `FishProfile` 倍率と `luckBonus` を JUnit で確認する(純 Java)。
- アイテム登録・オフハンド読み取り・消費は実機で確認する(オフハンドにエサを持って釣る、ファイト後に個数が減る、クリエでは減らない)。

## 5. 変更するファイル

- 新規: `fight/BaitType.java`、`fight/BaitEffect.java`、`item/ModItems.java`(`DeferredRegister`、タブ登録)、`BaitEffectTest`、assets の lang と item model(仮)
- 変更: `FightManager`(`ActiveFight` に `BaitType` を追加、終了時消費)、`FishingHookHandler`(エサ読み取りと profile 調整)、`CatchResolver`(幸運値の加算)、`FishingGuiMod`(`ModItems` 登録)
