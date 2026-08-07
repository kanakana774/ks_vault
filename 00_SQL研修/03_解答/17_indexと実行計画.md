# 17章 演習 解答：indexと実行計画

以下はすべて **PostgreSQL 17 での実測値**です。皆さんの環境でも、**スキャン方式とページ数は同じになるはず**です。

> **実行時間（`Execution Time`）だけは環境で変わります。** キャッシュの温まり具合やマシン性能に左右されるので、数値そのものではなく**倍率**を見てください。
> 逆に**ページ数（`Buffers`）は理屈で決まるので、ほぼ同じ数字が出ます。** 大きく違っていたら、初期状態に戻し忘れているか、`ANALYZE` を打ち忘れています。

---

## 問題1：基準値を確認する

```text
  relname  | ページ数 |  行数   | １ページあたり行数
-----------+----------+---------+--------------------
 customers |     1235 |  100000 |                 81
 orders    |     7255 | 1000000 |                138
```

| | ページ数 | 1ページあたり行数 |
| :--- | ---: | ---: |
| `customers` | **1,235** | **81** |
| `orders` | **7,255** | **138** |

`ctid` の切り替わり：

```text
  ctid  | customer_id
--------+-------------
 (0,81) |          81
 (0,82) |          82
 (1,1)  |          83      ← ここでページが変わる
 (1,2)  |          84
```

**答え：`customer_id = 83`。** 0番ページには82行入り、83行目から1番ページに移りました。8KBの箱が満杯になった瞬間です。

---

## 問題2：インデックスを張ると何が変わるか

### ① インデックスなし

```text
Seq Scan on customers  (cost=0.00..2485.00 rows=199 width=69)
                       (actual time=0.020..6.001 rows=200 loops=1)
  Filter: ((city)::text = 'City_123'::text)
  Rows Removed by Filter: 99800
  Buffers: shared hit=1235
Execution Time: 6.017 ms
```

| 項目 | 答え |
| :--- | :--- |
| スキャン方式 | **`Seq Scan`** |
| `Buffers` の合計 | **1,235** ページ |
| `Rows Removed by Filter` | **99,800** 件 |

**問題1の `customers` のページ数（1,235）と完全に一致しています。** これが「全ページ読んだ」＝全件走査の証拠です。200件を取るために99,800件を読んで捨てています。

### ② 散らばりを数える

```text
 該当行 | 使用ページ数
--------+--------------
    200 |          200      ← city 指定：1行につき1ページ
    200 |            3      ← 主キー範囲：3ページに収まる
```

**答え：200ページ 対 3ページ。** どちらも同じ200件なのに、**約67倍**の差があります。`city` は値が循環するように投入したので全ページに散り、`customer_id` は連番なので固まっています。

### ③ インデックスあり

**【予想】の答え：イ（`Bitmap Heap Scan`）**

```text
Bitmap Heap Scan on customers  (cost=5.83..533.52 rows=199 width=69)
                               (actual time=0.077..0.201 rows=200 loops=1)
  Recheck Cond: ((city)::text = 'City_123'::text)
  Heap Blocks: exact=200
  Buffers: shared hit=200 read=2
  ->  Bitmap Index Scan on idx_customers_city  (actual time=0.056..0.056 rows=200 loops=1)
        Index Cond: ((city)::text = 'City_123'::text)
Execution Time: 0.217 ms
```

| 項目 | 答え |
| :--- | :--- |
| スキャン方式 | **`Bitmap Heap Scan`** |
| `Heap Blocks: exact=` | **200** |
| `Buffers` の合計 | **202** ページ |
| `Execution Time` | 0.217 ms |

**1,235 → 202ページ。約6分の1です。** 実行時間は 6.0ms → 0.22ms（約27倍）。

**なぜ `Index Scan` ではないのか。** ②で数えた通り、200件が200ページに散っているからです。1件ずつ取りに行くと200回のランダムアクセスになり、しかも順序がバラバラです。`Bitmap` なら「必要なページの地図」を先に作り、**ページ番号順に1回ずつ**読めます。`Heap Blocks: exact=200` が「実際に読んだヒープページ数」です。

