## 問題 1: 全商品数とカテゴリ数
- **目的**: `COUNT(*)` と `COUNT(DISTINCT 列名)` を使用して、テーブルの全行数と、重複を除いたユニークな要素数をカウントする違いを理解する。

### 問題:
`products_mst` テーブルについて、以下の2つの値を算出してください。

1. **total_products**: 登録されている全商品の数
2. **unique_categories**: 登録されているユニークな（重複を除いた）カテゴリの数

### 解答:
```sql
-- 1. 全商品数
SELECT COUNT(*) AS total_products
FROM products_mst;

-- 2. ユニークなカテゴリ数
SELECT COUNT(DISTINCT category) AS unique_categories
FROM products_mst;
```

---

## 問題 2: 商品の合計価格と平均価格
- **目的**: `SUM()` と `AVG()` を使用して、数値列の合計値と平均値を算出する。

### 問題:
`products_mst` テーブルに登録されている全商品の、**価格の合計** と **平均価格** をそれぞれ算出してください。

### 解答:
```sql
SELECT
    SUM(price) AS total_price,
    AVG(price) AS average_price
FROM
    products_mst;
```

---

## 問題 3: 最も安い商品と最も高い商品の価格
- **目的**: `MIN()` と `MAX()` を使用して、列の最小値と最大値を特定する。

### 問題:
`products_mst` テーブルから、**最も安い商品の価格** と **最も高い商品の価格** をそれぞれ取得してください。

### 解答:
```sql
SELECT
    MIN(price) AS min_product_price,
    MAX(price) AS max_product_price
FROM
    products_mst;
```

---

## 問題 4: カテゴリごとの商品数と平均価格
- **目的**: `GROUP BY` 句を使用して特定の列（カテゴリ）でデータをグループ化し、グループごとに集計関数を実行する。

### 問題:
`products_mst` テーブルから、**カテゴリごと** の「商品数」と「平均価格」を算出してください。

### 解答:
```sql
SELECT
    category,
    COUNT(*) AS product_count,
    AVG(price) AS average_price
FROM
    products_mst
GROUP BY
    category;
```

---

## 問題 5: 高価格帯の商品が多いカテゴリを特定する
- **目的**: `HAVING` 句を使用して、`GROUP BY` で集計された**結果に対して**条件を指定し、フィルタリングを行う。

### 問題:
`products_mst` テーブルから、**平均価格が 5,000 円より大きい** カテゴリのみを抽出し、その「カテゴリ名」「商品数」「平均価格」を表示してください。

### 解答:
```sql
SELECT
    category,
    COUNT(*) AS product_count,
    AVG(price) AS average_price
FROM
    products_mst
GROUP BY
    category
HAVING
    AVG(price) > 5000;
```
> **解説**: 集計結果（平均価格）に対する条件なので、`WHERE` ではなく `HAVING` を使います。

---

## 問題 6: 在庫が特定の数量以上の商品のカテゴリ別集計
- **目的**: `WHERE` 句（集計前の行フィルタリング）と、`GROUP BY` + `HAVING`（集計後のグループフィルタリング）の処理順序と使い分けを理解する。

### 問題:
`products_mst` テーブルから、以下の条件で集計を行ってください。

1. **集計対象**: 在庫数(`stock_quantity`)が **100 個以上** の商品のみ
2. **集計単位**: カテゴリごと
3. **表示条件**: 集計した結果、商品数が **2 つ以上** あるカテゴリのみを表示

表示項目は「カテゴリ名」と「商品数」です。

### 解答:
```sql
SELECT
    category,
    COUNT(*) AS product_count
FROM
    products_mst
WHERE
    stock_quantity >= 100 -- 1. まず、在庫数 100 個以上の商品に絞り込む (WHERE)
GROUP BY
    category
HAVING
    COUNT(*) >= 2; -- 2. その後、グループ化された結果で商品数が 2 つ以上のものを抽出 (HAVING)
```

---

## 問題 7: 注文ごとの異なる商品点数と合計購入個数
- **目的**: `COUNT(DISTINCT 列名)` と `SUM()` を組み合わせて、各グループ内でユニークな要素の数と合計を算出する。

### 問題:
`order_details_trn` テーブルから、注文ID(`order_id`)ごとに以下の2つを算出してください。

1. **異なる商品の種類数** (distinct_product_count)
2. **合計購入個数** (total_quantity_ordered)

### 解答:
```sql
SELECT
    order_id,
    COUNT(DISTINCT product_id) AS distinct_product_count,
    SUM(quantity) AS total_quantity_ordered
FROM
    order_details_trn
GROUP BY
    order_id;
```
> **補足**: このテーブルの主キーは `(order_id, product_id)` なので、1つの注文の中に同じ `product_id` が重複して登録されることは本来ありません。そのため `COUNT(*)` でも結果は同じになりますが、「商品の種類数」を数えるという意味を明確にするため `COUNT(DISTINCT ...)` を使用しています。

