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