---

## 問題3：同じインデックスでも、値によって計画が変わる

**【予想】の答え：いいえ（変わります）**

### ① `Pending`（2%）

```text
Index Scan using idx_orders_status on orders  (cost=0.42..606.37 rows=19433 width=25)
                                              (actual time=0.214..4.336 rows=20000 loops=1)
  Index Cond: ((status)::text = 'Pending'::text)
  Buffers: shared hit=128 read=20
Execution Time: 5.156 ms
```

### ② `Completed`（90%）

```text
Seq Scan on orders  (cost=0.00..19755.00 rows=900300 width=25)
                    (actual time=0.009..71.967 rows=900000 loops=1)
  Filter: ((status)::text = 'Completed'::text)
  Rows Removed by Filter: 100000
  Buffers: shared hit=7255
Execution Time: 89.670 ms
```

| | スキャン方式 | `Buffers` 合計 | `Execution Time` |
| :--- | :--- | ---: | ---: |
| ① `Pending`（2%） | **`Index Scan`** | **148** ページ | 5.2 ms |
| ② `Completed`（90%） | **`Seq Scan`** | **7,255** ページ | 89.7 ms |

②は `orders` の総ページ数（7,255）と一致 ＝ 全件走査です。

**これはバグではなく、プランナの正しい判断です。** 90万件も該当するなら、インデックスで住所を調べてページを飛び回るより、最初から全部順番に読んだ方が安いからです。

> **実務での教訓**：「インデックスが使われない」と相談されたら、**まず何の値で検索しているかを聞いてください。** 値によって計画が変わるのは正常な動作です。

---

## 問題4：外部キーにインデックスがない

### ① インデックスなし

```text
Nested Loop  (cost=0.29..19763.41 rows=10 width=22) (actual time=1.015..39.913 rows=10 loops=1)
  Buffers: shared hit=7258
  ->  Index Scan using customers_pkey on customers c  (actual time=0.945..0.949 rows=1 loops=1)
        Index Cond: (customer_id = 1234)
        Buffers: shared hit=3
  ->  Seq Scan on orders o  (cost=0.00..19755.00 rows=10 width=12)
                            (actual time=0.068..38.955 rows=10 loops=1)
        Filter: (customer_id = 1234)
        Rows Removed by Filter: 999990          ← ここがボトルネック
        Buffers: shared hit=7255
Execution Time: 39.933 ms
```

| 項目 | 答え |
| :--- | :--- |
| `customers` 側 | `Index Scan`／**3** ページ |
| **`orders` 側** | **`Seq Scan`／7,255 ページ** |
| `Rows Removed by Filter` | **999,990** 件 |
| `Execution Time` | 39.9 ms |

**内側（`orders`）が圧倒的に重い**ことが分かります。全体7,258ページのうち **99.96%** が `orders` の無駄読みです。10件取るために99万9,990件を捨てています。

### ② インデックスあり

```text
Nested Loop  (cost=4.79..51.92 rows=10 width=22) (actual time=0.132..0.152 rows=10 loops=1)
  Buffers: shared hit=13 read=3
  ->  Index Scan using customers_pkey on customers c  (rows=1 loops=1)
        Buffers: shared hit=3
  ->  Bitmap Heap Scan on orders o  (actual time=0.117..0.136 rows=10 loops=1)
        Recheck Cond: (customer_id = 1234)
        Heap Blocks: exact=10
        Buffers: shared hit=10 read=3
Execution Time: 0.169 ms
```

| 項目 | 答え |
| :--- | :--- |
| `orders` 側のスキャン方式 | **`Bitmap Heap Scan`** |
| 全体の `Buffers` | **16** ページ |
| `Execution Time` | 0.169 ms |

**7,258 → 16ページ（約450分の1）。39.9ms → 0.17ms で約236倍。** `Rows Removed by Filter` は消えました。