---

## 問題 8: 顧客の登録年ごとの顧客数
- **目的**: `EXTRACT()` 関数で日付から「年」を抽出し、その計算結果でグループ化して集計を行う。

### 問題:
`customers_mst` テーブルから、顧客の **登録年** (`created_date` の年部分) ごとに、登録された顧客の数を算出してください。
結果は年の順に並べてください。

### 解答:
```sql
SELECT
    EXTRACT(YEAR FROM created_date) AS registration_year,
    COUNT(*) AS customer_count
FROM
    customers_mst
GROUP BY
    registration_year -- PostgreSQLではSELECT句のエイリアスをGROUP BYで使用可能
ORDER BY
    registration_year;
```
> **解説（重要）**: 標準SQLでは、`GROUP BY` 句の中で `SELECT` 句で付けた別名（エイリアス）を使うことはできません（`GROUP BY EXTRACT(...)` と書く必要があります）。PostgreSQLやMySQLなど一部のDBMSではエイリアスが使用可能ですが、環境依存であることを覚えておきましょう。

---

## 問題 9: 注文から90日後の月別集計
- **目的**: 日付の加算、`EXTRACT()` 関数、`GROUP BY` を組み合わせ、計算後の値に基づいて集計する応用力を養う。

### 問題:
`orders_trn` テーブルについて、各注文の「注文日から90日後」の日付を計算し、その **「90日後の日付」の月ごと** に、該当する注文の数を算出してください。

### 解答:
```sql
SELECT
    EXTRACT(MONTH FROM (order_date + INTERVAL '90 days')) AS calculated_month,
    COUNT(*) AS orders_count
FROM
    orders_trn
GROUP BY
    calculated_month
ORDER BY
    calculated_month;
```

### 期待結果:
| calculated_month | orders_count |
| ---: | ---: |
| 5 | 2 |
| 6 | 1 |
| 10 | 1 |
| 11 | 10 |
| 12 | 4 |

> **⚠️ 講師向けの注意**: 注文データには2023年と2024年の両方が含まれているため、`EXTRACT(MONTH)` だけでグループ化すると **異なる年の同じ月が合算されます**（11月の10件は 2023-11 と 2024-11 の合計）。
> 設問の指示どおりなので解答としては正しいのですが、**月次集計としては壊れている**という点が重要です。この欠陥をそのまま教材にしたのが **追加問題8** なので、続けて出題すると効果的です。

---

# 追加課題 解答

> **前提**: 以下の期待結果はすべて `03_解答/02_DDL（前半用）.md` の初期データを投入した状態で **実際に PostgreSQL 17 で実行して確認済み** です。
> 02〜03章（INSERT / UPDATE / DELETE）の演習を実行した DB では件数がずれます。数字を合わせたい場合は初期データを再投入してください。

## データ状況（講師用メモ）

`products_mst` は全 **23件**（うち販売終了1件＝`product_id=11` ゲーミングマウス）、カテゴリは **6種類**。

| カテゴリ | 商品ID | 販売中 | 在庫0 | memo未入力 |
| :--- | :--- | :--- | :--- | :--- |
| Electronics | 1, 4, 8, 11, 14 | 4（11は販売終了） | なし | 11 |
| Books | 2, 5, 9, 13, 19 | 5 | なし | 2 |
| Home & Kitchen | 3, 7, 12 | 3 | 7 | なし |
| Food | 6, 10, 15, 20 | 4 | なし | 6, 20 |
| Stationery | 16, 21, 22 | 3 | なし | 21, 22 |
| Toys | 17, 18, 23 | 3 | **17, 18, 23（全部）** | 18 |

07章の追加課題のために仕込まれている行は以下です。

| 商品ID | 仕込み | どの問題で使うか |
| :--- | :--- | :--- |
| 17, 18, 23 | Toys ＝ 全商品が在庫0 | 追加問題6(2)(3)、7 |
| 19 | `SQL 入門` が `product_id=2` と完全に同名 | 追加問題4(1) |
| 20 | `' 国産はちみつ '` が `product_id=15` と前後空白違い | 追加問題4(2)(3) |
| 21, 22 | memo未入力（Stationeryの入力率を下げる） | 追加問題2 |

`orders_trn` は全 **18件**（うちキャンセル2件＝`order_id=9, 18`）。`order_id=12〜18` が追加課題用で、**2024年の注文（13〜18）** が追加問題5・8の鍵になります。

---

## 追加問題 1: カテゴリ別の在庫評価額ランキング

### 解答:
```sql
SELECT
    category,
    COUNT(*) AS product_count,
    SUM(price * stock_quantity) AS inventory_value
FROM
    products_mst
WHERE
    deleted_at IS NULL          -- 販売終了商品を集計対象から外す
GROUP BY
    category
ORDER BY
    inventory_value DESC;
```

