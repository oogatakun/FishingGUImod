# ファーマーズディライト(FD)対応 設計メモ

- 日付: 2026-11-05
- 方針: FD が無くても動く。FD 用のレシピは `forge:mod_loaded`(modid `farmersdelight`)の条件つき。コード上の FD 依存は無い。

## 内容

1. **タグ(常時有効)**: オリジナル魚 56 種を `forge:raw_fishes`、焼き魚 56 種を `forge:cooked_fishes` に登録。FD の魚のレシピ(例: 魚のシチュー)で、うちの魚が使える。
2. **まな板(FD 時のみ)**: 魚 1 匹+ナイフ → `fishinggui:fish_fillet`(魚の切り身)2 個+骨粉。レシピは魚ごとに 56 件(`recipes/cutting/fillet_<魚>.json`)。
3. **FD の切り身への変換(FD 時のみ)**: 魚の切り身 1 個 → `farmersdelight:cod_slice` 1 個(作業台、順不同)。
4. **調理鍋(FD 時のみ)**: 魚の切り身 2 個+タマネギ(`forge:crops/onion`)+キャベツ(`forge:crops/cabbage`)+ボウル → 海鮮鍋(`fishinggui:seafood_hotpot`)。満腹度 10、飽和度の係数 0.8、スタック 16、ボウルで小分けして食べる。

## 画像

切り身と海鮮鍋はキットの 2D ルート(`fishinggui_items/fish_fillet`、`fishinggui_items/seafood_hotpot`)で作成。

## 確認できていないこと

FD 本体を入れたゲームでの動作(レシピの読み込み、まな板・調理鍋での動き)。FD の公開リポジトリ(1.20 ブランチ)のレシピ形式に合わせて書いた。