> **ここが実務で最も多いインデックス貼り忘れのパターンです。**
> PostgreSQLは**主キーとUNIQUE制約にはインデックスを自動作成しますが、外部キーには作りません。** 外部キーを定義したら、インデックスは自分で張る必要があります。
>
> **読み方のコツ**：結合の実行計画では、**字下げが深い方（内側）が `Seq Scan` になっていないか**を最初に見てください。

---

## 問題5：複合インデックスの「左端規則」

**【予想】の答え：いいえ（使われません）**

### ① 両方を指定

```text
Bitmap Heap Scan on orders  (cost=4.48..24.16 rows=5 width=25) (actual time=0.036..0.040 rows=4 loops=1)
  Recheck Cond: ((customer_id = 1234) AND (order_date >= '2025-01-01'::timestamp))
  Heap Blocks: exact=4
  Buffers: shared hit=4 read=3
  ->  Bitmap Index Scan on idx_orders_cust_date
        Index Cond: ((customer_id = 1234) AND (order_date >= '2025-01-01'::timestamp))
```

### ② 第1キーのみ

```text
Bitmap Heap Scan on orders  (cost=4.50..43.51 rows=10 width=25) (actual time=0.014..0.022 rows=10 loops=1)
  Buffers: shared hit=13
  ->  Bitmap Index Scan on idx_orders_cust_date
        Index Cond: (customer_id = 1234)
```

### ③ 第2キーのみ

```text
Seq Scan on orders  (cost=0.00..19755.00 rows=35288 width=25)
                    (actual time=100.046..103.772 rows=35201 loops=1)
  Filter: (order_date >= '2025-11-01'::timestamp)
  Rows Removed by Filter: 964799
  Buffers: shared hit=7255
Execution Time: 104.509 ms
```

| | スキャン方式 | `Buffers` 合計 |
| :--- | :--- | ---: |
| ① 両方 | `Bitmap Heap Scan`（index使用） | **7** ページ |
| ② 第1キーのみ | `Bitmap Heap Scan`（index使用） | **13** ページ |
| ③ 第2キーのみ | **`Seq Scan`（index未使用）** | **7,255** ページ |

③は `orders` の総ページ数と一致＝全件走査です。**インデックスがあるのに、まったく使われていません。**

**理由**：複合インデックスは「まず `customer_id` で並べ、同じ `customer_id` の中で `order_date` で並べる」という構造です。**辞書で「2文字目だけ分かっていても引けない」**のと同じで、第1キーが分からないと第2キーの並びを利用できません。

**設計のコツ**：`WHERE` 句で**必ず指定される列を左に**置いてください。

また①では、`Index Cond` に**両方の条件**が入っています。②の `Index Cond` は `customer_id` だけです。**`Index Cond` に入っている条件だけがインデックスで絞れた条件**です（17-2 §3-6）。

---

## 問題6：テーブル本体を見ない

### ① カバリングインデックス

```text
Index Only Scan using idx_orders_cust_cover on orders  (cost=0.42..4.60 rows=10 width=12)
                                                       (actual time=0.543..0.545 rows=10 loops=1)
  Index Cond: (customer_id = 500)
  Heap Fetches: 0
  Buffers: shared hit=1 read=3
Execution Time: 0.560 ms
```

| 項目 | 答え |
| :--- | :--- |
| スキャン方式 | **`Index Only Scan`** |
| **`Heap Fetches`** | **0** |
| `Buffers` の合計 | **4** ページ |

**`Heap Fetches: 0` ＝ テーブル本体を一度も読んでいません。** 必要な `customer_id` と `order_date` が両方インデックスの中にあるためです。

> **`VACUUM ANALYZE` を打った理由がここにあります。** インデックスには「その行が今見えるか」という情報がありません。そこで **Visibility Map** で確認するのですが、これを更新するのが `VACUUM` です。打ち忘れると `Heap Fetches` が増えて、この効果が消えます。

### ② `SELECT *` にすると