### 期待結果:
| category | product_count | inventory_value |
| :--- | ---: | ---: |
| Electronics | 4 | 6,525,600.00 |
| Books | 5 | 1,826,000.00 |
| Stationery | 3 | 1,626,600.00 |
| Food | 4 | 1,452,000.00 |
| Home & Kitchen | 3 | 965,000.00 |
| Toys | 3 | 0.00 |

### 💡 講師用の解説ポイント

**① `SUM(price * stock_quantity)` は「行ごとの計算 → 集計」の順**
`SUM(price) * SUM(stock_quantity)` と書くと全く違う数字になります。Electronics（販売中4件）で試すと
`(12800+29800+1500+3980) × (150+100+500+220) = 48,080 × 970 = 46,637,600` と、正解の約7倍になります。**この誤りは実務でも実際に起きます**ので、板書して差を見せると効果的です。

**② `WHERE deleted_at IS NULL` を外させてみる**
外すと Electronics が **7,071,600**（販売終了のゲーミングマウス `7800 × 70 = 546,000` が混入）になります。
「もう売らない商品を在庫評価額に入れてはいけない」という業務ルールが、SQL の1行に対応していることを実感させます。**論理削除を扱うテーブルでは、集計の前に必ず除外する**のが実務の作法です。

**③ Toys が 0 円になる**
在庫評価額0＝そのカテゴリは全商品が欠品、ということが数字から読み取れます。追加問題6(2) への伏線です。

**④ 既存の問題2との対比**
問題2の `SUM(price)`（価格の合計）は、実務ではまず使いません。「単価を足しても業務上の意味がない」ことと、「金額を集計するには数量との掛け算が必要」であることをセットで説明すると、問題2の位置づけもはっきりします。

---

## 追加問題 2: 商品マスタの整備状況レポート

### 解答:
```sql
SELECT
    category,
    COUNT(*)                     AS total,
    COUNT(deleted_at)            AS discontinued,   -- deleted_atがNULLでない件数
    COUNT(*) - COUNT(deleted_at) AS active,
    COUNT(memo)                  AS memo_filled,    -- memoがNULLでない件数
    ROUND(100.0 * COUNT(memo) / COUNT(*), 1) AS memo_filled_rate
FROM
    products_mst
GROUP BY
    category
ORDER BY
    memo_filled_rate;
```

### 期待結果:
| category | total | discontinued | active | memo_filled | memo_filled_rate |
| :--- | ---: | ---: | ---: | ---: | ---: |
| Stationery | 3 | 0 | 3 | 1 | 33.3 |
| Food | 4 | 0 | 4 | 2 | 50.0 |
| Toys | 3 | 0 | 3 | 2 | 66.7 |
| Electronics | 5 | 1 | 4 | 4 | 80.0 |
| Books | 5 | 0 | 5 | 4 | 80.0 |
| Home & Kitchen | 3 | 0 | 3 | 3 | 100.0 |

### 💡 講師用の解説ポイント

**① `COUNT(列名)` の性質を「削除済み件数」に転用する**
講義資料では `COUNT(price)` を「価格が設定されている商品数」として説明していますが、同じ性質は逆向きにも使えます。`deleted_at` は「削除された行だけ値が入る」列なので、`COUNT(deleted_at)` がそのまま **削除済み件数** になります。
`WHERE` を使わずに「全体・削除済み・有効」を1行で並べられるのがこの書き方の強みです。`WHERE deleted_at IS NOT NULL` で絞ると、削除された商品が0件のカテゴリは行ごと消えてしまいます。

**② 整数除算の罠（ここが本題）**
`100 * COUNT(memo) / COUNT(*)` と書くと、`COUNT` の戻り値は `bigint` のため **整数同士の割り算**になり、小数が切り捨てられます。

| category | `100.0 *` を付けた場合 | `100 *` と書いた場合 |
| :--- | ---: | ---: |
| Stationery | 33.3 | **33** |
| Toys | 66.7 | **66** |

割合を出すときは「**分子か分母のどちらかを必ず小数にする**」（`100.0 *` や `::numeric` を付ける）と覆えます。この事故は割合・達成率・構成比を計算するあらゆる場面で起きるので、必ず一度踏ませておきたいポイントです。

**③ 実務での使いどころ**
マスタの整備状況レポートは、データ移行の受け入れテストや、「商品説明の入力を現場に依頼する」ための根拠資料としてそのまま使えます。`ORDER BY memo_filled_rate` で入力率の低いカテゴリを上に出すと、そのまま依頼先の優先順位になります。

---

## 追加問題 3: 価格帯ごとの商品分布（ヒストグラム）

