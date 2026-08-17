## 問題 1: 商品の価格帯を分類する（検索CASE式）
- **目的**: 検索CASE式（CASE WHEN ...）を使用して、数値データ（価格）を基に新しいカテゴリ文字列を作成して表示する方法を理解する。

### 問題:
`products_mst` テーブルから、各商品の **価格帯** を以下の基準で分類し、「商品名」「価格」「分類された価格帯（price_category）」を表示してください。

- **高価格帯**: 20,000 円以上
- **中価格帯**: 5,000 円以上 20,000 円未満
- **低価格帯**: 5,000 円未満

### 解答:
```sql
SELECT
    product_name,
    price,
    CASE
        WHEN price >= 20000 THEN '高価格帯'
        WHEN price >= 5000  THEN '中価格帯'
        ELSE '低価格帯'
    END AS price_category
FROM
    products_mst
ORDER BY
    price DESC;
```
> **解説**: CASE式は上から順に評価され、最初に真(True)になった条件が適用されます。

---

## 問題 2: 顧客の登録時期をセグメント化する（日付範囲）
- **目的**: CASE式と日付比較を組み合わせて、日付データを基に顧客を分類する方法を理解する。

### 問題:
`customers_mst` テーブルから、各顧客の **登録時期** を以下の基準で分類し、「顧客名」「登録日」「分類された登録時期（registration_segment）」を表示してください。

- **初期登録顧客**: 2023年1月1日 ～ 2023年3月31日
- **中期登録顧客**: 2023年4月1日 ～ 2023年6月30日
- **最近登録顧客**: 2023年7月1日以降
- **その他**: 上記以外

### 解答:
```sql
SELECT
    customer_name,
    created_date,
    CASE
        WHEN created_date BETWEEN '2023-01-01' AND '2023-03-31' THEN '初期登録顧客'
        WHEN created_date BETWEEN '2023-04-01' AND '2023-06-30' THEN '中期登録顧客'
        WHEN created_date >= '2023-07-01' THEN '最近登録顧客'
        ELSE 'その他'
    END AS registration_segment
FROM
    customers_mst
ORDER BY
    created_date ASC;
```

---

## 問題 3: 商品カテゴリごとの在庫状況を評価する（クロス集計の基礎）
- **目的**: `COUNT(CASE ... END)` というテクニックを使い、条件に応じた件数を数える方法（いわゆるピボット/クロス集計）を学ぶ。
  ※少し応用的な内容ですが、レポート作成で非常によく使います。

### 問題:
`products_mst` テーブルから、 **カテゴリごと** に以下の在庫状況別の商品数を集計して表示してください。

- **stock_abundant**: 在庫数が 100 個以上
- **stock_normal**: 在庫数が 10 個以上 100 個未満
- **stock_low**: 在庫数が 10 個未満

また、そのカテゴリの **合計商品数 (total_products)** も併せて表示してください。

### 解答:
```sql
SELECT
    category,
    COUNT(CASE WHEN stock_quantity >= 100 THEN 1 END) AS stock_abundant,
    COUNT(CASE WHEN stock_quantity >= 10 AND stock_quantity < 100 THEN 1 END) AS stock_normal,
    COUNT(CASE WHEN stock_quantity < 10 THEN 1 END) AS stock_low,
    COUNT(*) AS total_products
FROM
    products_mst
GROUP BY
    category
ORDER BY
    category;
```
> **解説**: `COUNT` 関数は `NULL` を無視します。CASE式で `ELSE` を省略すると、条件に合わない場合は `NULL` が返るため、条件に合った行だけが「1」となり、結果としてその件数がカウントされます。

---

## 問題 4: 特定カテゴリの商品を優先して並び替える（ORDER BYでのCASE式）
- **目的**: `ORDER BY` 句内で CASE 式を使用し、特定の条件を満たす行を強制的に先頭や末尾に持ってくる「カスタムソート」の手法を理解する。

### 問題:
`products_mst` テーブルから、商品を以下の優先順位で並び替えて表示してください。

1. **'Electronics'** カテゴリの商品を最優先
2. 次に **'Books'** カテゴリの商品
3. それ以外のカテゴリの商品

※各グループ内（同じ優先度内）では、**価格が高い順** に並び替えてください。

### 解答:
```sql
SELECT
    product_name,
    category,
    price
FROM
    products_mst
ORDER BY
    CASE category
        WHEN 'Electronics' THEN 1
        WHEN 'Books' THEN 2
        ELSE 3
    END ASC,
    price DESC;
```
> **解説**: `ORDER BY` 句でCASE式を使うと、カテゴリ名そのものではなく、「1, 2, 3」という変換後の数値に基づいて並び替えが行われます。