```text
Bitmap Heap Scan on orders  (cost=4.50..43.51 rows=10 width=25) (actual time=0.013..0.022 rows=10 loops=1)
  Recheck Cond: (customer_id = 500)
  Heap Blocks: exact=10
  Buffers: shared hit=13
Execution Time: 0.032 ms
```

| 項目 | 答え |
| :--- | :--- |
| スキャン方式 | **`Bitmap Heap Scan`**（`Index Only` ではなくなった） |
| `Buffers` の合計 | **13** ページ |

**4 → 13ページ、9ページ増えました。**

インデックスに入っていない `order_id` と `status` を取るために、結局テーブル本体を読みに行っています。**「`SELECT *` をやめて必要な列だけにする」ことが、そのままI/O削減になる**という分かりやすい例です。

---

## 問題7：列を加工してはいけない

### ①② インデックスがない状態

| | スキャン方式 | `Buffers` | `Execution Time` |
| :--- | :--- | ---: | ---: |
| ① `DATE(order_date) = ...` | `Seq Scan` | 7,255 | 71.0 ms |
| ② 範囲に書き換え | `Seq Scan` | 7,255 | 54.7 ms |

**返る件数はどちらも 1,440 件**（同じ結果です）。

**スキャン方式は変わりませんでした。** ここが大事なポイントで、**「書き換えれば速くなる」わけではありません。** インデックスが無ければどちらも全件走査です。

### ③ `order_date` にインデックスを作ってから

```text
-- ① DATE() を使った方（インデックスがあっても使われない）
Seq Scan on orders  (cost=0.00..22255.00 rows=5000 width=25)
                    (actual time=19.947..76.007 rows=1440 loops=1)
  Filter: (date(order_date) = '2024-06-01'::date)
  Rows Removed by Filter: 998560
  Buffers: shared hit=7255
Execution Time: 76.059 ms

-- ② 範囲に書き換えた方
Index Scan using idx_orders_order_date on orders  (cost=0.42..59.30 rows=1444 width=25)
                                                  (actual time=0.087..0.249 rows=1440 loops=1)
  Index Cond: ((order_date >= '2024-06-01'::timestamp) AND (order_date < '2024-06-02'::timestamp))
  Buffers: shared hit=11 read=6
Execution Time: 0.289 ms
```

| | スキャン方式 | `Buffers` 合計 | `Execution Time` |
| :--- | :--- | ---: | ---: |
| ① `DATE()` を使った方 | **`Seq Scan`** | **7,255** ページ | 76.1 ms |
| ② 範囲に書き換えた方 | **`Index Scan`** | **17** ページ | 0.289 ms |

**まったく同じインデックスがあるのに、①だけ使われませんでした。7,255ページ 対 17ページ、約426倍の差です。**

**なぜか。** インデックスは **`order_date` の「加工前の生の値」** で並んでいます。`DATE()` を通した後の値がどこにあるかは、**全行を計算してみるまで分かりません**。だから並び順を使えず、全件走査するしかないのです。

**覚え方：「列を触ったら負け」。** 列はそのままにして、計算は右辺（条件側）に寄せてください。

| ❌ 効かない | ⭕ 直し方 |
| :--- | :--- |
| `WHERE DATE(order_date) = '2024-06-01'` | `WHERE order_date >= '2024-06-01' AND order_date < '2024-06-02'` |
| `WHERE age + 1 > 20` | `WHERE age > 19` |
| `WHERE customer_id::text = '1234'` | `WHERE customer_id = 1234` |

> どうしても加工した形で検索したい場合は、**関数インデックス**という手があります。
> ```sql
> CREATE INDEX idx_orders_date_func ON orders (DATE(order_date));
> ```
> これを作れば `DATE(order_date) = '2024-06-01'` でも `Index Scan` になります。

---

## 問題8：メモリに収まらないソート

### ① 既定（`work_mem = 4MB`）