### 解答:
```sql
SELECT
    CASE
        WHEN price <  3000 THEN '1:～2,999円'
        WHEN price < 10000 THEN '2:3,000～9,999円'
        ELSE                    '3:10,000円～'
    END AS price_band,
    COUNT(*) AS product_count,
    ROUND(AVG(price)) AS avg_price
FROM
    products_mst
WHERE
    deleted_at IS NULL
GROUP BY
    price_band      -- PostgreSQLではSELECT句の別名をGROUP BYで使用可能
ORDER BY
    price_band;
```

### 期待結果:
| price_band | product_count | avg_price |
| :--- | ---: | ---: |
| 1:～2,999円 | 12 | 1803 |
| 2:3,000～9,999円 | 8 | 5360 |
| 3:10,000円～ | 2 | 21300 |

### 💡 講師用の解説ポイント

**① CASE 式は「グループ化の軸」にもなれる**
06_CASE式 の問題1 で作った価格帯の CASE 式は、そこでは「表示用の列」でした。今回はそれを `GROUP BY` の対象にしています。
「CASE 式は `SELECT` 句に書くもの」という思い込みを外すのがこの問題の主目的です。`GROUP BY` はテーブルの列だけでなく **式でもグループ化できる**（問題8・9で `EXTRACT` を使ったのと同じ話）と接続すると納得しやすくなります。

**② 標準SQLで書く場合**
`GROUP BY price_band` は PostgreSQL の拡張です。標準SQLに寄せるなら CASE 式を `GROUP BY` にもう一度書きます（問題8の解説と同じ論点）。
```sql
GROUP BY CASE WHEN price < 3000 THEN '1:～2,999円' ... END
```
毎回書くのが冗長なので、**サブクエリ（10章）や CTE で価格帯の列を作ってから `GROUP BY` する**のが実務では一般的です。ここは10章への伏線として触れる程度で十分です。

**③ ラベルに `1:` `2:` `3:` を付けている理由**
文字列でソートするため、`'～2,999円'` のままだと意図した順に並びません。**並び順を制御するために番号を前置する**のは実務でよく使う小技です（06_CASE式 の問題4「ORDER BY での CASE 式」と同じ発想）。

---

## 追加問題 4: 商品名の重複チェック（マスタの名寄せ）

### 解答:
```sql
-- (1) 完全一致の重複を検出
SELECT
    product_name,
    COUNT(*) AS cnt,
    MIN(product_id) AS min_id,
    MAX(product_id) AS max_id
FROM
    products_mst
GROUP BY
    product_name
HAVING
    COUNT(*) > 1;

-- (2)
-- product_id=15 は '国産はちみつ'
-- product_id=20 は ' 国産はちみつ '（前後に半角スペースがある）
-- GROUP BY は文字列が1文字でも違えば別グループとして扱うため、
-- 空白の有無だけで別の商品名と判定され、重複として検出されない。

-- (3) 前後の空白を取り除いてからグループ化する
SELECT
    TRIM(product_name) AS normalized_name,
    COUNT(*) AS cnt,
    MIN(product_id) AS min_id,
    MAX(product_id) AS max_id
FROM
    products_mst
GROUP BY
    TRIM(product_name)
HAVING
    COUNT(*) > 1;
```

### 期待結果:

(1) の結果 ―― 1件だけ検出される
| product_name | cnt | min_id | max_id |
| :--- | ---: | ---: | ---: |
| SQL 入門 | 2 | 2 | 19 |

(3) の結果 ―― 表記ゆれも拾えて2件になる
| normalized_name | cnt | min_id | max_id |
| :--- | ---: | ---: | ---: |
| 国産はちみつ | 2 | 15 | 20 |
| SQL 入門 | 2 | 2 | 19 |

### 💡 講師用の解説ポイント

**① `GROUP BY` + `HAVING COUNT(*) > 1` は実務で最頻出**
この形は「重複検出」の定型句です。使う場面は、データ移行後の検証、`UNIQUE` 制約を追加する前の事前調査、名寄せ作業の洗い出しなど。**`HAVING` の最も実務的な用途**と言えます。

**② 「該当0件が正常」という調査クエリの型に慣れさせる**
これまでの問題は「結果が出て当たり前」でしたが、重複チェックは0件が正常です。**結果が出たら異常**という読み方をする問題を1問入れておくと、調査系のクエリに対する構えが変わります。

**③ `MIN(product_id)` / `MAX(product_id)` を出す理由**
「どちらを残してどちらを消すか」を判断するためです。実務では若い ID を正、後から登録された方を誤登録と見なして統合する（または論理削除する）ことが多いので、この2列がそのまま作業指示になります。

**④ さらに実務的な注意（時間があれば）**
- **全角スペースは `TRIM` では落ちません**。PostgreSQL の `TRIM` は既定で半角スペースだけを対象にするため、`TRIM(BOTH '　' FROM ...)` や `REPLACE(product_name, '　', '')` の併用が必要です。
- 英字商品名の大文字・小文字ゆれには `LOWER()` を重ねます（`GROUP BY LOWER(TRIM(product_name))`）。
- **正規化してから比較する**という考え方自体が本質で、どの関数を重ねるかはデータの汚れ方次第、という整理で締めると綺麗です。