---

## 問題 5: 特定の顧客のみ注文日を調整して表示する（WHERE句でのCASE式）
- **目的**: `WHERE` 句で条件分岐を行いたい場合の記述方法と、実務的なベストプラクティス（論理演算子 `OR` の活用）を対比して学ぶ。

### 問題:
`orders_trn` テーブルから、以下の条件でデータを抽出してください。

- **customer_id が 1 （佐藤 太郎）の場合**: 2023年8月10日 **以降** の注文のみ表示
- **それ以外の顧客の場合**: 日付に関わらず、全ての注文を表示

※ 余裕があれば、「論理演算子 `OR` を使った書き方」と「`WHERE` 句にCASE式を書く方法」の2通りで書き、どちらが読みやすいか比べてみてください。

### 解答:
```sql
SELECT
    order_id,
    customer_id,
    order_date
FROM
    orders_trn
WHERE
    (customer_id = 1 AND order_date >= '2023-08-10')
    OR
    (customer_id <> 1)
ORDER BY
    customer_id, order_date;
```

**別解: CASE式を使う（非推奨）**
```sql
SELECT
    order_id,
    customer_id,
    order_date
FROM
    orders_trn
WHERE
    CASE 
        WHEN customer_id = 1 THEN order_date >= '2023-08-10'
        ELSE TRUE 
    END
ORDER BY
    customer_id, order_date;
```
> **注意**: SQLの `WHERE` 句は「行ごとにTrueかFalseかを判定する場所」なので、CASE式の結果として比較式（`order_date >= ...`）を返すことは通常できません（一部のDBを除く）。PostgreSQLでは上記のような `CASE WHEN ... THEN 比較式 ELSE TRUE END` がブール値を返す式として機能する場合がありますが、可読性が低いため通常は推奨されません。

---

## 問題 6: 商品メモの有無と在庫状況に応じた評価（NULL と複数条件）
- **目的**: CASE式内で `IS NULL` と複数の条件(`AND`)を組み合わせ、複雑なビジネスロジックを表現する。

### 問題:
`products_mst` テーブルから、各商品の **詳細状況** を以下の基準で評価し表示してください。

- **詳細充実・在庫あり**: メモが入力済(`IS NOT NULL`) かつ 在庫数が 50 個以上
- **詳細充実・在庫少**: メモが入力済(`IS NOT NULL`) かつ 在庫数が 50 個未満
- **詳細未入力・販売中**: メモが未入力(`IS NULL`) かつ 在庫数が 0 個より大きい
- **その他**: 上記以外（例: メモなしで在庫0など）

### 解答:
```sql
SELECT
    product_name,
    stock_quantity,
    memo,
    CASE
        WHEN memo IS NOT NULL AND stock_quantity >= 50 THEN '詳細充実・在庫あり'
        WHEN memo IS NOT NULL AND stock_quantity < 50  THEN '詳細充実・在庫少'
        WHEN memo IS NULL     AND stock_quantity > 0   THEN '詳細未入力・販売中'
        ELSE 'その他'
    END AS detail_and_stock_status
FROM
    products_mst
ORDER BY
    detail_and_stock_status, product_name;
```

---

# 追加課題 解答
---

## 追加問題 1: WHEN の書き順が結果を変える
- **目的**: `CASE` の `WHEN` が上から順に評価され最初に真になった枝で確定するため、条件が排他的でない場合は「優先したい条件を先に書く」必要があることを、キャンセル注文を例に理解する。

### 問題:
`orders_trn` テーブルの注文一覧に「ステータス（`status`）」列を付けます。分類ルールは次のとおりです。

- **キャンセル**: キャンセルされた注文（`deleted_at` に値が入っている）
- **当年度**: 上記以外で、注文日（`order_date`）が 2024-01-01 以降
- **前年度以前**: 上記以外

**(1)** 新人が次のSQLを書きました。実行して `order_id = 18` がどのステータスになるかを確認し、上のルールに照らして正しいかどうか答えてください。

```sql
SELECT
    order_id,
    order_date,
    deleted_at,
    CASE
        WHEN order_date >= '2024-01-01' THEN '当年度'
        WHEN deleted_at IS NOT NULL THEN 'キャンセル'
        ELSE '前年度以前'
    END AS status
FROM
    orders_trn
ORDER BY
    order_id;
```

**(2)** ルールどおりの結果になるよう、上のSQLを修正してください。

**(3)** 修正前と修正後で、各ステータスの件数がどう変わるか答えてください。

