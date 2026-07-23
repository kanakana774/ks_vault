# PostgreSQL 実行計画（EXPLAIN）ガイド

---

## 目次

1. [EXPLAINの基本](#1-explainの基本)
2. [実行計画の構造と処理フローの読み方](#2-実行計画の構造と処理フローの読み方)
3. [スキャンノード詳細：選択基準と比較](#3-スキャンノード詳細選択基準と比較)
4. [結合ノード詳細：選択基準と比較](#4-結合ノード詳細選択基準と比較)
5. [集約・ソートノード詳細](#5-集約ソートノード詳細)
6. [Buffersオプション完全解説](#6-buffersオプション完全解説)
7. [ループを含む実行計画の手続き的読み方](#7-ループを含む実行計画の手続き的読み方)
8. [パフォーマンス改善チェックリスト](#8-パフォーマンス改善チェックリスト)
9. [実例：改善前後の比較](#9-実例改善前後の比較)
10. [便利なオプションまとめ](#10-便利なオプションまとめ)

---

## 1. EXPLAINの基本

```sql
-- 実行計画のみ（実際には実行しない）
EXPLAIN SELECT * FROM orders WHERE user_id = 42;

-- 実際に実行して実測値も取得
EXPLAIN ANALYZE SELECT * FROM orders WHERE user_id = 42;

-- バッファ使用状況も表示（ANALYZEと組み合わせる）
EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM orders WHERE user_id = 42;

-- JSON形式で出力（ツール連携に便利）
EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) SELECT * FROM orders WHERE user_id = 42;
```

> ⚠️ `EXPLAIN ANALYZE` は **実際にクエリを実行する**。`UPDATE`/`DELETE` に使う際は `BEGIN; ... ROLLBACK;` で囲むこと。

---

## 2. 実行計画の構造と処理フローの読み方

### 基本フォーマット

```
ノード名  (cost=起動コスト..総コスト  rows=推定行数  width=推定行幅)
          (actual time=起動時間..総時間  rows=実際行数  loops=ループ回数)
  付加情報（Filter, Index Cond, Hash Cond など）
  Buffers: shared hit=N read=N ...
```

| フィールド | 意味 |
|---|---|
| `cost=A..B` | A: 最初の1行を返すまでの**起動コスト**、B: 全行を返すまでの**総コスト**（単位：コストユニット） |
| `rows` | プランナが統計情報から**推定**した出力行数 |
| `width` | 1行あたりの推定バイト数（`SELECT *` なら全カラム合計） |
| `actual time=A..B` | A: 最初の1行までの実測ms、B: 全行処理完了までの実測ms（`ANALYZE`時のみ） |
| `loops` | このノードが**呼ばれた回数**。`actual time` と `rows` は1ループ分の値 |

### loopsを考慮した実コストの計算

```
表示値             意味
-----------        -----------------------------------------------
actual rows=10     1回のループで返した行数
loops=500          このノードは500回呼ばれた
→ 合計処理行数 = 10 × 500 = 5,000 行

actual time=0.1..2.5  1ループあたりの処理時間（ms）
loops=500
→ 合計処理時間 = 2.5ms × 500 = 1,250ms = 1.25秒
```

**注意：** 実行計画の表示値はループ1回分なので、ループが多いノードほど見た目のコストが小さく見える。
必ず `actual time × loops` で**本当のコスト**を計算すること。

### ツリーの読み方（実行順序）

実行計画は**下から上へ（深いインデントから）** 読む。
子ノードが行を生成し、親ノードがそれを受け取って処理する。

```
[例]
Hash Join                          ← ③ 最後：ハッシュ表と外側の行を照合
  ->  Seq Scan on orders           ← ① 先に全行スキャン（外側テーブル）
  ->  Hash                         ← ② ハッシュ表を構築（内側）
        ->  Seq Scan on users      ← ② の素材：usersを全件読む
```

---

## 3. スキャンノード詳細：選択基準と比較

### 前提知識：ヒープとインデックスの物理構造

スキャンノードを正確に理解するには、PostgreSQLがデータをディスク上にどう格納しているかを知る必要がある。

---

#### ヒープ（heap）とは

「ヒープ」とはPostgreSQLにおける**テーブルの実データファイル**のことを指す。
（データ構造のヒープ木とは無関係。「無秩序に積まれた実データ」というニュアンス）

```
ディスク上のファイル構成（テーブル orders の場合）:

$PGDATA/base/16384/
  ├── 24601          ← ordersテーブルの「ヒープファイル」（実データ）
  ├── 24601_vm       ← Visibility Map（後述）
  ├── 24601_fsm      ← Free Space Map
  ├── 24605          ← idx_orders_user_id インデックスファイル
  └── 24607          ← idx_orders_created インデックスファイル
```

ヒープファイルの中身は **8KBのページ（ブロック）** の連続。
各ページの中に実際の行データ（タプル）が格納されている。

```
ヒープファイルの内部構造:

┌──────────────────────────────────────────────────┐
│  ページ 0（8KB）                                   │
│  [ページヘッダ][行1データ][行2データ][行3データ]...  │
├──────────────────────────────────────────────────┤
│  ページ 1（8KB）                                   │
│  [ページヘッダ][行4データ][行5データ][行6データ]...  │
├──────────────────────────────────────────────────┤
│  ページ 2（8KB）                                   │
│  ...                                              │
└──────────────────────────────────────────────────┘

各行には TID（Tuple ID）= (ページ番号, ページ内の行番号) が振られる
例: TID = (1, 3) → ページ1の3番目の行
```

行データには `user_id`, `amount`, `status` などの**全カラムの値**が入っている。
テーブルに対してSELECTするということは、このヒープファイルを読むことを意味する。

---

#### インデックスとは

インデックスは**ヒープとは別の独立したファイル**。
「ある列の値 → その行がヒープのどこにあるか（TID）」という対応表。

```
インデックスファイル（B-tree構造）の概念:

          [ルートノード]
         /              \
   [内部ノード]        [内部ノード]
   /     \              /     \
[リーフ] [リーフ]  [リーフ]  [リーフ]

リーフノードの中身:
┌───────────────────────────────────┐
│ user_id=40 → TID=(5, 2)          │  ← ヒープのページ5・行2を指す
│ user_id=41 → TID=(8, 7)          │
│ user_id=42 → TID=(2, 1)          │  ← user_id=42 の行はページ2の1番目
│ user_id=42 → TID=(15, 4)         │  ← 同じuser_idが別ページに存在
│ user_id=43 → TID=(2, 5)          │
└───────────────────────────────────┘
```

インデックスには「条件に使う列の値」と「TID」しか入っていない。
`SELECT amount FROM orders WHERE user_id = 42` の場合、
`amount` の値はインデックスにない → ヒープを読みに行く必要がある。

---

#### なぜ特定のスキャンだけヒープを読むのか

この疑問が、各スキャンノードの本質的な違いを理解する鍵になる。

```
スキャン種別と「ヒープを読むか否か」の対応:

┌─────────────────────┬────────────────┬─────────────────────────────────────┐
│ ノード名             │ ヒープを読むか │ 理由                                │
├─────────────────────┼────────────────┼─────────────────────────────────────┤
│ Seq Scan            │ 常に読む       │ ヒープを直接走査するのがSeq Scan     │
│ Index Scan          │ 常に読む       │ インデックスでTIDを得た後、ヒープへ  │
│ Index Only Scan     │ 原則読まない   │ 必要な値がインデックスにある          │
│                     │（条件次第で読む） │ ただしVM未確認ページは読む           │
│ Bitmap Heap Scan    │ 常に読む       │ Bitmap Index Scanの後にヒープへ      │
└─────────────────────┴────────────────┴─────────────────────────────────────┘
```

つまり**「ヒープを読む」=「実データファイルにアクセスする」**こと。
インデックスだけで完結できればヒープアクセスは不要で、それが Index Only Scan の強みになる。

---

### 全スキャンノードの内部動作と最適条件

---

#### Seq Scan（シーケンシャルスキャン）

```
Seq Scan on orders  (cost=0.00..18340.00 rows=1000000 width=64)
  Filter: (status = 'pending')
  Rows Removed by Filter: 990000
```

**内部動作：**
インデックスを一切使わず、**ヒープファイルのページ0から末尾まで順番に読む**。
各ページを読んで行を取り出し、Filter条件を評価する。

```
処理フロー:
for page in heap_file(page=0 to N):    ← ページを先頭から順に読む
    for row in page.rows:
        if row.status == 'pending':    ← CPU上でFilter評価
            emit row
        # 該当しない行は捨てる（Rows Removed by Filter）
```

ページを先頭から順に読むため、OSのread-ahead（先読み）が機能し、
ランダムI/Oと比べてディスクアクセスが非常に効率的。

**最適と判断される条件：**
- 選択率が**高い（目安 20〜100%）**。大量の行を返すならどうせほぼ全ページ読む
- テーブルが small でshared_buffersに収まる（ランダムI/Oコストが無意味になる）
- 対象列にインデックスがない
- `random_page_cost` が高い環境（HDD）でインデックス経由よりSeq Scanの方が安い

**コスト計算式（プランナ内部）：**
```
コスト = seq_page_cost × ページ数 + cpu_tuple_cost × 総行数
       = 1.0 × 18000 + 0.01 × 1000000 = 28000
```

---

#### Index Scan

```
Index Scan using idx_orders_user_id on orders
  Index Cond: (user_id = 42)
  Filter: (amount > 100)
  Rows Removed by Filter: 150
```

**内部動作：**
インデックスとヒープの**両方**を読む。

```
処理フロー:
# Step 1: インデックスファイルをB-tree降下
index_entries = btree_lookup(idx_orders_user_id, user_id=42)
# → TIDのリスト: [(2,1), (15,4), (31,2), ...] が返る

# Step 2: TIDを1件ずつ使ってヒープへランダムアクセス
for tid in index_entries:
    heap_row = heap_fetch(tid)         ← ランダムI/O（各TIDごとに別ページかも）
    if heap_row.amount > 100:          ← Filter評価（Index Condに含まれない条件）
        emit heap_row
```

TIDはユーザーIDでソートされているが、**対応するヒープページ番号はバラバラ**。
TIDが100件あれば、最悪100回の異なるページへのランダムアクセスが発生する。

**`Index Cond` と `Filter` の違い：**
```
Index Cond: (user_id = 42)   ← インデックスで絞れる条件（B-treeを降下して直接ヒット）
Filter:     (amount > 100)   ← インデックスで絞れない条件（ヒープから行を取得後にCPU評価）

→ Filterは「インデックスの恩恵を受けていない条件」。
  Rows Removed by Filterが多いほど、ヒープから不要な行を取得している。
```

**最適と判断される条件：**
- 選択率が**低い（目安 0.01〜5%）**。ランダムアクセスの総数が少なく済む
- `random_page_cost` が低い環境（SSD）
- インデックス列の **correlation（物理順との一致度）が高い**

**correlationとランダムI/Oの関係：**
```sql
SELECT attname, correlation FROM pg_stats WHERE tablename = 'orders';
```

```
correlation = 1.0:
  インデックス順 = ヒープの物理格納順
  → user_id=42 の行が物理的に連続している
  → ページを再利用できてランダムI/O少ない ✅

correlation = 0.0:
  インデックス順 とヒープ格納順が無関係
  → user_id=42 の行が全ページに散らばっている
  → 100行あれば100ページへのランダムアクセスになりうる ❌

例: created_at に連番INSERTしたテーブル → correlation ≈ 1.0
    user_id がランダムに振られたテーブル  → correlation ≈ 0.0
```

correlationが低いカラムへのIndex Scanは、取得行数が増えると**Seq Scanより遅くなる**。
これがプランナがSeq Scanを選ぶ理由の一つ。

**コスト計算式（概略）：**
```
コスト = インデックス走査コスト + ヒープランダムI/Oコスト
       = (cpu_index_tuple_cost × インデックスエントリ数)
       + (random_page_cost    × ヒット行数)   ← 行数が増えると急増
```

---

#### Index Only Scan

```
Index Only Scan using idx_orders_user_created on orders
  Index Cond: (user_id = 42)
  Heap Fetches: 5
```

**内部動作：**
インデックスだけで回答できる場合に限り、**ヒープを読まない**スキャン。

```
前提: インデックス idx_orders_user_created が (user_id, created_at) で作られており、
      クエリが SELECT user_id, created_at FROM orders WHERE user_id = 42 の場合

処理フロー:
index_entries = btree_lookup(idx_orders_user_created, user_id=42)
for entry in index_entries:
    # entry には user_id と created_at の値が既に入っている
    # → ヒープを読まずにそのまま返せる！
    emit (entry.user_id, entry.created_at)
```

ただし、「この行は本当に現在のトランザクションから見えるか（可視か）」という確認が必要になる。
インデックスエントリ自体は可視性情報を持っていないため、**Visibility Map（VM）** で確認する。

**Visibility Map（VM）とは：**
```
VMはヒープファイルとは別の小さなファイル（orders_vm）。
各ヒープページに対して「このページの全行が全トランザクションにとって可視か」を
1ビットで記録している。

VM bit = 1（all-visible）: このページの全行は誰でも見える
         → インデックスの値をそのまま返してOK（ヒープ不要）
VM bit = 0: 最近更新・削除があり、まだ全員に可視ではない行が含まれる可能性がある
         → 確認のためヒープを読みに行く → これが Heap Fetches

VACUUMを実行するとVMビットが更新される。
テーブルへの更新・削除が多いとVMビットが0になり、Heap Fetchesが増える。
```

**Heap Fetches の意味：**
```
Heap Fetches: 0    → VMですべてのページが all-visible と確認済み。ヒープ参照ゼロ ✅
Heap Fetches: 500  → 500行分、可視性確認のためヒープを読んだ。
                     VACUUM不足の可能性。パフォーマンスが Index Scan に近づく ❌
```

**最適と判断される条件：**
- SELECTするカラムが**すべてインデックスに含まれている**（covering index）
- `Heap Fetches` が 0 または極小
- VACUUMが定期的に実行されVMが最新状態

```sql
-- Heap Fetches を減らすには
VACUUM orders;

-- または自動VACUUMの頻度を上げる
ALTER TABLE orders SET (autovacuum_vacuum_scale_factor = 0.01);

-- SELECTしたいカラムをINCLUDEで付け加えてcovering indexを作る
CREATE INDEX idx_orders_user_covering
    ON orders(user_id) INCLUDE (created_at, amount);
```

---

#### Bitmap Index Scan + Bitmap Heap Scan

```
Bitmap Heap Scan on orders  (cost=423.15..8901.34 rows=20000 width=64)
  Recheck Cond: (user_id = 42)
  Heap Blocks: exact=3500
  ->  Bitmap Index Scan on idx_orders_user_id
        Index Cond: (user_id = 42)
```

**なぜ2つのノードに分かれているか：**
これは1つの処理を2フェーズに分けて実行した結果が実行計画に現れたもの。
Index Scanのように「1TIDずつヒープへランダムアクセス」せず、
**一度インデックスを全部読んでからヒープへの読み方を最適化する**ことが目的。

**フェーズ1 — Bitmap Index Scan：**
```
インデックスを走査し、条件にヒットした行の「ヒープページ番号」をメモリ上のビットマップに記録する。

ビットマップ（exact mode）:
  ページ0: 0  （該当行なし）
  ページ2: 1  （user_id=42 の行がある）← TID=(2,1), (2,5) など
  ページ7: 0
  ページ15: 1 （user_id=42 の行がある）
  ページ31: 1
  ...

→ この段階ではヒープをまだ1ページも読んでいない。
  ビットマップはメモリ（work_mem）上だけに存在する。
```

**フェーズ2 — Bitmap Heap Scan：**
```
ビットマップを参照し、「1」が立っているページをページ番号順（＝物理順）に読む。

ページ2  → 読む（シーケンシャルI/Oに近い）
ページ15 → 読む
ページ31 → 読む
...

→ ページを物理順に読むため、ランダムI/Oが最小化される。
  これが Index Scan との最大の違い。
```

**Index Scan との I/O パターンの比較：**
```
【Index Scan の場合（user_id=42 が20000行あるケース）】
TID=(2,1)  → ヒープページ2  を読む（ランダムI/O）
TID=(15,4) → ヒープページ15 を読む（ランダムI/O）
TID=(2,5)  → ヒープページ2  を読む（またランダムI/O、さっき読んだけどキャッシュになければ再読）
TID=(31,2) → ヒープページ31 を読む（ランダムI/O）
... 20000回繰り返す

→ 同じページを何度もランダムアクセスする可能性あり

【Bitmap Heap Scan の場合】
まずインデックスを全走査してビットマップ構築 → ページ2, 15, 31, ... がリストアップ
その後ページ番号順に1回ずつ読む:
  ページ2  → 読む（その中の user_id=42 の行を全部まとめて処理）
  ページ15 → 読む
  ページ31 → 読む
... 対象ページ数だけ（重複なし）

→ 各ページを最大1回しか読まない。行数が多いほど Index Scan との差が大きくなる
```

**exact（正確）モードと lossy（概算）モード：**

ここが「lossyでもディスクI/Oは変わらないのでは？」という疑問の核心。

```
【exact モード（work_mem が十分なとき）】
ビットマップが「ページ番号 + ページ内の行番号（offset）」を記録する。
→ 「ページ2の1行目と5行目だけが該当」と正確に把握
→ ヒープを読んでも、該当行だけ取り出せばよい
→ Recheck Cond は形式上存在するが、実際には必ず通る（再確認は軽い）

Heap Blocks: exact=3500 lossy=0   ← 全部exactモード
```

```
【lossy モード（work_mem が不足したとき）】
ビットマップが行番号を保持しきれず、「ページ番号だけ」に情報が落ちる。
→ 「ページ2のどこかに該当行がある」という粗い情報しかない
→ ヒープを読むとき、該当ページの「全行」を読んで1行1行Recheck Condで再評価する必要がある

Heap Blocks: exact=3500 lossy=200  ← 200ページがlossyモード
```

**「lossyでもディスクI/Oは変わらないのでは？」への回答：**

```
ディスクI/O（ページの読み込み回数）は変わらない。
lossyになっても「読むページ数」は増えない。

変わるのは「読んだページの中から何行を捨てるか」= CPU処理。

exact: ページを読んで、該当する2行だけ返す
lossy: ページを読んで、全20行を読み込み、Recheck Condで18行を捨て、2行を返す

→ ディスクI/Oが問題ではなく、CPU処理と返却行数の増加が問題。
  ページ内に不要な行が多いほど、Recheckのオーバーヘッドが増える。

ただし lossy は「ページを読む」前の判断精度が落ちるという点で、
exact では「このページには該当行がない」と分かっていたページも
lossy では「どこかにある可能性」として読んでしまうケースがあれば、
その分だけディスクI/Oが増えることになる。
```

**最適と判断される条件：**
- 選択率が**中程度（目安 5〜20%）**
- 複数インデックスをAND/ORで組み合わせたい場合（BitmapAnd / BitmapOr）

```
BitmapAnd
  ->  Bitmap Index Scan on idx_orders_user_id    ← user_id=42 のビットマップ
  ->  Bitmap Index Scan on idx_orders_status     ← status='pending' のビットマップ
→ 2つのビットマップのAND（論理積）を取ってから、ヒープを読む
→ 複数インデックスを同時に活用できる唯一のスキャン方式
```

---

### スキャンノード選択の全体比較

**選択率とI/Oパターンの整理：**

```
【選択率 低（〜1%）: user_id = 42 で 100行ヒット（全体100万行）】

  Index Scan:
    インデックス数ページ + ヒープ100ページへランダムアクセス → 合計 ~105 ページ読む

  Seq Scan:
    ヒープ全10万ページを順次読む → 合計 10万ページ読む

  → Index Scan が圧倒的に少ないI/O ✅

【選択率 中（10%）: 10万行ヒット（全体100万行）】

  Index Scan:
    インデックス数百ページ + ヒープ最大10万ページへランダムアクセス
    → ランダムI/Oが全体の10万ページ分に達し、Seq Scanと同等かそれ以上

  Bitmap Heap Scan:
    インデックス走査 + ヒープ対象ページを物理順に読む（重複なし）
    → ランダムI/Oを最小化しつつSeq Scanより少ないページを読む ✅

  Seq Scan:
    10万ページを順次読む（read-aheadが効く）

【選択率 高（50%以上）: 50万行ヒット】

  Seq Scan:
    どうせほぼ全ページ読むなら、余計な処理（インデックス走査、ビットマップ構築）
    なしに直接読んだ方がシンプルで速い ✅
```

**コスト比較の概念図：**

```
コスト
  |
  | \  Index Scan（ランダムI/O累積）
  |  \
  |   \___
  |       \___  Bitmap Heap Scan（物理順読みで逓減）
  |           \___________
  |       Seq Scan（一定）\___________
  |_________________________________________→ 選択率
  0%    5%    10%   20%   50%  100%
```

| 選択率の目安 | 推奨スキャン | 決め手となる理由 |
|---|---|---|
| 0.01〜1% | Index Scan / Index Only Scan | ランダムI/Oが少数で収まる。ヒープページ読み込みが最小 |
| 1〜15% | Bitmap Heap Scan | 物理順読みでランダムI/Oを排除。複数インデックスの組み合わせも可能 |
| 15%〜 | Seq Scan | ほぼ全ページを読むなら直接順次読みが最もシンプルかつ高速 |

※ 実際には `random_page_cost`、`correlation`、shared_buffersのキャッシュ状況によって境界は変動する。

---

## 4. 結合ノード詳細：選択基準と比較

### Nested Loop（ネストループ結合）

```
Nested Loop  (cost=0.43..25600.80 rows=1000 width=128)
             (actual time=0.05..320.45 rows=980 loops=1)
  ->  Seq Scan on orders  (actual time=0.02..85.3 rows=1000 loops=1)
  ->  Index Scan using users_pkey on users
        Index Cond: (users.id = orders.user_id)
        (actual time=0.18..0.22 rows=1 loops=1000)  ← 1000回呼ばれている
```

**内部動作：**
```
for each row in outer_table:          ← 外側テーブル（orders）を1行ずつ
    for each matching row in inner_table:  ← 内側テーブル（users）を検索
        if join_condition matches:
            emit combined row
```

**コスト計算：**
```
総コスト ≈ outer_cost + outer_rows × inner_cost_per_lookup
         = 85ms + 1000 × 0.22ms = 85 + 220 = 305ms（実測に近い）
```

**最適と判断される条件：**
- 外側テーブルの行数が**少ない**（数百〜数千行程度）
- 内側テーブルに**インデックス**があり、1回のルックアップコストが低い
- LIMIT句があり、全件取得せずに早期終了できる場合（起動コスト重視）
- 結合後の出力行数が少ないと予想される場合

**問題が出るケース：**
- 外側が10万行 × 内側インデックス検索0.2ms = **20秒** になる
- 外側が多い場合はHash Join / Merge Joinの方が圧倒的に有利

---

### Hash Join（ハッシュ結合）

```
Hash Join  (cost=4820.00..31200.00 rows=500000 width=128)
           (actual time=210.5..4800.3 rows=498000 loops=1)
  Hash Cond: (orders.user_id = users.id)
  ->  Seq Scan on orders  (actual time=0.1..1200 rows=2000000 loops=1)
  ->  Hash  (actual time=205.3..205.3 rows=50000 loops=1)
        Buckets: 65536  Batches: 1  Memory Usage: 3200kB
        ->  Seq Scan on users  (actual time=0.1..180 rows=50000 loops=1)
```

**内部動作（2フェーズ）：**

**フェーズ1（Build phase）：** 内側テーブルをスキャンし、結合キーのハッシュ値をキーとするハッシュテーブルをメモリ上に構築。

**フェーズ2（Probe phase）：** 外側テーブルを1行ずつ読み、結合キーのハッシュ値で内側のハッシュテーブルを探索し、マッチすれば出力。

```
Build:  users全件 → hash_table["user_42"] = {行データ}
Probe:  orders 1行ずつ → hash_table[row.user_id] を参照
```

**Batches（バッチ数）の意味：**
```
Batches: 1   → ハッシュテーブル全体がwork_memに収まった（理想）
Batches: 4   → 4回に分けてディスクを使って処理（遅い）
             → work_mem を増やすとBatches=1になる可能性がある
```

**最適と判断される条件：**
- 両テーブルが**大きく**、Nested Loopのループコストが非現実的な場合
- **等価結合**（`=` 条件）のみ（ハッシュは等値比較のみ対応）
- 内側（build side）がwork_memに収まる規模
- インデックスが存在しない、または選択率が高くインデックスが効かない場合

**build sideの選択：**
プランナは通常、小さい方をbuild side（ハッシュ表を作る側）に選ぶ。
実行計画の `->  Hash -> Seq Scan` が内側（build side）。

---

### Merge Join（マージ結合）

```
Merge Join  (cost=0.87..45200.00 rows=500000 width=128)
  Merge Cond: (orders.user_id = users.id)
  ->  Index Scan using idx_orders_user_id on orders
        (actual time=0.1..1800 rows=2000000 loops=1)
  ->  Index Scan using users_pkey on users
        (actual time=0.05..95 rows=50000 loops=1)
```

**内部動作：**
両方の入力が**結合キーでソートされていること**を前提に、2つのポインタを進めながらマッチを探す。

```
sorted orders: user_id = 1, 1, 2, 3, 5, 5, 5, 8 ...
sorted users:  id      = 1, 2, 3, 4, 5, 6, 7, 8 ...
→ 両方を同時に走査してマッチをemit（ソート済みなので1パスで完結）
```

ソートが必要な場合はSort→Merge Joinになり、ソートコストが追加される。
インデックスがあれば、インデックス順読み出しでソート不要になる。

**最適と判断される条件：**
- 両側に**ソート済みインデックス**が存在する（ソートコストゼロ）
- 両テーブルが大きく、等価結合かつソート済みが保証できる場合
- 範囲結合（`<`, `>`, `BETWEEN`）にも対応（Hash Joinは不可）

**起動コストの特性：**
```
Nested Loop: 起動コスト低い（最初の1行はすぐ返る）
Hash Join:   起動コスト高い（まずbuild phaseが完了してから初行を返す）
Merge Join:  中程度（ソートが必要なら高い、インデックスがあれば低い）
```

---

### 結合アルゴリズムの全体比較

| 観点 | Nested Loop | Hash Join | Merge Join |
|---|---|---|---|
| **最適な外側行数** | 少（〜数千） | 大 | 大 |
| **最適な内側条件** | インデックスあり | なくてよい | ソート済み or インデックス |
| **等価結合** | ✅ | ✅ | ✅ |
| **非等価結合（<, >）** | ✅ | ❌ | ✅（一部） |
| **起動コスト** | 低い | 高い（build完了まで） | 中 |
| **LIMIT との相性** | ◎（早期終了） | △（build全完了が必要） | △ |
| **メモリ消費** | 低い | 高い（build side分） | 低い |
| **ディスクへのスピル** | なし | Batches>1で発生 | Sort時に発生 |

---

## 5. 集約・ソートノード詳細

### 前提知識：集約とソートがなぜ重い処理なのか

スキャンや結合は「入力を受け取りながら出力を流す（パイプライン）」処理だが、
集約とソートは根本的に異なる。

```
パイプライン処理（スキャン・結合）:
  入力1行 → 処理 → 出力1行  を繰り返す
  → 最初の1行は早く返る（起動コスト低い）

集約・ソート処理:
  全入力が揃うまで出力できない（blocking operator）

  Sort の例:
    10万行を全部受け取る → ソート実行 → 先頭行を出力
    → 最初の1行を返すまでに全件処理が必要（起動コストが総コストに近い）

  GROUP BY の例:
    全グループの集計が終わるまで結果を返せない
```

これが `cost=A..B` の A（起動コスト）と B（総コスト）の差が、
ソート・集約ノードで特に大きくなる理由。

---

### Sort ノード

#### 出力例と各フィールドの意味

```
Sort  (cost=12500.82..12750.82 rows=100000 width=72)
      (actual time=980.3..1120.5 rows=100000 loops=1)
  Sort Key: created_at DESC, amount
  Sort Method: quicksort  Memory: 18432kB
  Buffers: shared hit=1823, temp read=0 written=0
```

| フィールド | 意味 |
|---|---|
| `Sort Key` | ソートの基準列と方向（ASC/DESC）。複数列の場合は優先順に列記 |
| `Sort Method` | 使用されたソートアルゴリズム（後述） |
| `Memory` | ソートに使ったメモリ量（Sort Methodがメモリ系の場合） |
| `Disk` | 一時ファイルに書き出したデータ量（Sort Methodがexternal mergeの場合） |

---

#### Sort Method の種類と選択条件

PostgreSQLは入力データ量と `work_mem` に応じて、3つのソートアルゴリズムを使い分ける。

---

**① quicksort（クイックソート）**

```
Sort Method: quicksort  Memory: 18432kB
```

**使われる条件：** データ全体が `work_mem` に収まるとき

**内部動作：**
```
1. 入力行を全件 work_mem 上のバッファに読み込む
2. バッファ上でクイックソートを実行
3. ソート済みの行を先頭から順に返す

→ ディスクアクセスなし。純粋にメモリとCPUだけで完結。
```

**起動コストの特性：**
quicksortはデータを全件読み終えるまで1行も返せない。
そのため、実行計画上の `actual time=980.3..1120.5` を見ると、
起動時間（980ms）と総時間（1120ms）の差が小さい = ほぼ全時間がソートに費やされている。

---

**② top-N heapsort（ヒープソート）**

```
Sort Method: top-N heapsort  Memory: 128kB
```

**使われる条件：** `ORDER BY ... LIMIT N` が指定されており、N件が小さいとき

**内部動作：**
```
目的: 100万行から上位10件だけ取り出したい

全件をソートする必要はない。
「現在の上位10件」を保持するヒープ（優先度付きキュー）を使えばよい。

1. 最初の10行をヒープに入れる
2. 以降の行が来るたびに「ヒープの最小値と比較」
   - 新しい行の方が大きければ、ヒープ最小値を捨てて新しい行を入れる
   - 小さければ捨てる
3. 全行を読み終えたらヒープの中身が上位10件

→ ソートするのは常に「N件のヒープ」だけ。100万行来ても O(n log N) で済む。
→ N が小さいほど Memory が激小（上の例では128kB）
```

**quicksort との比較：**
```
100万行 ORDER BY amount DESC LIMIT 10:

quicksort:      100万行を全件 work_mem に読み込んでソート → 数GB必要な可能性
top-N heapsort: 常に10件のヒープを維持するだけ → 数kBで済む

→ LIMIT付きクエリでは top-N heapsort が選ばれることがパフォーマンス上理想的
```

---

**③ external merge（外部マージソート）**

```
Sort Method: external merge  Disk: 45824kB
Buffers: shared hit=1823, temp read=5728 written=5728
```

**使われる条件：** データ全体が `work_mem` に収まらないとき

**内部動作：**
```
work_mem = 4MB、ソートデータ合計 = 200MB の場合:

フェーズ1（run 生成）:
  4MB分のデータを読む → メモリ上でクイックソート → pgsql_tmp/ に書き出す（run 1）
  次の4MB を読む → メモリ上でクイックソート → pgsql_tmp/ に書き出す（run 2）
  ...これを50回繰り返す（50個の run ファイル）

フェーズ2（マージ）:
  50個のソート済みファイルを同時に読みながら、先頭値を比較してマージ
  → 最終的に1本のソート済みストリームとして出力

  pgsql_tmp/pgsort_XXXXX_1  → ─┐
  pgsql_tmp/pgsort_XXXXX_2  → ─┤
  ...                           ├─→ マージ → クライアントへ
  pgsql_tmp/pgsort_XXXXX_50 → ─┘
```

**ディスクI/Oの内訳：**
```
Disk: 45824kB の場合:
  temp written=5728  → フェーズ1でrunファイルを 5728ページ（=45MB）書き出した
  temp read=5728     → フェーズ2でrunファイルを 5728ページ読み込んだ

書き出し量 ≒ 読み込み量 になるのが典型パターン。
ディスクに1回書いて1回読む分のオーバーヘッドが追加される。

work_mem が極端に小さいと、マージパスが複数回（multi-pass）になり
temp read/written が written の数倍に膨らむこともある。
```

**external merge を解消するには：**
```sql
-- セッションレベルで work_mem を増やす
SET work_mem = '256MB';

-- または ORDER BY が不要な集計を見直す（後述）

-- 実際に何MBあれば収まるかは Disk: の値を目安に
-- Disk: 45824kB → ソートデータ約 45MB
-- work_mem を 64MB 程度に増やせば収まる可能性が高い
```

---

#### Sort ノードが発生する場面

Sort ノードは `ORDER BY` だけでなく、次の処理でも自動的に挿入される。

```sql
-- 1. ORDER BY
SELECT * FROM orders ORDER BY created_at DESC;
→ Sort (Sort Key: created_at DESC)

-- 2. Merge Join の前処理（両入力をソート済みにする必要がある）
SELECT * FROM orders o JOIN users u ON o.user_id = u.id;
→ Sort (Sort Key: o.user_id)
→ Sort (Sort Key: u.id)
→ Merge Join

-- 3. GroupAggregate の前処理
SELECT user_id, COUNT(*) FROM orders GROUP BY user_id;
→ Sort (Sort Key: user_id)
→ GroupAggregate

-- 4. DISTINCT の実装（Unique ノードとペア）
SELECT DISTINCT status FROM orders;
→ Sort (Sort Key: status)
→ Unique

-- 5. ウィンドウ関数
SELECT user_id, ROW_NUMBER() OVER (PARTITION BY status ORDER BY created_at) FROM orders;
→ Sort (Sort Key: status, created_at)
→ WindowAgg
```

インデックスがあれば Sort ノードが省略される場合がある（後述）。

---

### Aggregate ノード

#### Aggregate（単純集計）

```
Aggregate  (cost=18340.00..18340.01 rows=1 width=8)
           (actual time=1823.5..1823.5 rows=1 loops=1)
  ->  Seq Scan on orders  ...
```

`GROUP BY` なしの `COUNT(*)`、`SUM(amount)` など。
全入力を処理して**1行**だけ返す最もシンプルな集計ノード。

```
処理フロー:
accumulator = 0
for row in input:
    accumulator += row.amount    ← 逐次積み上げ
emit accumulator                 ← 最後に1行返す
```

---

### HashAggregate ノード

#### 出力例

```
HashAggregate  (cost=18340.00..18345.00 rows=500 width=12)
               (actual time=1820.3..1821.1 rows=500 loops=1)
  Group Key: status, region
  Batches: 1  Memory Usage: 512kB
  Buffers: shared hit=1823
  ->  Seq Scan on orders  (rows=1000000)
```

#### 内部動作

```
処理フロー:
hash_table = {}   ← work_mem 上のハッシュテーブル

for row in input:
    key = (row.status, row.region)          ← GROUP BY のキー
    h   = hash(key)                         ← ハッシュ値を計算
    if h in hash_table:
        hash_table[h].count += 1            ← 既存グループに集計
        hash_table[h].sum   += row.amount
    else:
        hash_table[h] = {count: 1, sum: row.amount}  ← 新グループ作成

for group in hash_table:
    emit group                              ← 全入力後にまとめて出力
```

**メモリ消費の構造：**
```
ハッシュテーブルのサイズ ≈ グループ数 × (キー列のバイト数 + 集計値のバイト数)

グループ数 = 500、1エントリ = 100バイト の場合:
  500 × 100B = 50KB → work_mem(4MB) に余裕で収まる → Batches: 1

グループ数 = 100万、1エントリ = 100バイト の場合:
  100万 × 100B = 100MB → work_mem(4MB) を超える → Batches > 1
```

#### Batches の意味と影響

```
Batches: 1  Memory Usage: 512kB
  → ハッシュテーブル全体が work_mem に収まった。ディスク使用なし ✅

Batches: 4  Memory Usage: 4096kB  Disk Usage: 15000kB
  → 4バッチに分けてディスクを使って処理 ❌
```

**Batches > 1 のとき何が起きているか：**
```
全グループを一度にメモリに乗せられない場合、
入力データをハッシュ値で N 個のパーティションに分割し、
パーティションごとに一時ファイルへ書き出してから順番に処理する。

Batches: 4 の場合:
  パス1: ハッシュ値 0〜25% のグループのみメモリに乗せて集計 → 結果出力
  パス2: ハッシュ値 25〜50% → 集計 → 結果出力
  パス3: ハッシュ値 50〜75% → 集計 → 結果出力
  パス4: ハッシュ値 75〜100% → 集計 → 結果出力

→ 入力データを4回スキャンに近い処理になる（実際は一時ファイル経由）
→ temp read/written が発生し、大幅に遅くなる
```

---

### GroupAggregate ノード

#### 出力例

```
GroupAggregate  (cost=12500.82..15000.82 rows=500 width=12)
                (actual time=980.3..1180.5 rows=500 loops=1)
  Group Key: status, region
  ->  Sort  (cost=12500.82..12750.82 rows=100000 width=24)
              Sort Key: status, region
              Sort Method: quicksort  Memory: 18432kB
        ->  Seq Scan on orders  ...
```

#### 内部動作

**前提：入力が GROUP BY キーでソート済みであること**

```
処理フロー（ソート済み入力を仮定）:

入力ストリーム（status, region でソート済み）:
  ('active', 'Tokyo',  100)
  ('active', 'Tokyo',  200)   ← 同じグループが連続
  ('active', 'Osaka',  150)
  ('pending', 'Tokyo', 300)
  ...

current_group = None
accumulator   = {}

for row in sorted_input:
    key = (row.status, row.region)
    if key != current_group:
        if current_group is not None:
            emit current_group, accumulator  ← グループが変わったら出力
        current_group = key
        accumulator   = {count: 0, sum: 0}
    accumulator.count += 1
    accumulator.sum   += row.amount
emit current_group, accumulator              ← 最後のグループを出力
```

**HashAggregate との本質的な違い：**
```
HashAggregate:
  全グループをメモリのハッシュテーブルに保持する必要がある
  → グループ数 × エントリサイズ 分の work_mem が必要

GroupAggregate:
  「今処理中のグループ」のaccumulatorだけメモリに保持
  → グループがいくつあっても、メモリ消費はO(1)で一定
  ただし、事前にソートが必要
```

#### Sort が省略される場合（インデックスとの組み合わせ）

```sql
-- GROUP BY user_id で、user_id のインデックスがある場合
SELECT user_id, COUNT(*) FROM orders GROUP BY user_id;
```

```
GroupAggregate
  ->  Index Only Scan using idx_orders_user_id on orders
        ← インデックスが user_id 順に行を返すため、Sort ノードが不要
```

インデックスが `GROUP BY` のキー順に行を返せるとき、Sort ノードが省略され
そのまま GroupAggregate に流れる。これは非常に効率的なパターン。

---

### HashAggregate vs GroupAggregate の選択基準

```
プランナが比較するコスト:

HashAggregate のコスト:
  = スキャンコスト + ハッシュ構築コスト（+ Batches>1なら一時ファイルコスト）
  
GroupAggregate のコスト:
  = スキャンコスト + Sort コスト（外部ソートなら高い）+ 逐次集計コスト
    ただしインデックスでソート済みなら Sort コスト = 0
```

**実践的な選択の目安：**

| 状況 | 有利なノード | 理由 |
|---|---|---|
| グループ数が少ない（〜数千） | HashAggregate | ハッシュテーブルが小さくwork_memに収まる |
| グループ数が多い（〜数百万） | GroupAggregate | ハッシュテーブルが巨大になりBatches発生を回避 |
| GROUP BY列にインデックスあり | GroupAggregate | Sort不要でそのまま集計できる |
| GROUP BY列にインデックスなし・大テーブル | HashAggregate | Sortコストを払うよりハッシュの方が速い |
| work_mem が十分大きい | HashAggregate | Batches=1が保証されるなら高速 |
| ORDER BY + GROUP BY が同一列 | GroupAggregate | ソートが集計と兼用できる |

---

### Unique ノード

#### 出力例

```
Unique  (cost=12500.82..13000.82 rows=50 width=8)
        (actual time=980.3..1005.2 rows=50 loops=1)
  ->  Sort  (cost=12500.82..12750.82 rows=100000 width=8)
              Sort Key: status
        ->  Seq Scan on orders  ...
```

#### 内部動作

`DISTINCT` の実装。**ソート済みの入力から重複を除去する**。

```
処理フロー:
前提: 入力が DISTINCT キーでソート済み

  ('active')
  ('active')   ← 前と同じ → 捨てる
  ('active')   ← 前と同じ → 捨てる
  ('pending')  ← 前と違う → 出力
  ('pending')  ← 前と同じ → 捨てる
  ('done')     ← 前と違う → 出力

→ 直前の値と比較するだけ。ソートさえ済んでいれば O(n) の単純処理
```

**HashAggregate による DISTINCT の代替：**
プランナは `DISTINCT` を `HashAggregate` で実装することもある。

```
HashAggregate  (cost=18340.00..18340.50 rows=50 width=8)
  Group Key: status
  ->  Seq Scan on orders  ...

→ Sort → Unique の代わりに、ハッシュで重複排除
→ グループ数（distinct値数）が少ないときはこちらが有利
```

---

### WindowAgg ノード

#### 出力例

```sql
SELECT user_id,
       amount,
       SUM(amount) OVER (PARTITION BY user_id ORDER BY created_at) AS running_total
FROM orders;
```

```
WindowAgg  (cost=12500.82..16500.82 rows=100000 width=80)
           (actual time=980.3..2100.5 rows=100000 loops=1)
  ->  Sort  (cost=12500.82..12750.82 rows=100000 width=72)
              Sort Key: user_id, created_at
        ->  Seq Scan on orders  ...
```

#### 内部動作

```
前提: PARTITION BY user_id ORDER BY created_at でソート済み

処理フロー:
  ユーザー1のパーティション:
    (user_id=1, amount=100, created_at=1/1) → running_total = 100
    (user_id=1, amount=200, created_at=1/5) → running_total = 300
    (user_id=1, amount=150, created_at=2/1) → running_total = 450
  
  ユーザー2のパーティション（PARTITION BY で区切られる）:
    (user_id=2, amount=500, created_at=1/3) → running_total = 500
    ...

→ Sort → WindowAgg の順で、
  「PARTITION BY + ORDER BY の組み合わせ」でソートしてから1パスで集計
```

**Sort Key の読み方：**
```
Sort Key: user_id, created_at

PARTITION BY user_id  → Sort Key の先頭列 = user_id
ORDER BY created_at   → Sort Key の後続列 = created_at

PARTITION BY と ORDER BY が合わさって1つの Sort Key になる
```

**複数ウィンドウ関数がある場合：**

```sql
SELECT
  SUM(amount)   OVER (PARTITION BY user_id ORDER BY created_at),
  COUNT(*)      OVER (PARTITION BY user_id ORDER BY created_at),  ← 同じパーティション
  MAX(amount)   OVER (PARTITION BY status)                        ← 別パーティション
FROM orders;
```

```
WindowAgg   ← PARTITION BY status の MAX
  ->  Sort  (Sort Key: status)
        ->  WindowAgg   ← PARTITION BY user_id ORDER BY created_at の SUM, COUNT
              ->  Sort  (Sort Key: user_id, created_at)
                    ->  Seq Scan on orders

→ 同一パーティション・ORDER BYの関数はまとめて1つの WindowAgg ノードで処理
→ 異なるパーティションはSort + WindowAgg を積み重ねる
→ ウィンドウ定義が増えるほどSort回数が増える → できるだけ定義を統一する
```

---

### Limit ノード

```
Limit  (cost=0.43..8.45 rows=10 width=72)
       (actual time=0.082..0.312 rows=10 loops=1)
  ->  Index Scan using idx_orders_created on orders
        Index Cond: (created_at > '2024-01-01')
        (actual time=0.078..0.278 rows=10 loops=1)
```

Limit は子ノードから必要な行数だけ取り出して処理を止める。
子ノードが **パイプライン処理（Index Scan 等）** の場合、
必要行数が揃った時点で子ノードの実行が打ち切られる。

```
→ actual rows=10 で打ち切られた Index Scan は
  全件スキャンせず 10件目を見つけた時点で終了する

→ cost の B 値（総コスト）は全件取得した場合のコストだが、
  実際には Limit で早期終了するため、actual time は大幅に小さくなる
```

**Limit + Sort の場合は top-N heapsort が使われる（Sort ノードの節参照）。**

---

### 集約・ソートノード 全体比較

| ノード | ソートが必要か | メモリ消費 | ディスクスピル | 最適な状況 |
|---|---|---|---|---|
| Aggregate | 不要 | O(1) | なし | GROUP BYなし集計 |
| HashAggregate | 不要 | グループ数に比例 | Batches>1で発生 | グループ数少〜中、インデックスなし |
| GroupAggregate | **必要** | O(1) | Sortノードで発生 | グループ数多い、インデックスあり |
| Sort（quicksort） | — | 全データ分 | なし | work_memに収まる場合 |
| Sort（top-N） | — | N件分のみ | なし | ORDER BY + LIMIT N |
| Sort（external merge） | — | work_mem分 | **常に発生** | work_mem不足時（改善必要） |
| Unique | **必要** | O(1) | Sortノードで発生 | DISTINCT、ソート済み入力 |
| WindowAgg | **必要** | パーティション分 | Sortノードで発生 | ウィンドウ関数 |

---

## 6. Buffersオプション完全解説

### PostgreSQLのメモリ・ディスク構造

```
┌─────────────────────────────────────────────────────────┐
│                    PostgreSQL プロセス                     │
│                                                           │
│  ┌───────────────────────────────────────────────────┐  │
│  │             shared_buffers（共有メモリ）            │  │
│  │  全バックエンドプロセスが共有するページキャッシュ   │  │
│  │  デフォルト: 128MB（推奨: 物理メモリの25%）         │  │
│  │                                                    │  │
│  │  [page][page][page]...[page]  ← 8KBページ単位      │  │
│  └───────────────────────────────────────────────────┘  │
│                                                           │
│  ┌──────────────────────────────────────────────────┐   │
│  │    work_mem（各クエリ操作ごとのプライベートメモリ）  │   │
│  │  Sort, HashJoin, HashAggregate ごとに確保         │   │
│  │  デフォルト: 4MB（並列クエリは操作×ワーカー数分）  │   │
│  └──────────────────────────────────────────────────┘   │
│                                                           │
└─────────────────────────────────────────────────────────┘
               ↕ OS buffer cache（カーネル管理）
┌─────────────────────────────────────────────────────────┐
│                   ディスク（データファイル）                 │
│  base/16384/xxxxx  ← テーブルファイル（ヒープ）            │
│  base/16384/xxxxx  ← インデックスファイル                  │
│  pg_wal/           ← WAL（書き込み先行ログ）               │
│  pgsql_tmp/        ← 一時ファイル（work_mem溢れ）          │
└─────────────────────────────────────────────────────────┘
```

### Buffers 種別の完全解説

```
Buffers: shared hit=8120 read=340 dirtied=15 written=8
         local hit=0 read=0 dirtied=0 written=0
         temp read=2400 written=2400
```

---

#### `shared hit`（共有バッファヒット）

**発生する処理：** テーブルまたはインデックスのページ読み込み全般  
**場所：** shared_buffers（共有メモリ上）  
**意味：** 要求したページがすでにshared_buffersにキャッシュされていた  

```
shared hit = 8120
→ 8120ページ（× 8KB = 約63MB）をメモリから読んだ
→ ディスクI/Oなし。最速。
```

**理想状態：** hit / (hit + read) ≈ **0.95以上**

---

#### `shared read`（共有バッファミス→ディスク読み込み）

**発生する処理：** テーブルまたはインデックスのページ読み込みで、キャッシュミス時  
**場所：** shared_buffers にないため、ディスク（またはOSバッファキャッシュ）から読む  
**意味：** shared_buffersにページがなかったため、ディスクから読み込んだ  

```
shared read = 340
→ 340ページ（約2.7MB）をディスクから読んだ
→ ディスクI/O発生。readの多さがボトルネックになりうる。
```

**対処：** `shared_buffers` を増やすか、よく使うテーブルをキャッシュに乗せるようクエリ頻度を上げる。

**注意：** `shared read` でも、OSカーネルのバッファキャッシュに乗っていれば実際はメモリから読んでいる場合がある（PostgreSQLからは「ディスク読み」に見える）。

---

#### `shared dirtied`（共有バッファ上の更新）

**発生する処理：** `UPDATE`, `DELETE`, `INSERT` などデータ変更操作  
**場所：** shared_buffers 上のページを変更（ダーティページ化）  
**意味：** shared_buffers上のページを変更したが、まだディスクへ書き出していない  

```
shared dirtied = 15
→ 15ページを変更し、ダーティ状態にした
→ このページはcheckpointかbgwriterが後でディスクへ書き出す
```

---

#### `shared written`（共有バッファからディスクへの書き出し）

**発生する処理：** クエリ実行中にshared_buffersが不足し、古いダーティページを追い出す際  
**場所：** shared_buffers → ディスク  
**意味：** このクエリ実行中に、バッファ圧迫でページをディスクへ書き出した  

```
shared written = 8
→ クエリ実行中に8ページを強制書き出し
→ 通常は0が望ましい。多い場合はshared_buffersが不足している
```

---

#### `local hit` / `local read`（ローカルバッファ）

**発生する処理：** **一時テーブル**（`CREATE TEMP TABLE`）や **未ログテーブル**（`UNLOGGED TABLE`）へのアクセス  
**場所：** セッション専用のローカルバッファ（shared_buffersとは別）  
**意味：** 一時テーブルのページキャッシュのヒット／ミス  

```sql
CREATE TEMP TABLE temp_result AS SELECT ...;
-- このSELECT: Buffers: local read=50（一時テーブルのページを読んだ）
```

一時テーブルを多用するクエリで `local read` が多い場合、`temp_buffers`（デフォルト8MB）の増加を検討。

---

#### `temp read` / `temp written`（一時ファイル）⚠️ 最重要警告

**発生する処理：** Sort / Hash Join / HashAggregate が `work_mem` に収まらず、ディスクにスピルした時  
**場所：** `pg_catalog.pg_temp_XXXXX` ディレクトリ（`$PGDATA/pgsql_tmp/`）  
**意味：** ソートやハッシュのデータをディスクの一時ファイルに書き出し・読み込みした  

```
temp read=2400 written=2400
→ 2400ページ（約18MB）をwork_mem溢れにより一時ファイルで処理
→ ディスクへの書き出しと読み込みが1セット発生している
→ パフォーマンスへの影響が非常に大きい
```

**work_memとの関係：**
```
Sort が temp written を出す条件：
  ソートする総データ量 > work_mem
  例: 100万行 × 200バイト = 約200MB > work_mem(4MB) → 一時ファイルへ
```

**どのノードで発生しているか確認：**
```
Sort  (cost=... rows=... width=...)
  Sort Method: external merge  Disk: 45000kB   ← Sortによるtemp
  Buffers: temp read=5625 written=5625

Hash  (... Batches: 8 ...)                     ← Hash Joinによるtemp
  Buffers: temp read=1200 written=1200
```

**対処：**
```sql
SET work_mem = '256MB';  -- セッションレベルで一時的に増やす
-- または特定クエリにのみ適用
BEGIN;
SET LOCAL work_mem = '512MB';
SELECT ...;
COMMIT;
```

---

### Buffers 値の総合的な読み方

```
実行計画に Buffers が出る例：

Hash Join  (actual time=205..4800 rows=498000 loops=1)
  Buffers: shared hit=62000 read=15000    ← ① 結合全体のバッファ合計
  ->  Seq Scan on orders
        Buffers: shared hit=50000 read=12000  ← ② ordersのスキャン分
  ->  Hash
        Buffers: shared hit=12000 read=3000   ← ③ usersのスキャン分
        ->  Seq Scan on users
              Buffers: shared hit=12000 read=3000
```

**親ノードの Buffers は子ノードを含む累積値。**
各ノードの実質的なバッファ消費は `親の値 - 子の値の合計` で求まる。

---

## 7. ループを含む実行計画の手続き的読み方

### 具体的な実行計画例

```sql
EXPLAIN ANALYZE
SELECT u.name, o.order_id, o.amount
FROM users u
JOIN orders o ON u.id = o.user_id
WHERE u.region = 'Tokyo'
  AND o.amount > 10000;
```

```
Nested Loop  (cost=0.43..8920.50 rows=450 width=72)
             (actual time=0.082..215.3 rows=432 loops=1)
  Buffers: shared hit=4820 read=210
  ->  Seq Scan on users u  (cost=0.00..850.00 rows=500 width=40)
                            (actual time=0.015..28.5 rows=500 loops=1)
        Filter: (region = 'Tokyo')
        Rows Removed by Filter: 4500
        Buffers: shared hit=680 read=20
  ->  Index Scan using idx_orders_user_id on orders o
                            (cost=0.43..16.12 rows=1 width=32)
                            (actual time=0.350..0.366 rows=0.86 loops=500)
        Index Cond: (o.user_id = u.id)
        Filter: (o.amount > 10000)
        Rows Removed by Filter: 3
        Buffers: shared hit=4140 read=190
```

### この実行計画を手続き的に読む

---

**ステップ1：最初に実行されるノード（最深部）を特定する**

```
最も深いノード = "Seq Scan on users u"
  ↓
まずこれが実行される
```

---

**ステップ2：Seq Scan on users の処理**

```
処理内容：
  usersテーブルの全ページをシーケンシャルに読む
  各行に対して Filter: (region = 'Tokyo') を評価

実測値：
  実際行数 = 500行（Tokyoの人だけ）
  除外行数 = 4500行（Filter除去）
  loops = 1（1回だけ実行）

バッファ：
  shared hit=680（680ページをキャッシュから読んだ）
  read=20（20ページをディスクから読んだ）
```

処理フロー（疑似コード）：
```python
tokyo_users = []
for page in users.heap_pages:           # 680+20=700ページを順次読む
    for row in page.rows:
        if row.region == 'Tokyo':       # Filter評価
            tokyo_users.append(row)     # 500行がここに入る
        # else: Rows Removed by Filter にカウント
```

---

**ステップ3：Nested Loop の処理開始**

Seq Scan が1行返すたびに、Nested Loop は次の処理を行う（パイプライン実行）。

```
Nested Loopは「Seq Scanが全件返し終わるのを待つ」のではなく、
Seq Scanが1行返すたびに即座に内側（Index Scan）を実行する。
```

処理フロー（疑似コード）：
```python
result = []
for user_row in seq_scan_users():       # ← 外側ループ（500回）
    # この時点でuser_rowが1行手に入る
    for order_row in index_scan_orders(user_id=user_row.id):
        # idx_orders_user_idを使ってuser_idでルックアップ
        if order_row.amount > 10000:    # Filter評価
            result.append(merge(user_row, order_row))
```

---

**ステップ4：Index Scan on orders の処理（内側ループ）**

```
処理内容：
  idx_orders_user_idインデックスを使い user_id = u.id で検索
  取得した行に対し Filter: (o.amount > 10000) を評価

実測値：
  actual rows = 0.86  ← 1ループあたり平均0.86行がヒット（= 総432行 / 500ループ）
  loops = 500         ← usersの取得行数分だけ呼ばれた
  Rows Removed by Filter: 3  ← 1ループあたり平均3行を金額フィルタで除外

合計処理：
  インデックスルックアップ = 500回
  合計取得行（フィルタ前）= (0.86 + 3) × 500 = 1930行
  最終出力 = 0.86 × 500 = 430行（≒ 実測432行）
```

---

**ステップ5：Nested Loop からの出力**

```
最終出力：432行
  （userの500行 × 平均0.86行ヒット = 432行）

total loops時間：
  Index Scanの1ループ時間 = 0.366ms
  500ループ = 0.366 × 500 = 183ms
  Seq Scan時間 = 28.5ms
  合計 ≈ 211.5ms（実測 215.3ms と一致）
```

---

### 読み方サマリー

```
実行計画ツリーの手続き的読み方：

1. 最も深いノードが「最初の処理」
2. 親ノードは子ノードの出力を1行ずつ受け取りながら処理（パイプライン）
3. loopsの数 = 親ノードが何回この子を呼んだか
4. actual rows は 1ループあたりの行数 → ×loops で総処理行数
5. actual time は 1ループあたりの時間 → ×loops で総処理時間
6. 子ノードのactual timeは「その子以下の全処理時間」を含む
```

---

### ループが多段になる場合

```sql
SELECT p.name, o.order_id, oi.product_name
FROM users u
JOIN orders o ON u.id = o.user_id
JOIN order_items oi ON o.id = oi.order_id
WHERE u.region = 'Tokyo';
```

```
Nested Loop [A]  (actual time=... rows=5000 loops=1)
  ->  Nested Loop [B]  (actual time=... rows=500 loops=1)
        ->  Seq Scan on users  (rows=500 loops=1)
        ->  Index Scan on orders  (rows=1 loops=500)    ← Bの内側: 500回
  ->  Index Scan on order_items  (rows=10 loops=500)   ← Aの内側: 500回
```

```
実行順序と回数：
1. Seq Scan on users: 1回実行 → 500行出力
2. Index Scan on orders: 500回実行（usersの各行に対して）→ 計500行出力
3. Nested Loop [B]: 500行出力（ordersとusersの結合結果）
4. Index Scan on order_items: 500回実行（[B]の各行に対して）→ 計5000行出力
5. Nested Loop [A]: 5000行出力（最終結果）
```

---

## 8. パフォーマンス改善チェックリスト

### ✅ 良いサイン

| サイン | 理由 |
|---|---|
| `Index Only Scan` with `Heap Fetches: 0` | ヒープアクセス完全不要 |
| `Bitmap Heap Scan` with `Heap Blocks: exact=N lossy=0` | lossy不要＝work_mem十分 |
| `shared hit / (shared hit + shared read)` > 0.95 | キャッシュヒット率95%超 |
| `rows` 推定と `actual rows` の乖離が 2〜3倍以内 | 統計が正確でプランが信頼できる |
| `Sort Method: quicksort` or `top-N heapsort` | メモリ内ソート完了 |
| `Hash Batches: 1` | ハッシュテーブルがwork_memに収まっている |
| Nested Loop の内側が `Index Scan` で loops が少ない | ループコストが低い |
| `temp read=0 written=0` | work_mem溢れなし |

---

### ❌ 悪いサイン・要注意

#### 🔴 temp read/written が 0 以外

```
Buffers: temp read=5625 written=5625
Sort Method: external merge  Disk: 45000kB
```
→ work_mem 不足。`SET work_mem = '256MB';` で試す。

#### 🔴 推定行数と実際行数の大きな乖離（10倍以上）

```
rows=100  actual rows=80000   ← 800倍の乖離
```
→ プランナの判断が誤っている。`ANALYZE`、statistics target増加、または `pg_statistic` の確認。

#### 🔴 Seq Scan + 大量の Rows Removed by Filter

```
Seq Scan on orders
  Filter: (status = 'pending')
  Rows Removed by Filter: 1998000  ← 全体の99.9%を捨てている
```
→ インデックスが機能していない。インデックス作成または `random_page_cost` の見直し。

#### 🔴 Nested Loop の外側が大量行

```
Nested Loop  (loops=1)
  ->  Seq Scan  (rows=50000)            ← 外側50000行
  ->  Index Scan  (loops=50000)         ← 50000回のルックアップ
```
→ `SET enable_nestloop = off;` で一時的にHash Joinを強制して比較検証できる。

#### 🔴 Hash Join の Batches > 1

```
Batches: 8  Memory Usage: 4096kB  ← 8バッチ = 7回余分にディスクI/O
```
→ `work_mem` を増やしてBatches=1を目指す。

#### 🔴 Index Scan の Rows Removed by Filter が多い

```
Index Scan using idx_orders_user_id on orders
  Index Cond: (user_id = 42)
  Filter: (amount > 1000)
  Rows Removed by Filter: 9800   ← インデックスで絞れていない
```
→ `(user_id, amount)` の複合インデックスに変更してフィルタをIndex Condに昇格させる。

---

## 9. 実例：改善前後の比較

### クエリ

```sql
SELECT user_id, SUM(amount)
FROM orders
WHERE created_at > '2024-01-01'
GROUP BY user_id
ORDER BY SUM(amount) DESC;
```

### ❌ 改善前

```
Sort  (actual time=4800..4802 rows=5000 loops=1)
  Sort Key: (sum(amount)) DESC
  Sort Method: external merge  Disk: 2800kB
  Buffers: shared hit=5000 read=13000 temp read=350 written=350
  ->  HashAggregate  (actual time=3200..3400 rows=5000 loops=1)
        Batches: 1  Memory Usage: 512kB
        Buffers: shared hit=5000 read=13000
        ->  Seq Scan on orders  (actual time=0.1..2100 rows=800000 loops=1)
              Filter: (created_at > '2024-01-01')
              Rows Removed by Filter: 200000
              Buffers: shared hit=5000 read=13000
```

**問題点：**
- `shared read=13000`（ほぼ全ページがキャッシュミス）
- `temp written=350`（Sortがwork_mem溢れ）
- Seq Scan で 200000行を捨てている

### ✅ 改善後

```sql
CREATE INDEX idx_orders_created_at ON orders(created_at) INCLUDE (user_id, amount);
SET work_mem = '128MB';
```

```
Sort  (actual time=320..322 rows=5000 loops=1)
  Sort Key: (sum(amount)) DESC
  Sort Method: quicksort  Memory: 480kB
  Buffers: shared hit=8200 read=0 temp read=0 written=0
  ->  HashAggregate  (actual time=180..220 rows=5000 loops=1)
        Batches: 1  Memory Usage: 512kB
        Buffers: shared hit=8200 read=0
        ->  Index Only Scan using idx_orders_created_at on orders
              Index Cond: (created_at > '2024-01-01')
              Heap Fetches: 0
              Buffers: shared hit=8200 read=0
```

**改善効果：4800ms → 322ms（約15倍高速化）**
- `shared read` が 13000 → 0（完全キャッシュヒット）
- `temp` が 350 → 0（work_mem内でソート完結）
- `Index Only Scan` で Heap Fetches=0（ヒープアクセス完全排除）

---

## 10. 便利なオプションまとめ

```sql
-- 最も詳細な情報
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, FORMAT TEXT) SELECT ...;

-- JSON形式（ビジュアライザ向け）
EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) SELECT ...;

-- WALの書き込み量も確認（更新系クエリ）
EXPLAIN (ANALYZE, BUFFERS, WAL) UPDATE orders SET status = 'done' WHERE id = 1;
```

### 外部ツール

| ツール | URL | 用途 |
|---|---|---|
| depesz explain | https://explain.depesz.com | テキスト形式の可視化 |
| pgMustard | https://www.pgmustard.com | JSON形式の詳細分析 |
| PEV2 | https://dalibo.github.io/pev2 | JSON形式のビジュアライザ |
| auto_explain | postgresql.conf 拡張 | 遅いクエリの実行計画を自動ログ |

```sql
-- auto_explain の設定例（postgresql.conf）
shared_preload_libraries = 'auto_explain'
auto_explain.log_min_duration = 1000   -- 1秒以上のクエリを記録
auto_explain.log_analyze = on
auto_explain.log_buffers = on
```

---

*PostgreSQL 16 時点の情報をベースに作成。*