---

## 追加問題 5: リピーター顧客と休眠顧客の抽出

### 解答:
```sql
-- (1) リピーター顧客
SELECT
    customer_id,
    COUNT(*) AS order_count,
    MIN(order_date) AS first_order_date,
    MAX(order_date) AS last_order_date,
    MAX(order_date) - MIN(order_date) AS active_days
FROM
    orders_trn
WHERE
    deleted_at IS NULL
GROUP BY
    customer_id
HAVING
    COUNT(*) >= 2
ORDER BY
    order_count DESC;

-- (2) 休眠顧客
SELECT
    customer_id,
    COUNT(*) AS order_count,
    MAX(order_date) AS last_order_date
FROM
    orders_trn
WHERE
    deleted_at IS NULL
GROUP BY
    customer_id
HAVING
    MAX(order_date) < '2024-01-01'
ORDER BY
    last_order_date;

-- (3)
-- customer_id=6（高橋 明）: 注文は1件あるが、それがキャンセル済み（order_id=9）。
--   WHERE deleted_at IS NULL で行が全部消えるため、グループそのものが作られない。
--   （customer_id=7 中村友子も order_id=18 がキャンセルだが、order_id=10 が生きているので現れる）
-- customer_id=9（伊藤 さやか）: orders_trn に1件も注文がない。
--   元から行が存在しないため、当然グループも作られない。
-- いずれも「orders_trn だけを見ている限り検出できない」。
-- 顧客マスタ側を基準にした集計は、外部結合（08章）やサブクエリ（10章）が必要になる。
```

### 期待結果:

(1) リピーター顧客
| customer_id | order_count | first_order_date | last_order_date | active_days |
| ---: | ---: | :--- | :--- | ---: |
| 1 | 6 | 2023-08-01 | 2024-08-21 | 386 |
| 2 | 4 | 2023-08-05 | 2024-08-07 | 368 |
| 3 | 2 | 2023-08-12 | 2023-09-15 | 34 |

(2) 休眠顧客
| customer_id | order_count | last_order_date |
| ---: | ---: | :--- |
| 4 | 1 | 2023-08-15 |
| 5 | 1 | 2023-08-22 |
| 7 | 1 | 2023-09-05 |
| 8 | 1 | 2023-09-10 |
| 3 | 2 | 2023-09-15 |

### 💡 講師用の解説ポイント

**① `MIN` / `MAX` は日付にも使える**
講義資料には「数値だけでなく、文字列や日付型にも使用できます」と書いてありますが、問題1〜9では日付に使う問題がありませんでした。ここで「**`MIN(order_date)` = 初回購入日、`MAX(order_date)` = 最終購入日**」という、実務で最もよく使う `MIN`/`MAX` の用途を押さえます。

**② `日付 - 日付 = 整数（日数）`**
PostgreSQL では `date` 型同士の引き算が日数（integer）になります。`AGE()` を使うと「1年20日」のような interval になるので、**日数で欲しいのか、年月日で欲しいのか**で使い分けます。

**③ `HAVING` には `SELECT` に出していない集計関数も書ける**
(2) の `HAVING MAX(order_date) < '2024-01-01'` は「集計した結果で絞り込む」典型例です。`WHERE order_date < '2024-01-01'` にすると **「2024年より前に1回でも注文した顧客」** という全く違う意味になります。ここは `WHERE` と `HAVING` の違いを問い直す絶好の材料なので、両方実行させて結果を比べさせてください（`WHERE` 版だと現役の顧客1・2も出てきてしまいます）。

**④ (3) が一番大事 ―― 単一テーブル集計の限界**
`GROUP BY` は「**そのテーブルに存在する行**」からしかグループを作れません。したがって
- 注文が全部キャンセルされた顧客（customer_id=6）
- 一度も注文していない顧客（customer_id=9）

は、`orders_trn` をどう集計しても絶対に出てきません。**「0件」を表示するには顧客マスタ側を基準にする必要があり、それが `LEFT JOIN`（08章）や `NOT EXISTS`（10章）の存在理由です。**
この問題を08章の導入として使うと、JOIN の必要性が「習ったから使う」ではなく「これが無いと解けない」として腹落ちします。

---

## 追加問題 6: 発注アラートレポート