### 解答:
```sql
-- (1)
-- order_id = 18（2024-09-03 の注文・キャンセル済み）は「当年度」と判定される。
-- ルールでは「キャンセル」が正しいので、この結果は誤り。
-- 1つ目の WHEN（order_date >= '2024-01-01'）が先に真になり、
-- 2つ目の WHEN（deleted_at IS NOT NULL）はもう評価されないため。

-- (2) キャンセルの判定を先に書く
SELECT
    order_id,
    order_date,
    deleted_at,
    CASE
        WHEN deleted_at IS NOT NULL THEN 'キャンセル'
        WHEN order_date >= '2024-01-01' THEN '当年度'
        ELSE '前年度以前'
    END AS status
FROM
    orders_trn
ORDER BY
    order_id;

-- (3) 件数の変化
-- 修正前: キャンセル 1件 / 当年度 6件 / 前年度以前 11件（合計 18件）
-- 修正後: キャンセル 2件 / 当年度 5件 / 前年度以前 11件（合計 18件）
-- order_id = 18 が「当年度」から「キャンセル」へ移っただけで、合計は変わらない。
```

### 期待結果:

(1) 誤った順序 ―― キャンセル済みの2件が食い違う

| order_id | order_date | deleted_at | status |
| ---: | :--- | :--- | :--- |
| 9 | 2023-09-01 | 2023-09-02 11:30:00+09 | キャンセル |
| 18 | 2024-09-03 | 2024-09-04 09:15:00+09 | **当年度** ← 誤り |

(2) 修正後 ―― どちらも「キャンセル」になる

| order_id | order_date | deleted_at | status |
| ---: | :--- | :--- | :--- |
| 9 | 2023-09-01 | 2023-09-02 11:30:00+09 | キャンセル |
| 18 | 2024-09-03 | 2024-09-04 09:15:00+09 | **キャンセル** |

(3) 各ステータスの件数

| status | 修正前（誤った順序） | 修正後（正しい順序） |
| :--- | ---: | ---: |
| キャンセル | 1 | **2** |
| 当年度 | 6 | **5** |
| 前年度以前 | 11 | 11 |
| 合計 | 18 | 18 |

> **解説**: `CASE` は上から順に `WHEN` を評価し、最初に TRUE になった枝で確定して以降の `WHEN` を見ません。今回の2つの条件は排他的ではなく「2024年のキャンセル注文」が両方に当てはまるため、書き順がそのまま結果を変えます。実務では「キャンセル」「削除済み」のような除外・上書き系の条件を必ず先頭に書くこと、また合計は誤った順序でも18件のままなので合計件数だけ見ていても誤りに気づけないことを押さえてください。

---

## 追加問題 2: シンプルCASE式では NULL を判定できない
- **目的**: シンプルCASE式（`CASE 列 WHEN 値 …`）は内部的に `=` による等価比較であるため NULL を捕まえられず、NULL の分岐には検索CASE式と `IS NULL` が必要であることを理解する。

### 問題:
新人が「商品説明（`memo`）が未入力かどうか」を判定するために、次のSQLを書きました。

```sql
SELECT
    product_id,
    product_name,
    memo,
    CASE memo
        WHEN NULL THEN '説明なし'
        ELSE '説明あり'
    END AS memo_status
FROM
    products_mst
ORDER BY
    product_id;
```

**(1)** このSQLを実行すると `memo_status` はどうなりますか。`説明なし` は何件表示されるか答えてください。

**(2)** なぜそうなるのかを説明してください。

**(3)** `説明なし` が正しく 7 件表示されるように、このSQLを修正してください。

**(4)** シンプルCASE式が正しく使える例を、`category` 列を英語から日本語に置き換える形で1つ書いてください。

### 解答:
```sql
-- (1)
-- 全 23 件が「説明あり」になる。「説明なし」は 0 件。

-- (2)
-- シンプルCASE式 CASE memo WHEN NULL ... は、内部的に memo = NULL という
-- 等価比較を行う。SQLでは NULL との比較結果は TRUE でも FALSE でもなく UNKNOWN
-- になるため、WHEN NULL は絶対に真にならず、必ず ELSE に落ちる。

-- (3) 検索CASE式にして IS NULL で判定する
SELECT
    product_id,
    product_name,
    memo,
    CASE
        WHEN memo IS NULL THEN '説明なし'
        ELSE '説明あり'
    END AS memo_status
FROM
    products_mst
ORDER BY
    product_id;

-- (4) シンプルCASE式が正しく使える例（値の対応表）
SELECT
    product_id,
    category,
    CASE category
        WHEN 'Electronics' THEN '家電'
        WHEN 'Books'       THEN '書籍'
        WHEN 'Food'        THEN '食品'
        WHEN 'Toys'        THEN '玩具'
        WHEN 'Stationery'  THEN '文具'
        ELSE 'その他'
    END AS category_jp
FROM
    products_mst
ORDER BY
    product_id;
```

