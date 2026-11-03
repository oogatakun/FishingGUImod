# Phase 4-A: ポイント変換とスキル画面の枠 設計書

- 関連: `docs/superpowers/backlog.md` の項目 7、8
- 日付: 2026-11-04
- 対象外: スキルの効果(別フェーズ)、リセットアイテム(Phase 4-C)。リセットで返すポイントを計算できるよう、スキルのレベルは保存する。

## 1. ポイント変換

- 釣果画面に第3のボタン「ポイントに変換」を足す(キープ、リリース、ポイントに変換)。
- 選ぶと魚はリリースされ、**釣り経験値は入らない**。代わりにポイントが入る。
- 得られるポイントは、釣った物のレア度で決まる(重量は使わない)。
  | レア度 | ポイント |
  |---|---|
  | common | 1 |
  | uncommon | 2 |
  | rare | 3 |
  | epic | 4 |
  これはレベル帯 1〜5 / 6〜10 / 11〜15 / 16〜20 と同じ区切り。ゴミ、宝物も、そのアイテムのレア度で同じ式を使う。
- 変換後、チャットに「ポイント +N(合計 M)」を出す。
- ESC は従来どおりキープ。
- 純 Java の `PointRules.pointsFor(Rarity)` に式を置き、単体テストする。

## 2. 保存

- 既存の釣りデータ(`Player.PERSISTED_NBT_TAG` → `fishinggui`)に、次を足す。
  - `points`: 今持っているポイント(整数、0 以上)。
  - `skills`: スキル ID → レベル(整数)の複合タグ。
- 死亡、再ログインでも残る(既存の `level`、`xp` と同じ扱い)。
- 純 Java の値オブジェクト `SkillState`(points、skills のマップ)を作り、`spend`、`add`、`levelOf`、`withSkillLevel` を単体テストする。

## 3. スキル(データ駆動)

- データパック `data/<ns>/fishinggui/skills/<id>.json`:
  ```json
  {"max_level": 3, "cost": [1, 2, 3]}
  ```
  - `cost` の長さ = `max_level`。`cost[i]` が、レベル i から i+1 に上げるコスト。すべて 1 以上の整数。
  - 不正なファイルは警告ログを出して読み飛ばす(`SkillParser` を純 Java にして単体テスト)。
- 名前と説明は言語ファイル(`skill.fishinggui.<id>`、`skill.fishinggui.<id>.desc`)。
- 今回は効果のないダミーを 3 つ同梱する: `dummy_a`、`dummy_b`、`dummy_c`(名前は「スキルA」など)。

## 4. スキル画面(J キー)

- J キー(キー設定で変更可、カテゴリ: 釣りGUI)で開く `SkillScreen`。
- 表示: 所持ポイント。スキルの一覧(名前、説明、レベル / 最大、次のコスト、「取得」ボタン)。最大レベルなら「MAX」。ポイント不足なら「取得」は押せない。
- 「取得」を押すと、サーバーにスキル ID を送る(`SkillBuyPacket`)。サーバーが、存在、最大未満、ポイント足りるかを確認して、ポイントを引き、レベルを 1 上げ、全体を同期する。拒否のときは何もしない。
- 同期パケット(`SkillSyncPacket`): `points` と、スキル定義(ID、最大、コスト列)、現在のスキルレベルを送る。ログイン、リスポーン、変更のたびに送る。クライアントは `ClientSkills` に保持して、画面はそこから描く。
- ポイント変換のとき、同期とチャット通知も一緒に出す。

## 5. コマンド(権限レベル 2)

- `/fishinggui points set|add <players> <int>`。
- `/fishinggui query <player>` の出力にポイントを足す。

## 6. テスト

- `PointRulesTest`: レア度ごとの点。
- `SkillStateTest`: 加算、消費、不足の拒否、スキルレベルの更新。
- `SkillParserTest`: 正常、`cost` の長さ違い、0 以下、最大の欠落。
- 実機: 変換でポイントが入り経験値が入らない。J で画面が開く。取得でポイントが減りレベルが上がる。不足、最大では取得できない。再ログインで残る。

## 7. 変更するファイル(予定)

- 新規: `skill/PointRules`、`skill/SkillState`、`skill/SkillDef`、`data/SkillParser`、`data/SkillLoader`、`skill/SkillService`、`net/SkillBuyPacket`、`net/SkillSyncPacket`、`client/ClientSkills`、`client/SkillScreen`、キー登録、ダミー JSON 3 つ。
- 変更: `client/CatchScreen`(第3ボタン)、`net/CatchChoicePacket`(3 値)、`hook/PendingCatchManager`(変換)、`level/FishingData`(保存)、`command/FishingCommand`、`net/ModNetwork`、言語ファイル。