### 解答:
```sql
-- (1) 在庫100個以上の商品が半数以上を占めるカテゴリ
SELECT
    category,
    COUNT(*) AS product_count,
    COUNT(CASE WHEN stock_quantity >= 100 THEN 1 END) AS abundant_count
FROM
    products_mst
WHERE
    deleted_at IS NULL
GROUP BY
    category
HAVING
    COUNT(CASE WHEN stock_quantity >= 100 THEN 1 END) >= COUNT(*) * 0.5
ORDER BY
    category;

-- (2) 全商品が在庫0のカテゴリ（全称命題）
SELECT
    category,
    COUNT(*) AS product_count
FROM
    products_mst
WHERE
    deleted_at IS NULL
GROUP BY
    category
HAVING
    COUNT(*) = COUNT(CASE WHEN stock_quantity = 0 THEN 1 END);

-- (3) 発注アラート付きの一覧
SELECT
    category,
    SUM(stock_quantity) AS total_stock,
    COUNT(CASE WHEN stock_quantity = 0 THEN 1 END) AS out_of_stock_count,
    CASE
        WHEN COUNT(CASE WHEN stock_quantity = 0 THEN 1 END) > 0 THEN '要発注'
        ELSE '-'
    END AS alert
FROM
    products_mst
WHERE
    deleted_at IS NULL
GROUP BY
    category
ORDER BY
    out_of_stock_count DESC, category;
```

### 期待結果:

(1) 品揃えが潤沢なカテゴリ
| category | product_count | abundant_count |
| :--- | ---: | ---: |
| Books | 5 | 3 |
| Electronics | 4 | 4 |
| Food | 4 | 3 |
| Stationery | 3 | 3 |

※ `Home & Kitchen`（3件中1件）と `Toys`（3件中0件）が除外されます。

(2) 全商品が在庫0のカテゴリ
| category | product_count |
| :--- | ---: |
| Toys | 3 |

(3) 発注アラート付きの一覧
| category | total_stock | out_of_stock_count | alert |
| :--- | ---: | ---: | :--- |
| Toys | 0 | 3 | 要発注 |
| Home & Kitchen | 190 | 1 | 要発注 |
| Books | 720 | 0 | - |
| Electronics | 970 | 0 | - |
| Food | 910 | 0 | - |
| Stationery | 1220 | 0 | - |

### 💡 講師用の解説ポイント

**① `COUNT(CASE WHEN 条件 THEN 1 END)` の仕組みを毎回言語化させる**
`ELSE` を書かないので、条件に合わない行は `NULL` になります。`COUNT` は `NULL` を数えないので、結果として **条件に合う行だけの件数** になります。講義資料 §6.1 の内容ですが、口で言えるまで反復させる価値があります。

**② (1) は「構成比」を `HAVING` で判定している**
`COUNT(条件) >= COUNT(*) * 0.5` は「**割り算せずに比率を判定する**」書き方です。
`COUNT(条件) / COUNT(*) >= 0.5` と書くと追加問題2と同じ整数除算に落ちるため（`3/5` が `0` になる）、**掛け算の形に移項しておくのが定石**です。ここは追加問題2と必ずセットで説明してください。

**③ (2) は全称命題 ―― `全体の件数 = 条件に合う件数`**
「全部が○○」を SQL で直接書く構文はありません。**「全体と一致するか」に言い換える**のがテクニックです。
`WHERE stock_quantity = 0` で絞ってから `GROUP BY` しても Toys は出てきますが、それでは「1件だけ欠品している Home & Kitchen」も一緒に出てしまい、**「全部欠品」と「一部欠品」を区別できません**。両方実行させて比べると、`HAVING` を使う理由がはっきりします。

**④ (3) は集計関数を CASE の条件に使う**
`SELECT` 句の CASE 式の中に集計関数を書けます（`HAVING` と同じく、グループ化後に評価されるため）。逆に **`WHERE` 句には集計関数を書けません**（グループ化前に評価されるため）。評価順序（講義§5）の復習になります。
なお `alert` 列を作る代わりに (2) のように `HAVING` で絞る形もありますが、**レポートは「全カテゴリを出して状態を並べる」方が現場では使いやすい**ことが多いです。「絞るのか、印を付けるのか」は要件次第、という話ができます。

---

## 追加問題 7: 条件付き平均と `ELSE 0` の罠

### 解答:
```sql
SELECT
    category,
    ROUND(AVG(price), 2) AS avg_all,
    ROUND(AVG(CASE WHEN stock_quantity > 0 THEN price END), 2) AS avg_in_stock,
    ROUND(AVG(CASE WHEN stock_quantity > 0 THEN price ELSE 0 END), 2) AS avg_wrong
FROM
    products_mst
WHERE
    deleted_at IS NULL
GROUP BY
    category
ORDER BY
    category;
```

### 期待結果:
| category | avg_all | avg_in_stock | avg_wrong |
| :--- | ---: | ---: | ---: |
| Books | 2760.00 | 2760.00 | 2760.00 |
| Electronics | 12020.00 | 12020.00 | 12020.00 |
| Food | 1600.00 | 1600.00 | 1600.00 |
| Home & Kitchen | 6600.00 | **5000.00** | **3333.33** |
| Stationery | 1510.00 | 1510.00 | 1510.00 |
| Toys | 4833.33 | **(NULL)** | **0.00** |

### 💡 講師用の解説ポイント