### 期待結果:

(1) 誤ったSQLの結果 ―― `memo` が NULL の行まで「説明あり」になる（抜粋）

| product_id | product_name | memo | memo_status |
| ---: | :--- | :--- | :--- |
| 1 | ワイヤレスイヤホン | 高音質でノイズキャンセリング機能付き | 説明あり |
| 2 | SQL 入門 | (NULL) | **説明あり** ← 誤り |
| 6 | オーガニックコーヒー豆 | (NULL) | **説明あり** ← 誤り |

`説明なし` は **0 件**（全 23 件が「説明あり」）。

(3) 修正後 ―― `説明なし` が **7 件**（`product_id` = 2, 6, 11, 18, 20, 21, 22）、`説明あり` が 16 件

(4) `category` を日本語化した結果の内訳

| category_jp | 件数 |
| :--- | ---: |
| 家電 | 5 |
| 書籍 | 5 |
| 食品 | 4 |
| 玩具 | 3 |
| 文具 | 3 |
| **その他** | **3** |

※ `その他` の 3 件は `Home & Kitchen`（`product_id` = 3, 7, 12）。`WHEN` に列挙し忘れたため `ELSE` に落ちている。

> **解説**: シンプルCASE式は内部で `列 = 値` の等価比較を行うため、`WHEN NULL` は `memo = NULL` となり結果が UNKNOWN になって決して真になりません。NULL を分岐させたいときは検索CASE式にして `IS NULL` を使います。(4) のような値の対応表ならシンプルCASE式の方が読みやすいのですが、`Home & Kitchen` が列挙漏れで `ELSE` に落ちているように、実データで漏れがないか必ず確認してください。

---

## 追加問題 3: UPDATE の SET 句で CASE を使う（`ELSE` 省略の罠）
- **目的**: 1つの `UPDATE` 文で行ごとに違う値をセットする `SET 列 = CASE …` の書き方を習得し、`ELSE` を省略すると条件に合わない行が NULL で潰れることを理解する。
### 問題:
> **注意**: この問題は `UPDATE` 文で `products_mst` のデータを実際に書き換えます。この章のあとも同じテーブルを使うので、**`BEGIN;` … `ROLLBACK;` で囲んで実行する**か、下の復旧SQLで必ず元の状態に戻してください。

`products_mst` の **販売中の商品（`deleted_at` が NULL）** だけを対象に、**1つの `UPDATE` 文** で `memo` を次のように更新してください。

- **在庫数が 0**: `memo` の先頭に `【欠品】` を付ける
- **在庫数が 1〜49**: `memo` の先頭に `【残りわずか】` を付ける
- **それ以外**: `memo` を変更しない

※ `memo` が未入力（NULL）の商品もあるため、連結するときは `COALESCE(memo, '')` で NULL を空文字に置き換えてください。

**(1)** 上の `UPDATE` を実行し、表示される「UPDATE 〇」の件数と、**実際に値が変わった行**の件数がそれぞれいくつか答えてください。

**(2)** (1) の `CASE` 式から `ELSE` を削除すると何が起きますか。まず理由を予想して答えたうえで、`BEGIN;` … `ROLLBACK;` で囲んで実際に確認してください。

**(3)** `COALESCE` を使わず `'【欠品】' || memo` と書いた場合、`product_id = 18`（ラジコンカー）はどうなりますか。

