# インデックスと実行計画（実践編）
## 〜インデックスを作り、EXPLAINで効果を確かめる〜

17-1 では「なぜインデックスが速いのか」を物理構造から見ました。ここでは実際に**インデックスを作り**、**`EXPLAIN` で効果を確認する**手順を身につけます。

[参考：fujitsu パフォーマンスチューニング9つの技 ～「探し」について～](https://global.fujitsu/ja-jp/local/software/postgres/tuningrule9-search)

> **この資料の前提**
> - PostgreSQL 17。掲載している実行計画・ページ数・実行時間は**すべて実測値**です。手元で再現できます。
> - テーブルは [[17_indexと実行計画で使用するテーブル|17_indexと実行計画で使用するテーブル]] で作成したもの（`customers` 10万件 = 1,235ページ / `orders` 100万件 = 7,255ページ）。
> - この**ページ数（1,235 と 7,255）が全編を通じた基準値**です。`Buffers` がこの数字と一致したら全件走査です。

---

## 1. インデックスの作り方

### 1-1. 基本形

```sql
CREATE INDEX idx_customers_city ON customers (city);
```

命名は自由ですが、`idx_テーブル名_列名` にしておくと後から読めます。何も指定しなければ **B-Tree** が作られます（17-1 の通り、実務ではほぼこれです）。

作ったら**必ず統計情報を更新**します。

```sql
ANALYZE customers;
```

### 1-2. 4つの型を使い分ける

| 種類 | 何をするもの | こんな時に使う |
| :--- | :--- | :--- |
| **単一列** | 1つの列に作る基本形 | `WHERE city = 'City_123'` |
| **複合** | 複数列をセットで作る。**列の順番が超重要** | `WHERE customer_id = 1 AND order_date >= '...'` |
| **カバリング** | 取得したい列もインデックスに含める | `Index Only Scan` を狙いたい |
| **関数** | 列を加工した結果に作る | `WHERE LOWER(email) = '...'` |

**① 単一列インデックス**

```sql
CREATE INDEX idx_customers_city ON customers (city);
```

**② 複合インデックス**

```sql
-- customer_id で絞り、その中で order_date で絞るクエリに有効
CREATE INDEX idx_orders_cust_date ON orders (customer_id, order_date);
```

`(A, B)` の順で作ると、`WHERE A = ?` や `WHERE A = ? AND B = ?` では効きますが、**`WHERE B = ?` だけでは効きません**。「辞書で2文字目だけ分かっていても引けない」のと同じです。

![[17-2_複合インデックスの並び方.excalidraw]]

**辿り方は 17-1 §5-1 の図とまったく同じです。** ノードに書いてある境目の値と比べて、左右どちらかへ降りていきます。

違うのは**比べる対象**です。**`(苗字, 名前)` をセットにした1つの値**として大小を比べています。`(鈴木, 一郎)` は `(佐藤, 花子)` より大きく、`(田中, 太郎)` より小さい ── という具合です。

だから**まず「苗字」でソートされ、同じ苗字の中で「名前」でソートされている**形になります。複合キーは「苗字のインデックス」と「名前のインデックス」を2本持っているのとは違います。

そして名前だけを指定しても、**その名前が木のあちこちに散っている**ので、辿る手がかりになりません。これが「`WHERE B = ?` だけでは効かない」の中身です。

**③ カバリングインデックス（`INCLUDE`）**

```sql
-- customer_id をキーにし、order_date もインデックスに含める
CREATE INDEX idx_orders_cust_cover ON orders (customer_id) INCLUDE (order_date);
```

`INCLUDE` の列は検索条件には使えませんが、**取得する値として使える**ので、テーブル本体を読まずに済みます（`Index Only Scan`）。

**④ 関数インデックス**

```sql
-- WHERE句で使う式と「まったく同じ式」で作る
CREATE INDEX idx_customers_email_lower ON customers (LOWER(email));
```

### 1-3. インデックス作成はタダではない：`CONCURRENTLY`

ここまで気軽に `CREATE INDEX` を実行してきましたが、**通常の `CREATE INDEX` は対象テーブルをロックし、作成が終わるまで書き込み（`INSERT` / `UPDATE` / `DELETE`）を止めます。**

練習用の小さなテーブルなら一瞬で終わるので気づきませんが、行数が多いテーブルほど作成には時間がかかります。稼働中のサービスで何気なく実行すると、**その間ずっと更新処理が詰まります。**

そのため本番では `CONCURRENTLY` を付けて、ロックを取らずに作ります。

```sql
CREATE INDEX CONCURRENTLY idx_customers_city ON customers (city);
```

ただしロックを取らない代わりに、テーブルを**2回スキャン**するので通常より時間がかかり、**トランザクションブロックの中では実行できません**。失敗したときの後始末など細かい作法もあるので、気を付けましょう。

---

## 2. EXPLAIN の基本

### 2-1. `EXPLAIN` と `EXPLAIN ANALYZE`

```sql
-- ① 予測だけを見る（クエリは実行されない）
EXPLAIN SELECT * FROM customers WHERE city = 'City_123';

-- ② 実際に実行して、実測値も見る
EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM customers WHERE city = 'City_123';
```

*   **`ANALYZE`**: 実際に実行して実測値（`actual time`, 実際の行数）を出す。
*   **`BUFFERS`**: **読んだページ数**を出す。ここが本質なので、**常に付けてください。**

> ⚠️ **`ANALYZE` は「実際にクエリを実行する」オプションです。**
> `SELECT` なら安全ですが、`UPDATE` / `DELETE` / `INSERT` に付けると**本当にデータが変わります**。更新系で実行計画を見たいときは、必ずトランザクションで囲んで戻してください。
> ```sql
> BEGIN;
> EXPLAIN (ANALYZE, BUFFERS) DELETE FROM orders WHERE order_id = 1;
> ROLLBACK;
> ```

### 2-2. 出力1行の構造

まずフォーマットを覚えます。

```text
ノード名  (cost=起動コスト..総コスト rows=推定行数 width=推定行幅)
          (actual time=起動時間..総時間 rows=実際の行数 loops=ループ回数)
  付加情報（Index Cond, Filter, Sort Key など）
  Buffers: shared hit=N read=N
```

上のカッコが**プランナの予測**、下のカッコが**実際の結果**です。この2つを見比べるのが読み方の基本になります。

---

## 3. 実行計画の読みどころ

### 3-1. 読む順番：上下ではなく「字下げの深さ」

実行計画はツリー構造です。

**基本ルール：字下げが深いノードほど先に実行される。** 子ノードが行を作り、親ノードがそれを受け取ります。

**同じ深さに兄弟ノードが並ぶときは、上が「外側」・下が「内側」です。** ただし**どちらが先に動くかは結合方式で変わります**。

*   `Nested Loop`：上（外側）が1行返すたびに下（内側）を実行する → **上が先**
*   `Hash Join`：下（内側）を全件読んでハッシュテーブルを作り終えてから、上（外側）を流し込む → **下が先**

```text
Hash Join                            ← ③ 最後に突き合わせ
  ->  Seq Scan on orders             ← ② ①の完成後に流し込む（外側）
  ->  Hash                           ← ① まずここを作り切る（内側）
        ->  Seq Scan on customers
```

そのため「下から上へ読む」という説明は、枝分かれのない単純な計画でしか成り立ちません。**上下ではなく「字下げの深さ」で捉えてください。**

### 3-2. ノードの種類

| 分類     | ノード                                                    | 内容                                                        |
| :----- | :----------------------------------------------------- | :-------------------------------------------------------- |
| スキャン   | **Seq Scan**                                           | 全件走査                                                      |
|        | **Index Scan**                                         | インデックスから住所を得て本体を狙い撃ち                                      |
|        | **Index Only Scan**                                    | 本体を見ずインデックス内で完結                                           |
|        | **Bitmap Index Scan**<br>＋ Bitmap Heap Scan            | 住所を地図にしてページ順にまとめ読み                                        |
|        | **Index Scan Backward**                                | インデックスを逆順に辿る（`ORDER BY ... DESC`）                         |
| 結合     | **Nested Loop**                                        | 外側1行ごとに内側をスキャン                                            |
|        | **Hash Join**                                          | ハッシュテーブルを作って突き合わせ                                         |
|        | **Merge Join**                                         | 双方をソートして突き合わせ                                             |
| ソート・集約 | **Sort**                                               | 並べ替え。`ORDER BY` だけでなく `GROUP BY` や `Merge Join` の前処理でも現れる |
|        | **Aggregate** / **HashAggregate** / **GroupAggregate** | 集計（`COUNT` `SUM` `GROUP BY`）                              |
|        | **Limit**                                              | 必要な行数が揃った時点で打ち切る                                          |
| その他    | Hash / BitmapOr / BitmapAnd / Materialize              | 上位ノードの下請けとして現れる                                           |

**`Sort` は「消せるかもしれないノード」です。** インデックスは既にソート済みなので、うまく使えば `Sort` そのものが不要になります（→ §5 のコラム「これは効きます」）。また大量データのソートはメモリを食い、溢れるとディスクを使うため、`Sort` を見つけたら一度立ち止まる価値があります（→ 17-3）。

#### 結合方式（Join Methods）の使い分け

3方式の違いは、**「総当たり（外側の行数 × 内側の行数）をどう避けるか」**の違いです。ここでは選ばれる条件だけ押さえてください。

| 方式              | 総当たりの回避方法      | 選ばれる条件                       | 非等価結合 |
| :-------------- | :------------- | :--------------------------- | :---: |
| **Nested Loop** | 内側をインデックスで引く   | 外側が少数 ＋ **内側の結合キーにindexがある** |   ⭕   |
| **Hash Join**   | 内側をハッシュテーブルにする | **大量データ同士**。indexは不要         |   ✕   |
| **Merge Join**  | 双方をソートして並走する   | 両側が**すでにソート済み**（index順に読める）  |   ⭕   |

**押さえるべきは「Hash が速い」ではありません。** 3方式に絶対的な優劣はなく、**条件が揃っているかどうか**で決まります。

とくに `Nested Loop` は、**内側の結合キーにインデックスがあるかどうか**で性質が正反対になります。

*   **インデックスがある** → 外側1行につき内側を数ページ引くだけ。**外側が少数なら最速**
*   **インデックスがない** → 外側1行ごとに内側を全件走査することになる。**「外側の行数 × 内側のページ数」**で、行数が増えるほど手が付けられなくなる

大量データ同士の結合で `Hash Join` が選ばれるのは、**ハッシュ化が魔法だから**ではなく、**インデックスが使えない状況で `Nested Loop` を選ぶと総当たりになってしまうから**です。「安いものを選んだ」というより「**高くつく選択肢を避けた**」と捉えるほうが実態に近いです。

> ⚠️ **どれが選ばれるかは、データ量・インデックスの有無・`work_mem` の設定で簡単に入れ替わります。** 「この結合はいつも Hash」と覚えるのではなく、**その場で `EXPLAIN` して確かめてください。**

> 📖 ハッシュテーブルの作り方、`Buckets` の意味、なぜ Hash Join が等価結合専用なのか、`Buffers` が両テーブルのページ数の「和」になる理由 ── アルゴリズムの中身は [[00_SQL研修/01_講義資料/17_indexと実行計画/17-4_補足_結合アルゴリズム|17-4_補足_結合アルゴリズム]] にまとめています。

### 3-3. 予測を読む（cost / rows / width）

#### cost（コスト）

`cost=0.00..15.25` の形式です。

*   左（0.00）：**最初の1行**を返すまでのコスト
*   右（15.25）：**全行**を返すまでの推定合計コスト
*   単位は「**シーケンシャルに**ページ1枚を読む時間」を1とした相対値です（`seq_page_cost = 1.0`）。秒でもミリ秒でもありません。

**cost は「比べるための指標」です。** 絶対値そのものに意味はありません。**同じクエリに対する別の計画と比べる**ために使います。プランナも、複数の計画のこの数字を比べて一番小さいものを選んでいるだけです。

内訳を決めているパラメータのうち、押さえておきたいのは2つです。

| パラメータ | 既定値 | 意味 |
| :--- | :--- | :--- |
| `seq_page_cost` | 1.0 | ページを**順番に**1枚読む |
| `random_page_cost` | **4.0** | ページを**ランダムに**1枚読む |

⚠️ `random_page_cost = 4.0` は「ランダムアクセスは順次読みの4倍高い」という**HDD時代の想定値**です。**SSD環境では 1.1〜2 に下げるのが定石**で、この1つの設定でインデックスを使うかどうかの判断が変わります（→ ケース5）。
#### rows（推定行数）
プランナが統計情報から予測した出力行数です。

#### width（行の幅）
1行あたりの推定バイト数です。`SELECT *` だと全列の合計になります。

### 3-4. 実測を読む（actual time / rows / loops）

`ANALYZE` を付けたときだけ出ます。

#### actual time
`actual time=0.016..10.227` の形式で、左が最初の1行まで、右がそのノードの処理完了までの**実測ミリ秒**です。子ノードの時間を含んだ累積値です。

#### rows
そのノードが**実際に出力した**行数です。**読み方の要点は、3-3 の推定 `rows` と見比べること。** 10倍以上ズレていたら統計情報を疑います（`ANALYZE テーブル名;`）。

#### loops（繰り返し回数）
そのノードが**呼び出された回数**です。Nested Loop の内側などで2以上になります。

⚠️ **`loops` が2以上のとき、`actual time` と `rows` は「1ループあたりの平均値」です。** 総量は自分で掛け算する必要があります。

実際に `loops` が出る計画を見てみます。

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT c.name, o.order_date
FROM customers c JOIN orders o ON c.customer_id = o.customer_id
WHERE c.city IN ('City_1','City_2','City_3','City_4','City_5');
```

```text
Nested Loop  (actual time=0.070..43.174 rows=10000 loops=1)
  Buffers: shared hit=13005 read=214
  ->  Bitmap Heap Scan on customers c  (actual time=0.064..0.438 rows=1000 loops=1)
        Buffers: shared hit=219
  ->  Index Scan using idx_orders_customer_id on orders o
            (actual time=0.037..0.042 rows=10 loops=1000)      ← ★1,000回呼ばれている
        Index Cond: (customer_id = c.customer_id)
        Buffers: shared hit=12786 read=214
Execution Time: 43.436 ms
```

内側の `Index Scan` は「0.042ms で 10行」と表示されていますが、これは**1回あたりの平均**です。

```text
総処理時間 = 0.042ms × 1,000ループ = 約42ms
総出力行数 = 10行   × 1,000ループ = 10,000行   ← 親の rows=10000 と一致
```

全体が43.4msなので、**そのうち約42ms（約97%）が内側のループ**でした。表示上は「0.042ms」という小さな数字なのに、実際は処理時間のほぼ全部を占めています。

`Buffers` も同じです。内側だけで13,000ページ。外側の `Bitmap Heap Scan` は219ページしか読んでいません。**「どこが重いのか」は `loops` の掛け算をしないと見えません。**

ループが多いノードは**表示上のコストが小さく見える**ため、ここを見落とすと真のボトルネックを取り逃します。**`loops` が大きい行を見つけたら、必ず掛け算してください。**

### 3-5. Buffers（I/Oの実態）

`Buffers: shared hit=1235 read=2` の形式です。

*   **`shared hit`**: 共有バッファ（メモリ）で見つかったページ数 → 速い
*   **`shared read`**: ディスクから読んだページ数 → 遅い
*   **`temp read` / `written`**: `work_mem` に収まらず一時ファイルを使った → **危険信号**（→ 17-3）

**`hit + read` の合計＝そのノードが読んだページ数**です。これを減らすことがチューニングのゴールです。

> 📌 **この研修環境では、`shared read` はほぼ0になります。**
> 理由は単純で、**データが共有バッファに全部収まってしまう**からです。`shared_buffers` が 128MB あるのに対し、`customers`（9.9MB）＋ `orders`（57MB）＝ 約67MB。一度読めば、以降ずっとメモリ上に居続けます。
> ```sql
> SHOW shared_buffers;   -- 128MB
> ```
> つまり**この資料の実測値がほぼ `shared hit` なのは正常**で、皆さんの手元でも同じ結果になります。**ここでは「`hit + read` の合計＝読んだページ数」の大小で比較してください。**
> 本番環境のように**テーブルがメモリに収まらない規模**になると、この数字がそのまま `shared read`（実際のディスク読み）に変わり、実行時間に直結します。
### 3-6. `Index Cond` と `Filter` の違い（重要）

同じ「絞り込み」でも、実行計画では2箇所に分かれて表示されます。**ここの区別が読めると、インデックスの改善点が一発で分かります。**

*   **`Index Cond`** … **インデックスで絞り込めた**条件。速い。
*   **`Filter`** … インデックスでは絞れず、**行を取ってきた後にCPUで捨てた**条件。無駄。

**【手を動かす】実際に比べる**

`orders(customer_id)` と `orders(order_date)` を**別々の単一列インデックス**として持っている状態で、両方を条件にします。

```sql
EXPLAIN SELECT * FROM orders WHERE customer_id = 1234 AND order_date >= '2025-01-01';
```

```text
Bitmap Heap Scan on orders  (cost=4.50..43.54 rows=5 width=25)
  Filter: (order_date >= '2025-01-01 00:00:00'::timestamp without time zone)   ← ⚠️ 後で捨てている
  ->  Bitmap Index Scan on idx_orders_customer_id  (cost=0.00..4.50 rows=10 width=0)
        Index Cond: (customer_id = 1234)                                        ← これだけで絞った
```

`customer_id` で10件に絞ってから、`order_date` は**取ってきた後にフィルタ**しています。では複合インデックスにすると──

```sql
CREATE INDEX idx_orders_cust_date ON orders (customer_id, order_date);
```

```text
Bitmap Heap Scan on orders  (cost=4.48..24.16 rows=5 width=25)
  ->  Bitmap Index Scan on idx_orders_cust_date  (cost=0.00..4.47 rows=5 width=0)
        Index Cond: ((customer_id = 1234) AND (order_date >= '2025-01-01'::timestamp))
```

**`Filter` が消えて、両方の条件が `Index Cond` に昇格しました。** コストも 43.54 → 24.16 に下がっています。

> **チューニングの定石**：`Filter` に出ている列を複合インデックスに足して `Index Cond` へ昇格させる。特に `Rows Removed by Filter` が大きい場合に効きます。

---

## 4. ケーススタディ（すべて実測）

### ケース1：インデックスがない → 張る

10万件の顧客から、特定の都市の顧客（200件）を検索します。

```sql
EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM customers WHERE city = 'City_123';
```

**❌ インデックスなし**

```text
Seq Scan on customers  (cost=0.00..2485.00 rows=200 width=69)
                       (actual time=0.016..10.227 rows=200 loops=1)
  Filter: (city = 'City_123'::text)
  Rows Removed by Filter: 99800
  Buffers: shared hit=1235
Execution Time: 10.247 ms
```

*   `Buffers: shared hit=1235` は `customers` の**総ページ数と完全一致**。全ページ読んだ証拠です。
*   `Rows Removed by Filter: 99800` — 200件のために99,800件を読んで捨てています。

**⭕ インデックスを張った後**

```sql
CREATE INDEX idx_customers_city ON customers (city);
ANALYZE customers;
```

```text
Bitmap Heap Scan on customers  (cost=5.83..533.52 rows=199 width=69)
                               (actual time=0.061..0.182 rows=200 loops=1)
  Recheck Cond: (city = 'City_123'::text)
  Heap Blocks: exact=186
  Buffers: shared hit=200 read=2
  ->  Bitmap Index Scan on idx_customers_city  (actual time=0.042..0.042 rows=200 loops=1)
        Index Cond: (city = 'City_123'::text)
Execution Time: 0.196 ms
```

**1,235ページ → 202ページ。10.2ms → 0.20ms（約52倍）。**

`Index Scan` ではなく `Bitmap Heap Scan` が選ばれたのは、200件が別々のページに散っているからです（17-1 §6-4 で `ctid` を数えて確認した通り）。

### ケース2：【罠】外部キーにインデックスがない

`orders.customer_id` には外部キー制約が付いています。しかし──

> **PostgreSQLは、主キーとUNIQUE制約にはインデックスを自動作成しますが、外部キーには作りません。**

実務で最も多いインデックス貼り忘れのパターンです。

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT c.name, o.order_date
FROM customers c JOIN orders o ON c.customer_id = o.customer_id
WHERE c.customer_id = 1234;
```

**❌ インデックスなし**

```text
Nested Loop  (actual time=0.654..8.222 rows=10 loops=1)
  Buffers: shared hit=7258
  ->  Index Scan using customers_pkey on customers c  (actual time=0.007..0.012 rows=1 loops=1)
        Index Cond: (customer_id = 1234)
        Buffers: shared hit=3
  ->  Seq Scan on orders o  (actual time=0.063..48.469 rows=10 loops=1)
        Filter: (customer_id = 1234)
        Rows Removed by Filter: 199990          ← ⚠️ ここがボトルネック
        Buffers: shared hit=7255
Execution Time: 48.526 ms
```

*   `customers` 側は**3ページ**で1行に絞れているのに、結合相手の `orders` で**全7,255ページ**を読んでいます。全体の99.96%が無駄読みです。
*   **読み方のコツ：「字下げが深い方（内側）が `Seq Scan` になっていないか」。** これが最も効く着眼点です。

**⭕ `orders(customer_id)` にインデックスを張った後**

```text
Nested Loop  (actual time=0.392..0.414 rows=10 loops=1)
  Buffers: shared hit=13 read=3
  ->  Index Scan using customers_pkey on customers c  (rows=1 loops=1)
        Buffers: shared hit=3
  ->  Bitmap Heap Scan on orders o  (actual time=0.149..0.161 rows=10 loops=1)
        Recheck Cond: (customer_id = 1234)
        Heap Blocks: exact=10
        Buffers: shared hit=10 read=3
Execution Time: 0.196 ms
```

**7,258ページ → 16ページ。48.5ms → 0.20ms（約247倍）。** `Rows Removed by Filter` は消えました。

### ケース3：【最強】テーブル本体を見ない

```sql
CREATE INDEX idx_orders_cust_cover ON orders (customer_id) INCLUDE (order_date);
```

```sql
VACUUM ANALYZE orders;   -- ★重要：Visibility Map を更新する。この行だけを単独で実行すること
```

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT customer_id, order_date FROM orders WHERE customer_id = 500;
```

> ⚠️ **`VACUUM` は他のSQLとまとめて実行できません。** 複数のSQLを一度に流すとPostgreSQLが1つのトランザクションとして扱うため、`ERROR: VACUUM cannot run inside a transaction block` になります。**その行だけを選択して実行**してください。

```text
Index Only Scan using idx_orders_cust_cover on orders  (cost=0.42..4.60 rows=10 width=12)
                                                      (actual time=0.094..0.095 rows=10 loops=1)
  Index Cond: (customer_id = 500)
  Heap Fetches: 0
  Buffers: shared hit=1 read=3
Execution Time: 0.127 ms
```

**わずか4ページ**、そして `Heap Fetches: 0`。テーブル本体を一度も読んでいません。

⚠️ **`VACUUM` を打たないとVisibility Mapが更新されず、`Heap Fetches` が増えて効果が消えます**（→ 17-1 §6-5）。

**`SELECT *` に変えると Index Only ではなくなります。**

```text
Bitmap Heap Scan on orders  (actual time=0.014..0.025 rows=10 loops=1)
  Recheck Cond: (customer_id = 500)
  Heap Blocks: exact=10
  Buffers: shared hit=13
```

インデックスに入っていない `order_id` と `status` を取るために、結局ヒープを読んでいます（4ページ → 13ページ）。**「SELECTする列を絞る」ことがそのままI/O削減になる**という分かりやすい例です。

### ケース4：【重要】同じインデックスでも、値によって計画が変わる

`orders.status` にインデックスを張ります。分布は Completed 90% / Shipped 8% / Pending 2% です。

```sql
CREATE INDEX idx_orders_status ON orders (status);
ANALYZE orders;
```

**`status = 'Pending'`（2% = 2万件）**

```text
Index Scan using idx_orders_status on orders  (actual time=0.595..2.905 rows=20000 loops=1)
  Index Cond: ((status)::text = 'Pending'::text)
  Buffers: shared hit=128 read=20
Execution Time: 3.501 ms
```

**`status = 'Completed'`（90% = 90万件）**

```text
Seq Scan on orders  (actual time=0.008..115.875 rows=900000 loops=1)
  Filter: ((status)::text = 'Completed'::text)
  Buffers: shared hit=7255
Execution Time: 144.680 ms
```

**同じテーブル、同じインデックス、同じSQLの形。違うのは検索する値だけ。** それでも一方は148ページ、もう一方は7,255ページ（全件走査）になりました。

プランナは統計情報から「この値は何件くらいヒットするか」を知っています。90%もヒットするなら、インデックス経由で飛び回るより全部読んだ方が安いと**正しく**判断しています。

> **ここから学ぶこと**：「インデックスが使われない」と相談を受けたとき、**まず何の値で検索しているかを聞く**。値によって計画が変わるのは正常な動作です。

### ケース5：【上級】プランナは間違えることもある

最後に「コストが低い＝速い」が崩れる実例です。`city` 5件分（顧客1,000人）を結合します。

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT c.name, o.order_date
FROM customers c JOIN orders o ON c.customer_id = o.customer_id
WHERE c.city IN ('City_1','City_2','City_3','City_4','City_5');
```

**プランナが選んだ計画（`random_page_cost = 4`：既定値）**

```text
Hash Join  (cost=1282.80..21162.91 rows=9960 width=22)
           (actual time=21.038..312.043 rows=10000 loops=1)
  Hash Cond: (o.customer_id = c.customer_id)
  Buffers: shared hit=7468 read=6
  ->  Seq Scan on orders o  (actual time=0.029..190.397 rows=1000000 loops=1)  ← indexがあるのに使わない
        Buffers: shared hit=7255
  ->  Hash  (actual time=20.981..20.983 rows=1000 loops=1)
        Buckets: 1024  Batches: 1  Memory Usage: 59kB
        ->  Bitmap Heap Scan on customers c  (actual time=2.229..20.702 rows=1000 loops=1)
Execution Time: 312.427 ms
```

**`random_page_cost` をSSD相当に下げると**

```sql
SET random_page_cost = 1.1;
```

```text
Nested Loop  (cost=15.10..8662.60 rows=9960 width=22)
             (actual time=0.070..43.174 rows=10000 loops=1)
  Buffers: shared hit=13005 read=214
  ->  Bitmap Heap Scan on customers c  (actual time=0.064..0.438 rows=1000 loops=1)
        Buffers: shared hit=219
  ->  Index Scan using idx_orders_customer_id on orders o
            (actual time=0.037..0.042 rows=10 loops=1000)
        Index Cond: (customer_id = c.customer_id)
        Buffers: shared hit=12786 read=214
Execution Time: 43.436 ms
```

*   **312.4ms → 43.4ms。約7倍速い計画が、既定設定では選ばれませんでした。**
*   **なぜ**：既定の `random_page_cost = 4.0` は「ランダムアクセスは4倍高い」というHDD時代の想定です。実際にはデータが共有バッファに乗っており、罰則はほぼゼロでした。プランナのコストモデルが現実とズレていたのです。

> **ここで1つ、面白い観察があります。** 速い方（Nested Loop）の方が、読んだページ数は**多い**のです（13,219ページ 対 7,474ページ）。
> なぜ速いのか。Hash Join は `orders` の**100万行すべてを執行器に通して**ハッシュ照合しています（`Seq Scan` だけで190ms）。一方 Nested Loop が触った13,000ページはほぼ全て `shared hit`、つまり**メモリ上の同じインデックス領域を繰り返し叩いているだけ**なので、1回あたりが極めて安いのです。
> **「読んだページ数」は本番でディスクを読むときの目安**であって、全部キャッシュに乗っている環境では「何行処理したか」が効いてきます。目安は目安として、最後は実測で判断してください。
*   **実務での意味**：「インデックスがあるのに Seq Scan が選ばれる」と悩んだとき、SQLやインデックスではなく**この設定が原因**であることは珍しくありません。

**【手を動かす】プランナに別の計画を作らせて比較する**

「なぜこっちを選んだの？」を調べる道具があります。特定の方式を一時的に禁止して、もう一方のコストを見るのです。

```sql
SET enable_seqscan = off;    -- Seq Scan を禁止
EXPLAIN (ANALYZE, BUFFERS) SELECT ... ;
RESET enable_seqscan;        -- 必ず戻す

-- 他にもこんなスイッチがあります
-- enable_indexscan / enable_bitmapscan / enable_hashjoin / enable_nestloop
```

セッション限定なので安全です。**両方のコストと実測を並べて見る**と、プランナの判断根拠が見えてきます。

> **参考：cost はどう作られているのか**（読み物。講義では扱いません）
> `cost` は魔法の数字ではなく、**ページ数と行数からの単純な足し算**です。`customers` の全件走査 `cost=0.00..2485.00` は次のように分解できます。
> ```text
> seq_page_cost      1.0    × 1,235ページ  = 1,235
> cpu_tuple_cost     0.01   × 100,000行    = 1,000
> cpu_operator_cost  0.0025 × 100,000行    =   250
>                                    合計  = 2,485   ← 表示と一致
> ```
> 「機械的に計算しているだけ」と分かれば、2つの計画のコストを見比べる作業に納得がいくはずです。各パラメータは `SHOW cpu_tuple_cost;` などで確認できます。
### まとめ：チューニングの優先順位

1.  **読んでいるページ数（`Buffers` の hit + read）が多いノードを探す**
    *   `pg_class.relpages` と一致していたら全件走査です。
2.  **`Rows Removed by Filter` を見る**
    *   大量に捨てている場所は、インデックスで絞り込めていない証拠です。
3.  **`Filter` に出ている条件を `Index Cond` に昇格できないか考える**（→ 3-6）
4.  **結合の「内側（字下げが深い方）」が `Seq Scan` になっていないか見る**
    *   `loops` が多いとき、ここでのSeq Scanは致命的です。
5.  **推定 `rows` と実測 `rows` が大きくズレていないか見る**
    *   ズレていれば `ANALYZE`。それでも直らなければケース5のようにコスト設定を疑います。

---

## 5. インデックスが効かない落とし穴

インデックスがあっても、**SQLの書き方**で使えなくなることがあります。よく引っかかるものだけを挙げます。

### ① 列を加工している（最頻出）

インデックスは「**加工前の生の値**」で並んでいます。列を関数や計算式に入れると、並び順が使えなくなります。

*   ❌ **効かない**
    ```sql
    SELECT * FROM orders WHERE DATE(order_date) = '2024-06-01';
    ```
    ```text
    Seq Scan on orders  (actual time=16.600..78.893 rows=1440 loops=1)
      Buffers: shared hit=7255                      ← 全ページ読んでいる
    Execution Time: 78.937 ms
    ```
*   ⭕ **改善：列はそのままにして、条件側を範囲に書き換える**
    ```sql
    SELECT * FROM orders
    WHERE order_date >= '2024-06-01' AND order_date < '2024-06-02';
    ```
    ```text
    Index Scan using idx_orders_order_date on orders  (actual time=0.197..0.373 rows=1440 loops=1)
      Buffers: shared hit=11 read=6                  ← 17ページ
    Execution Time: 0.413 ms
    ```

**7,255ページ → 17ページ。78.9ms → 0.41ms（約191倍）。同じ結果を返すSQLです。**

同じ理由で、次もすべて効きません。**「列を触ったら負け」**と覚えてください。

| ❌ 効かない書き方 | ⭕ 直し方 |
| :--- | :--- |
| `WHERE order_date + interval '30 days' < now()` | `WHERE order_date < now() - interval '30 days'` |
| `WHERE UPPER(name) = 'ALICE'` | 関数インデックス、または `citext` 型 |
| `WHERE customer_id::text = '1234'` | `WHERE customer_id = 1234` |

**キャストについて補足。** `customer_id::text = '1234'` は列側にキャストが付くので効きません。

```text
Seq Scan on customers   Filter: ((customer_id)::text = '1234'::text)
```

一方 `WHERE customer_id = '1234'` は**問題なく効きます**。リテラル側が数値として解決されるためです。

```text
Index Scan using customers_pkey on customers   Index Cond: (customer_id = 1234)
```

> 「型が違うとインデックスが効かない」というのは**他のDB（MySQLなど）の話**です。PostgreSQLでは、文字列型の列に数値を渡すとそもそもエラーになります（`operator does not exist: character varying = integer`）。**気にすべきは「列側にキャストが付いていないか」だけ**です。

**どうしても加工した形で検索したい場合は、関数インデックスを作ります。**

```sql
-- ❌ Seq Scan（1,235ページ / 16.5ms）
SELECT * FROM customers WHERE LOWER(email) = 'user5555@example.com';

CREATE INDEX idx_customers_email_lower ON customers (LOWER(email));

-- ⭕ Index Scan（4ページ / 0.12ms）── 約137倍
```

### ② 否定形で書いている

「〜ではない」は、インデックスでは絞り込めません。索引を引いて「該当しないもの全部」を集めるのは、全部見るのと同じことだからです。

*   ❌ **効かない**
    ```sql
    SELECT * FROM orders WHERE status <> 'Completed';
    -- SELECT * FROM orders WHERE status NOT IN ('Completed');  -- これも同じ
    ```
    ```text
    Seq Scan on orders  (cost=0.00..19755.00 rows=102033 width=25)
      Filter: ((status)::text <> 'Completed'::text)
    ```
*   ⭕ **改善：欲しい値を列挙する（肯定形にする）**
    ```sql
    SELECT * FROM orders WHERE status IN ('Shipped', 'Pending');
    ```
    ```text
    Index Scan using idx_orders_status on orders  (cost=0.42..2921.91 rows=102033 width=25)
      Index Cond: ((status)::text = ANY ('{Shipped,Pending}'::text[]))
    ```

**推定行数はどちらも 102,033 行、つまり結果は同じです。** 書き方を変えただけで `Seq Scan` が `Index Scan` になり、コストが 19,755 → 2,921 に下がりました。

`status` のように取りうる値が少ない列では、この書き換えが有効です。

### ③ LIKE の前方一致になっていない

*   ⭕ **効く可能性がある**：`LIKE 'ABC%'`（前方一致）
*   ❌ **効かない**：`LIKE '%ABC'` `LIKE '%ABC%'`（後方・部分一致）
*   **理由**：辞書で「あ」から始まる単語はすぐ引けますが、「あ」で終わる単語は最初から最後まで読むしかありません。

⚠️ **`_` もワイルドカード（任意の1文字）です。** そのため絞り込みに使える前置きは `_` の手前までになります。

```sql
CREATE INDEX idx_customers_name ON customers (name);
ANALYZE customers;

-- customers.name は 'Customer_1' 'Customer_2' ... という値
EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM customers WHERE name LIKE 'Customer_5555%';
EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM customers WHERE name LIKE 'Customer\_5555%';
```

```text
-- ❌ エスケープなし：前置きが 'Customer' までしか取れず、全件が候補になる
Seq Scan on customers  (cost=0.00..2485.00 rows=10 width=69)
                       (actual time=0.882..11.440 rows=11 loops=1)
  Filter: ((name)::text ~~ 'Customer_5555%'::text)
  Rows Removed by Filter: 99989
  Buffers: shared hit=1235
Execution Time: 11.465 ms

-- ⭕ _ をエスケープ：本来の前方一致として扱われる
Index Scan using idx_customers_name on customers  (cost=0.42..8.44 rows=10 width=69)
                                                  (actual time=0.070..0.074 rows=11 loops=1)
  Index Cond: (((name)::text >= 'Customer_5555'::text) AND ((name)::text < 'Customer_5556'::text))
  Filter: ((name)::text ~~ 'Customer\_5555%'::text)
  Buffers: shared hit=4 read=1
Execution Time: 0.085 ms
```

**1,235ページ → 5ページ。11.5ms → 0.085ms（約134倍）。** バックスラッシュ1文字の差です。

`Index Cond` に注目してください。**PostgreSQLが前方一致を範囲検索（`>= 'Customer_5555' AND < 'Customer_5556'`）に読み替えている**のが見えます。これが「前方一致ならインデックスが使える」の中身です。

> **実務での注意**：前方一致でインデックスが効くかは、DBの照合順序に依存します。`ja_JP.UTF-8` などの環境では `text_pattern_ops` を指定して作る必要があります。自分の環境は `SELECT datcollate FROM pg_database WHERE datname = current_database();` で確認してください（研修環境は `C` なので、そのままでも効きます）。
> 部分一致 `'%ABC%'` を高速化したい場合は `pg_trgm` 拡張 + GINインデックスの領域です。

### ④ 複合インデックスの順番を無視している

`(A, B)` の順で作った場合、**A を指定せず B だけで検索しても効きません**（1-2 の構造図の通り）。

*   ⭕ **効く**：`WHERE A = 10` / `WHERE A = 10 AND B = 20`
*   ❌ **効かない**：`WHERE B = 20`

`orders (customer_id, order_date)` の複合インデックスだけがある状態で `order_date` 単独で検索すると、実際に全件走査になります。

```text
Seq Scan on orders  (cost=0.00..19755.00 rows=100 width=25)
```

**設計時のコツ**：`WHERE` で**必ず指定される列を左に**置きます。

### ⑤ OR の片方にインデックスがない

`OR` は、**片方でもインデックスが使えないと全体が全件走査になります。**

*   ⭕ **両方にインデックスあり** → `BitmapOr` で両方のインデックスを使う
    ```text
    Bitmap Heap Scan on customers
      ->  BitmapOr
            ->  Bitmap Index Scan on idx_customers_city      Index Cond: (city = 'City_5')
            ->  Bitmap Index Scan on customers_pkey          Index Cond: (customer_id = 1234)
    ```
*   ❌ **片方にインデックスなし**（`phone` にインデックスがない）
    ```sql
    SELECT * FROM customers WHERE city = 'City_5' OR phone = '090-0001-0000';
    ```
    ```text
    Seq Scan on customers  (cost=0.00..2735.00 rows=209 width=69)
      Filter: (((city)::text = 'City_5'::text) OR ((phone)::text = '090-0001-0000'::text))
    ```

`city` 側のインデックスも**使われなくなっている**点に注目してください。`OR` で並べる列は、**全部にインデックスが必要**です。

### ⑥ 選択性が低い（絞り込めない列）

`membership_id`（4種類・各25%）にインデックスを張って、実際に測ってみます。

```sql
CREATE INDEX idx_customers_membership ON customers (membership_id);
ANALYZE customers;
EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM customers WHERE membership_id = 2;
```

```text
-- インデックスあり
Bitmap Heap Scan on customers  (cost=279.80..1823.67 rows=24710 width=69)
                               (actual time=2.622..5.367 rows=25000 loops=1)
  Buffers: shared hit=1235 read=23        ← 合計1,258ページ
Execution Time: 5.988 ms

-- Seq Scan を強制して比較
Seq Scan on customers  (cost=0.00..2485.00 rows=24710 width=69)
                       (actual time=0.022..23.130 rows=25000 loops=1)
  Filter: (membership_id = 2)
  Rows Removed by Filter: 75000
  Buffers: shared hit=1235                ← 1,235ページ
Execution Time: 23.820 ms
```

ここで**重要な観察**です。

*   時間は 23.8ms → 5.99ms で**4倍速くなっています**。
*   しかし**読んだページ数は 1,235 → 1,258 で、むしろ増えています。**

つまり **I/Oは1ページも減っていません。** 25%も該当すれば、どのページにも該当行があるので、結局全ページ読むことになります。速くなったのは「75,000行を捨てる処理が消えた」という**CPUの節約分だけ**です。

*   さらに選択率が上がると、プランナはインデックスを使うことすらやめます（ケース4の `status = 'Completed'` 90% → `Seq Scan`）。
*   **結論**：選択性が低い列へのインデックスは、**I/O削減にはならない**。得られるのはCPU分の改善だけなので、**書き込み負荷（17-1 §8 で見た7.6倍）と容量の代償に見合うか**を天秤にかけてください。

「とりあえずインデックスを張る」が最も無意味になる典型です。**その列で本当に絞り込めるのか**を先に確認しましょう。

```sql
-- 列ごとの異なる値の数を見る（n_distinct が小さいほど絞り込めない）
SELECT attname, n_distinct FROM pg_stats WHERE tablename = 'customers';
```

### ⑦ 統計情報が古い

大量に `INSERT` / `UPDATE` した直後は、プランナが「まだデータは少ない」と勘違いしていることがあります。

*   ❌ **症状**：`EXPLAIN` の推定 `rows` と実測 `rows` が大きく乖離している。
*   ⭕ **対処**：`ANALYZE テーブル名;` を実行する。

**実際に計画が変わることを確かめます。** 20万行を入れて、`ANALYZE` する前と後で比べます。

```sql
CREATE TABLE stale2 (id INT, v TEXT);
INSERT INTO stale2 SELECT gs, md5(gs::text) FROM generate_series(1,200000) gs;
CREATE INDEX ON stale2 (id);

-- ANALYZE せずに実行計画を見る
EXPLAIN SELECT * FROM stale2 WHERE id < 150000;   -- 実際は 149,999行（75%）が該当

ANALYZE stale2;
EXPLAIN SELECT * FROM stale2 WHERE id < 150000;
```

```text
-- ANALYZE 前：統計情報がないので「33%くらいだろう」と推測している
Bitmap Heap Scan on stale2  (cost=1253.09..3753.43 rows=66667 width=36)
  ->  Bitmap Index Scan on stale2_id_idx  (cost=0.00..1236.42 rows=66667 width=0)
        Index Cond: (id < 150000)

-- ANALYZE 後：正しく75%と分かり、全件走査に切り替わった
Seq Scan on stale2  (cost=0.00..4167.00 rows=149585 width=37)
  Filter: (id < 150000)
```

**推定 66,667行 → 149,585行。実際は149,999行なので、`ANALYZE` 後はほぼ正確です。**

統計情報がないとき、PostgreSQLは「`<` なら3分の1くらい該当するだろう」という既定値で見積もります。実際は75%だったので、**インデックスを使う計画を誤って選んでいました。** `ANALYZE` した瞬間に判断が変わっています。

大量の `INSERT` / `UPDATE` の直後は、**autovacuum が追いつく前に `ANALYZE` を手で打つ**のが安全です。

```sql
-- 最後にいつANALYZEされたか確認
SELECT relname, last_analyze, last_autoanalyze
FROM pg_stat_user_tables WHERE relname IN ('customers','orders');
```

```sql
-- 後片付け
DROP TABLE stale2;
```

### 【誤解しやすい】これは「効きます」

逆に、効かないと思われがちだが**効く**ものも押さえておきましょう。

**`IS NULL` は効きます。** PostgreSQLはNULLもインデックスに格納します（Oracle経験者が誤解しがちな点です）。

```text
Index Scan using idx_customers_city on customers
  Index Cond: (city IS NULL)
```

**`ORDER BY` もインデックスで代替できます。** インデックスはソート済みなので、そのまま読めばソート結果になります。

```sql
EXPLAIN SELECT * FROM orders ORDER BY order_date LIMIT 10;
```

```text
Limit
  ->  Index Scan using idx_orders_order_date on orders     ← Sortノードが無い！
```

降順にすると `Index Scan Backward`（インデックスを逆から辿る）になります。逆にインデックスのない列でソートすると、`Sort` ノードが現れます。

```text
Limit
  ->  Sort   Sort Key: phone                               ← ソート処理が発生
        ->  Seq Scan on customers
```

**`Sort` ノードが出ていたら、インデックスで消せないか**を考える癖をつけてください（§3-2 のノード一覧でも触れた通りです）。ソートは大量データでは `work_mem` を食い、溢れるとディスクを使います（→ 17-3）。

---

## 6. チューニングのチェックリスト

遅いクエリに出会ったら、上から順に確認します。

1.  **`EXPLAIN (ANALYZE, BUFFERS)` を取る。** 憶測で直さない。
2.  **`Buffers` が `pg_class.relpages` と一致していないか。** していたら全件走査。
3.  **インデックスの貼り忘れはないか。** 特に**外部キー**（自動では作られない）。
4.  **インデックスが効かない書き方をしていないか。**
    *   列を加工していないか（関数・計算・キャスト）
    *   否定形（`<>` `NOT IN`）になっていないか
    *   `OR` の全ての列にインデックスがあるか
5.  **`Filter` に出ている条件を `Index Cond` に昇格できないか。**（複合インデックス化）
6.  **複合インデックスの列順は、WHERE句の使い方と合っているか。**
7.  **`Sort` ノードをインデックスで消せないか。**
8.  **統計情報は最新か。** `ANALYZE テーブル名;`
9.  **それでもおかしいなら、コスト設定を疑う。** SSDなら `random_page_cost` を 1.1〜2 へ。
10. **そのインデックスは本当に必要か。** `pg_stat_user_indexes.idx_scan = 0` のものは、容量と書き込み負荷だけを食っています。

次の **17-3** では、`Buffers` の `temp` と `work_mem` に踏み込み、「メモリに収まらなかったとき何が起きるか」を見ます。