**① `Home & Kitchen` で3つの値が全部違う（ここが本題）**
販売中の3商品は 電気ケトル 4,500円（在庫80）／高性能ブレンダー 9,800円（在庫**0**）／セラミックフライパン 5,500円（在庫110）です。

| 列 | 計算式 | 結果 |
| :--- | :--- | ---: |
| `avg_all` | (4500 + 9800 + 5500) ÷ **3** | 6600.00 |
| `avg_in_stock` | (4500 + 5500) ÷ **2** | 5000.00 |
| `avg_wrong` | (4500 + **0** + 5500) ÷ **3** | 3333.33 |

`ELSE 0` と書くと、**除外したかった行が「0円の商品」として分母に残ります**。分子からは消えるのに分母には残る、という点が事故の本質です。講義資料 §6.2 の内容がそのまま数字で見えるので、この表を板書して見せるのが最も効きます。

なお `Electronics`・`Books`・`Food`・`Stationery` は在庫0の商品が無いため3列とも同じ値になります。**差が出るのは在庫0の商品を含むカテゴリだけ**という点も確認させてください。

**② `Toys` で NULL と 0 の違いが出る**
Toys は在庫のある商品が1件もないため、
- `avg_in_stock` → 集計対象の行が全部 NULL なので **結果も NULL**（「在庫がある商品が無いので平均は定義できない」＝業務的に正しい）
- `avg_wrong` → 0円が3件あることになり **0.00**（「平均単価0円のカテゴリ」という嘘の数字）

**NULL は「値が無い」、0 は「0という値がある」** という区別が、そのままレポートの正しさに直結します。NULL のままだと見栄えが悪い場合は、平均を計算した後で `COALESCE(AVG(...), 0)` するのか、そもそも行を出さないのか、**業務としてどう表示したいかを決めてから書く**という順序を伝えてください。

**③ `SUM` なら `ELSE 0` でも良い**
`SUM(CASE WHEN 条件 THEN price ELSE 0 END)` は、0を足しても合計が変わらないので結果は正しくなります。**割り算（＝分母が関わる集計）のときだけ `ELSE 0` が危険**、という切り分けを添えると混乱しません。

---

## 追加問題 8: 月次集計 ―― 年をまたいだ瞬間に壊れる集計

### 解答:
```sql
-- (1) 問題9と同じ考え方（EXTRACT(MONTH) だけでグループ化）
SELECT
    EXTRACT(MONTH FROM order_date) AS order_month,
    COUNT(*) AS order_count
FROM
    orders_trn
WHERE
    deleted_at IS NULL
GROUP BY
    order_month
ORDER BY
    order_month;

-- (2)
-- 「8月」が10件になっている。
-- これは 2023年8月の8件 と 2024年8月の2件 を合算した数字。
-- EXTRACT(MONTH) は月の数字しか取り出さないため、年の情報が失われ、
-- 異なる年の同じ月が同じグループにまとめられてしまう。

-- (3) 年月でグループ化する
SELECT
    TO_CHAR(order_date, 'YYYY-MM') AS year_month,
    COUNT(*) AS order_count
FROM
    orders_trn
WHERE
    deleted_at IS NULL
GROUP BY
    year_month
ORDER BY
    year_month;
```

### 期待結果:

(1) 壊れている集計
| order_month | order_count |
| ---: | ---: |
| 2 | 2 |
| 3 | 1 |
| 8 | **10** ← 2023年8月と2024年8月が合算されている |
| 9 | 3 |

(3) 正しい月次集計
| year_month | order_count |
| :--- | ---: |
| 2023-08 | 8 |
| 2023-09 | 3 |
| 2024-02 | 2 |
| 2024-03 | 1 |
| 2024-08 | 2 |

### 💡 講師用の解説ポイント

**① 「既存の解答が、データが増えた瞬間に間違いになる」体験**
問題9の書き方は、注文データが2023年しかなければ正しく見えます。**2024年の注文（`order_id=13〜18`）が入っている今のデータでは、`EXTRACT(MONTH)` だけで括った瞬間に年が潰れます。**
**テスト環境では通っていたクエリが、運用してデータが溜まった翌年に間違った数字を出し始める** ―― 実務で本当によくある事故です。問題8・9を解いた直後にこの問題を出すと、インパクトが最大になります。

**② 月次集計は `YYYY-MM` で括るのが原則**
`TO_CHAR(order_date, 'YYYY-MM')` が最も簡潔です。他に
- `GROUP BY EXTRACT(YEAR FROM order_date), EXTRACT(MONTH FROM order_date)` ―― 年と月を2列に分ける
- `GROUP BY DATE_TRUNC('month', order_date)` ―― 月初の日付に丸める（並び替えやグラフ化がしやすく、実務ではこれもよく使います）

があります。**「年を落とさない」ことが本質**で、書き方はどれでも構いません。