### 解答:
```sql
-- (1) 1つの UPDATE で、行ごとに違う値をセットする
UPDATE products_mst
SET memo = CASE
        WHEN stock_quantity = 0  THEN '【欠品】'       || COALESCE(memo, '')
        WHEN stock_quantity < 50 THEN '【残りわずか】' || COALESCE(memo, '')
        ELSE memo                -- ← 条件に合わない行は元の値のまま
    END
WHERE
    deleted_at IS NULL;
-- 「UPDATE 22」と表示される（WHERE に合致した行数＝販売中の 22 件）。
-- ただし実際に値が変わったのは 5 件（product_id = 7, 17, 18, 20, 23）だけ。

-- 実際に印が付いた行を確認する
SELECT
    product_id,
    product_name,
    stock_quantity,
    memo
FROM
    products_mst
WHERE
    memo LIKE '【%'
ORDER BY
    product_id;

-- (2) ELSE を削除した場合（必ず BEGIN 〜 ROLLBACK で囲んで確認する）
-- CASE はどの WHEN にも当てはまらず ELSE も無いとき NULL を返す。
-- UPDATE は WHERE に合致した 22 行すべてを書き換えるので、在庫 50 以上の 17 行は
-- memo が NULL で上書きされ、うち 13 行は入力済みの説明文が消えてしまう。
BEGIN;
UPDATE products_mst
SET memo = CASE
        WHEN stock_quantity = 0  THEN '【欠品】'       || COALESCE(memo, '')
        WHEN stock_quantity < 50 THEN '【残りわずか】' || COALESCE(memo, '')
    END
WHERE
    deleted_at IS NULL;
SELECT product_id, product_name, memo FROM products_mst ORDER BY product_id;
ROLLBACK;

-- (3) COALESCE を使わない場合（同じく BEGIN 〜 ROLLBACK で囲む）
-- '【欠品】' || NULL は NULL になるため、memo が未入力の product_id = 18 は
-- NULL のままとなり【欠品】の印が付かない（product_id = 20 も同様）。
BEGIN;
UPDATE products_mst
SET memo = CASE
        WHEN stock_quantity = 0  THEN '【欠品】'       || memo
        WHEN stock_quantity < 50 THEN '【残りわずか】' || memo
        ELSE memo
    END
WHERE
    deleted_at IS NULL;
SELECT product_id, product_name, memo FROM products_mst WHERE product_id IN (7, 17, 18, 20, 23) ORDER BY product_id;
ROLLBACK;

-- ★ 復旧SQL（(1) をそのまま実行してしまった場合は必ずこれを流す）
UPDATE products_mst SET memo = 'スムージー作りに最適'      WHERE product_id = 7;
UPDATE products_mst SET memo = '知育玩具・対象年齢3歳から' WHERE product_id = 17;
UPDATE products_mst SET memo = NULL                        WHERE product_id = 18;
UPDATE products_mst SET memo = NULL                        WHERE product_id = 20;
UPDATE products_mst SET memo = 'ギフト包装対応'            WHERE product_id = 23;

-- ★ 復旧の検証（memo が未入力の商品がちょうど 7 件に戻っていればOK）
SELECT
    product_id,
    product_name
FROM
    products_mst
WHERE
    memo IS NULL
ORDER BY
    product_id;
```

### 期待結果:

(1)「UPDATE 22」と表示されるが、実際に値が変わったのは 5 行

| product_id | product_name | stock_quantity | 更新後の memo |
| ---: | :--- | ---: | :--- |
| 7 | 高性能ブレンダー | 0 | 【欠品】スムージー作りに最適 |
| 17 | 木製パズル | 0 | 【欠品】知育玩具・対象年齢3歳から |
| 18 | ラジコンカー | 0 | 【欠品】 |
| 20 | （前後に空白）国産はちみつ | 30 | 【残りわずか】 |
| 23 | ぬいぐるみ（うさぎ） | 0 | 【欠品】ギフト包装対応 |

(2) `ELSE` を削除した場合 ―― 上の 5 行以外の 17 行の `memo` がすべて NULL になる（うち 13 行は入力済みだった説明文が消える）

(3) `COALESCE` なしの場合 ―― `product_id = 18` は `'【欠品】' || NULL` が NULL になるため `memo` は NULL のままで、`【欠品】` の印が付かない（`product_id = 20` も同じく NULL のまま）

復旧SQL実行後に `memo` が未入力の商品

| product_id | product_name |
| ---: | :--- |
| 2 | SQL 入門 |
| 6 | オーガニックコーヒー豆 |
| 11 | ゲーミングマウス |
| 18 | ラジコンカー |
| 20 | （前後に空白）国産はちみつ |
| 21 | A4 ノート 5冊セット |
| 22 | 蛍光マーカー 6色 |

> **解説**: `UPDATE` は `WHERE` に合致した行すべてを書き換えるため、`CASE` が NULL を返せばその NULL がそのまま書き込まれます。`ELSE memo`（元の列）を書かないと、条件に合わない行の値まで消えてしまうので、`SET 列 = CASE …` では `ELSE 元の列` を必ず書きます。「UPDATE 22」と表示されても実際に値が変わったのは 5 行であり、更新件数＝変更された件数ではない点も押さえてください。
> **⚠️ 講師向けの注意**: 演習後は必ず復旧SQLを流し、`memo` が未入力の商品が 7 件（`product_id` = 2, 6, 11, 18, 20, 21, 22）に戻っていることを確認してください。この状態を前提に 07 章の追加問題 2（`memo` 入力率）などが作られています。