```text
Limit  (actual time=... rows=1 loops=1)
  Buffers: shared hit=1235, temp read=1004 written=1006     ← 一時ファイルを使った
  ->  Sort  (actual time=... rows=100000 loops=1)
        Sort Key: phone
        Sort Method: external merge  Disk: 8032kB           ← メモリに収まらなかった
        ->  Seq Scan on customers  (rows=100000 loops=1)
```

### ②③

| | `Sort Method` | 使ったメモリ | `Execution Time`（目安） |
| :--- | :--- | ---: | ---: |
| ① 既定（4MB） | **`external merge`** | Disk **8,032** kB | 約 57 ms |
| ② 32MB | **`quicksort`** | Memory **11,666** kB | 約 30 ms |
| ③ `LIMIT 10` | **`top-N heapsort`** | Memory **26** kB | 約 10 ms |

**③は②の約450分の1のメモリ**で、しかも一番速く終わっています。

> **実行時間は1回ごとに大きくブレます**（同じクエリを3回流して 87ms → 60ms → 57ms ということが普通に起きます）。**`Sort Method` と使用メモリ量は毎回同じ値になる**ので、そちらで判断してください。

**ポイント3つ。**

1.  **既定の4MBでも溢れていました。** 10万行 × 約80バイト ＝ 約8MB のソートに4MBでは足りません。`Sort Method: external merge` と `temp` が出たら「メモリ不足でディスクを使った」という確定診断です。
2.  **必要量は `Disk:` の値から逆算できます。** 8,032kB と出ているので、16MB程度あれば `quicksort` になります。
3.  **`LIMIT` が付くと `top-N heapsort` に変わります。** 全件を並べ替えるのではなく「上位10件だけ保持する入れ物」を維持しながら流すので、メモリが26kBで済みます。**`ORDER BY` に `LIMIT` を付けられないかを考える**価値があります。

> ⚠️ `work_mem` は「1接続あたり」ではなく「**ソートやハッシュ1つあたり**」の上限です。全体設定を大きくするとメモリを食い潰すので、重い処理だけ `SET LOCAL` で個別に上げるのが定石です。**全体設定を変えるのはサーバー管理者の判断**なので、勝手に上げないでください。

---

## 最後に：棚卸し

```text
 テーブル  |       インデックス       | 使用回数 |  サイズ
-----------+--------------------------+----------+---------
 orders    | idx_orders_order_date    |        0 | 21 MB
 customers | idx_customers_city       |        1 | 688 kB
 orders    | idx_orders_status        |        1 | 6896 kB
 orders    | idx_orders_cust_cover    |        2 | 30 MB
 orders    | idx_orders_cust_date     |        2 | 30 MB
```

※ 使用回数は演習の進め方で前後します。

**問題7で作った `idx_orders_order_date` は、`Index Scan` に1回使われただけで 21MB を消費しています。** 使用回数が `0` のまま残っているインデックスがあれば、それは**検索を1ミリ秒も速くしていないのに、INSERT/UPDATEのたびに更新され、容量を食い続けている**ことになります。

実務では `idx_scan = 0` のインデックスを定期的に探して消します。ただし**「月次バッチでしか使わない」インデックスもある**ので、最低1ヶ月は様子を見てから判断してください。

---

## 演習全体のまとめ

この演習で確認したことを、1行ずつ振り返ってください。

| 問題 | 確認したこと |
| :--- | :--- |
| 1 | 性能の話は最後は「**何ページ読んだか**」。基準は `customers` 1,235 / `orders` 7,255 |
| 2 | インデックスで読むページが激減する。**散らばっていると `Bitmap`** になる |
| 3 | **同じインデックスでも、検索する値によって計画が変わる** |
| 4 | **外部キーにインデックスは自動で作られない**。結合は内側の `Seq Scan` を疑う |
| 5 | 複合インデックスは**左端から**。第2キー単独では使えない |
| 6 | 必要な列が全部インデックスにあれば**本体を読まない**（`Heap Fetches: 0`） |
| 7 | **列を加工したら負け**。インデックスがあっても使われなくなる |
| 8 | ソートが `work_mem` に収まらないと**ディスクに溢れる**（`external merge` / `temp`） |