**③ `EXTRACT(MONTH)` が正しい場面もある**
「季節性を見たい（何月が繁忙期か）」という分析では、年を潰して月だけで集計するのが正解です。**問題9のクエリは間違いではなく、目的が違う**という整理をしておくと、「EXTRACT は使うな」という誤った学習になりません。

**④ 2024-09 が結果に出てこないこと**
`order_id=18`（2024-09-03）はキャンセル済みなので `WHERE deleted_at IS NULL` で消え、2024-09 の行自体が現れません。追加問題5(3) と同じ「**0件の行は集計結果に出てこない**」という話で、月次レポートで「欠けた月」を0件として表示するには別の手段（カレンダーテーブルとの外部結合など）が必要になります。08章以降への伏線として触れられます。

---

## 追加問題 9: 商品別の売れ筋ランキング（指標の選び方）

### 解答:
```sql
-- (1) 販売個数の多い順
SELECT
    product_id,
    COUNT(DISTINCT order_id) AS order_count,
    SUM(quantity) AS total_quantity
FROM
    order_details_trn
WHERE
    deleted_at IS NULL
GROUP BY
    product_id
ORDER BY
    total_quantity DESC;

-- (2) 注文件数の多い順
SELECT
    product_id,
    COUNT(DISTINCT order_id) AS order_count,
    SUM(quantity) AS total_quantity
FROM
    order_details_trn
WHERE
    deleted_at IS NULL
GROUP BY
    product_id
ORDER BY
    order_count DESC;

-- (3)
-- (1) の1位: product_id=15（国産はちみつ）… 合計502個だが、注文はたった2件
-- (2) の1位: product_id=1（ワイヤレスイヤホン）… 4件の注文で、合計4個
--
-- 「売れ筋」としてレポートするなら order_count（注文件数）を主に見るべき。
-- product_id=15 の502個は、order_id=10 の500個という1件の大口注文で作られた数字であり、
-- 「多くの顧客に繰り返し買われている」という意味での売れ筋ではない。
-- 合計個数は1件の外れ値で順位が簡単に入れ替わるため、
-- 実務では「注文件数」「購入した顧客数」「合計個数」を並べて見るのが基本。
```

### 期待結果:

(1) 販売個数の多い順（上位）
| product_id | order_count | total_quantity |
| ---: | ---: | ---: |
| 15 | 2 | **502** |
| 6 | 2 | 6 |
| 8 | 2 | 5 |
| 1 | 4 | 4 |
| 16 | 1 | 3 |
| 2 | 3 | 3 |
| 10 | 1 | 2 |
| 21 | 1 | 2 |

(2) 注文件数の多い順（上位）
| product_id | order_count | total_quantity |
| ---: | ---: | ---: |
| 1 | **4** | 4 |
| 2 | 3 | 3 |
| 6 | 2 | 6 |
| 8 | 2 | 5 |
| 15 | 2 | 502 |
| 3 | 1 | 1 |
| 4 | 1 | 1 |
| 5 | 1 | 1 |

### 💡 講師用の解説ポイント

**① 既存の問題7の「軸を変えただけ」**
問題7は同じ `order_details_trn` を `order_id` でグループ化して「注文ごとの商品点数」を出しました。今回は `product_id` でグループ化して「商品ごとの販売実績」を出しています。
**同じテーブル・同じ集計関数でも、`GROUP BY` の軸を変えると全く別の指標になる**ということが、この対比で一番よく伝わります。「何を1行にしたいのか（＝集計の粒度）」を先に決めるのが SQL を書く順序です。

**② 外れ値で順位が入れ替わる**
`order_id=10` の「国産はちみつ（`product_id=15`）500個」は、初期データで **在庫数（400個）を超える大量注文** として意図的に仕込まれているレコードです。これ1件があるだけで販売個数ランキングが完全に歪みます（合計502個のうち500個が1注文分）。
**どの指標を選ぶかで結論が変わる**という体験は、集計を業務で使うときの一番大事な感覚なので、ここは受講者同士で議論させる価値があります。

**③ この集計で出せないもの（08章への伏線）**
- **売上金額** ―― 単価は `products_mst` にあるので、結合しないと出せません
- **商品名** ―― 同様に `products_mst` が必要（`product_id` のままではレポートとして使えない）
- **購入した顧客数** ―― `customer_id` は `orders_trn` にあるので、結合が必要

「売れ筋レポートを完成させるには結合が要る」と締めると、そのまま08章に入れます。

**④ `COUNT(DISTINCT order_id)` と `COUNT(*)` について**
`order_details_trn` の主キーは `(order_id, product_id)` なので、同じ商品が同じ注文に2行入ることはありません。したがって `COUNT(*)` でも結果は同じです（問題7の補足と同じ論点）。
ただし **「注文件数を数えている」という意図を明示する** ために `COUNT(DISTINCT order_id)` と書いています。将来テーブル設計が変わって重複行を許すようになった場合にも壊れません。
