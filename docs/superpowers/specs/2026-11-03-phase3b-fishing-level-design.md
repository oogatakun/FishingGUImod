# Phase 3-B: 釣りレベルと経験値 設計書

- 親設計書: `2026-11-03-phase3a-catch-screen-design.md`
- 日付: 2026-11-03
- 全体の順序: 3-A 釣果 GUI(済) → **3-B 釣りレベルと経験値** → 3-C レベルで変わるエサと釣れる魚 → オリジナル魚
- 対象外: レベルによる制限(3-C)、価格、オリジナル魚、エサの入手手段

## 1. 経験値とレベル

- 経験値が入るのは**リリースしたときだけ**。値は魚の `xp`(難易度 JSON、3-A で追加済み)。キープでは入らない。ESC やログアウトによるキープ扱いでも入らない。
- レベルは 1 から始まり、上限は **Lv.20**。上限に達したら経験値は 0 に固定し、それ以上増えない。
- Lv.n → n+1 の必要経験値は既定で `50 × n`。
- データパック `data/<ns>/fishinggui/fishing_levels/*.json`(`fish_profiles` と同じディレクトリ方式)で必要経験値を上書きできる。同梱は `data/fishinggui/fishinggui/fishing_levels/default.json`。形式: `{"xp_to_next": [50, 100, ...]}`(要素 i が Lv.(i+1) → Lv.(i+2) の必要量)。要素数は 19 でなければならず、各要素は 1 以上の整数。不正なファイルは警告を出して無視する。有効なファイルが複数あればファイル id の昇順で最後のものが勝つ。有効なファイルが 1 つもなければ既定値(`50 × n`)を使う。
- 純 Java の `FishingLevel`(`fight` パッケージ、または新設 `level` パッケージ)に計算を置く:
  - `record FishingProgress(int level, int xp)`
  - `FishingProgress addXp(FishingProgress, int amount, LevelCurve)`: 繰り上がり(複数レベル分も)、上限で固定。戻り値でレベルが上がったかが分かる。
  - `LevelCurve.xpToNext(int level)`: 上限レベルでは 0 を返す。
  - `FishingProgress` の `setLevel` / `setXp`(コマンド用。範囲を丸める)。

## 2. 保存

- プレイヤーの永続データ(`getPersistentData()` → `Player.PERSISTED_NBT_TAG` → `fishinggui` 以下に `level` / `xp`)に保存する。ログアウトしても残り、`PERSISTED_NBT_TAG` 以下は Forge が死亡時のクローンで自動的に引き継ぐ。リスポーン後はクライアントへ再同期する。
- 未保存のプレイヤーは Lv.1 / xp 0。

## 3. 表示と通知

- **釣果画面**: 重量・経験値の枠の中に、現在の「Lv.N 経験値/必要量」と経験値バーを出す(値はクライアントが保持している最新値。リリース後は画面が閉じるので、増えた量はチャット通知で見せる)。
- **ファイト画面**: 隅に「釣りLv.N」を小さく出す。
- **経験値チャット通知**: 経験値が入ったとき(リリース時、コマンドの `xp add` 時)に「釣り経験値 +12(Lv.3 80/150)」のように、増えた量と現在値を表示する。上限レベルでは「(最大レベル)」と表示する。
- **レベルアップ演出**(チャットではなく画面): 画面中央に「LEVEL UP!」と新しいレベルを大きく拡大表示し、光の放射、キラキラ、ファンファーレの音を重ねる。約 3 秒で消える。ゲームは止めない。複数レベル同時に上がったら最終レベルを出す。`/fishinggui level` コマンドで上がった場合も出す。
- 文字は言語ファイル(ja_jp / en_us)。

## 4. 音

- **アタリの合図**: 魚がウキに食いつき、ファイトを開始できる状態になった瞬間(`hook.nibble` が 0 から正に変わった瞬間、クリックして画面を開く前)に、そのプレイヤーにだけ経験値オーブ取得音(`ENTITY_EXPERIENCE_ORB_PICKUP`)を鳴らす。経験値の増減とは無関係。サーバーでプレイヤーの tick ごとに、ウキの `nibble` の前回値を覚えて検出する。
- ファイト中に `nibble` を 2 以上に固定している処理(FightManager)とは干渉しない。ファイトが始まった後は鳴らさない。

## 5. コマンド

- ルート `/fishinggui`、権限はオペレーター(レベル 2)。
  - `/fishinggui level set <players> <level>`(1〜20 に丸める。xp は 0 に戻る)
  - `/fishinggui level add <players> <amount>`(負の値で下げられる。1〜20 に丸める)
  - `/fishinggui xp set <players> <amount>`(0 以上。レベルは変えず、必要量以上なら繰り上げ処理に通す)
  - `/fishinggui xp add <players> <amount>`(繰り上がりあり。負の値は無効)
  - `/fishinggui query <player>`(現在のレベルと経験値を返す)
- 実行結果は実行者にフィードバックを返す。対象プレイヤーには経験値チャット通知とレベルアップ演出を出す。

## 6. 通信

- S→C `FishingProgressPacket(int level, int xp, int xpToNext, int gained, boolean leveledUp)`: ログイン時、ファイト開始時、リリース時、コマンド変更時に送る。クライアントは値を保持し(`ClientFightHooks`)、`gained > 0` ならチャット通知を出し、`leveledUp` なら演出を始める。
- `CatchResultPacket` は変更しない。
- `PendingCatchManager.choose(player, keep=false)` が経験値を加算して保存し、パケットを送る。ログイン、リスポーン、ファイト開始時は `gained = 0` で同期する。

## 7. テスト

- `FishingLevelTest`: 加算、繰り上がり、複数レベル同時、上限固定、上限での加算が無効、`set` の丸め。
- `LevelCurve` パーサのテスト: 正常、要素数不正、0 以下、非整数は拒否。
- 実機: リリースで経験値とチャット通知、レベルアップ演出、キープで経験値なし、コマンド各種、ログアウト後と死亡後の保持、アタリ音(クリック前)。

## 8. 変更するファイル(予定)

- 新規: `level/FishingProgress.java`、`level/LevelCurve.java`、`level/FishingData.java`(保存の読み書き)、`data/LevelCurveLoader.java`、`command/FishingCommand.java`、`net/FishingProgressPacket.java`、`client/LevelUpOverlay.java`、`hook/BiteSoundHandler.java`、各テスト、同梱の `fishing_levels.json`
- 変更: `PendingCatchManager`、`FightManager`(ファイト開始時の同期)、`CatchScreen`、`FightScreen`、`ClientFightHooks`、`ModNetwork`、lang ファイル
