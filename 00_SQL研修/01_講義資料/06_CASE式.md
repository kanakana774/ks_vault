
## CASE式による条件分岐

### 1. CASE式とは？

CASE式は、SQLクエリ内で条件に応じて処理を分岐させるための構文です。多くのプログラミング言語における `if-then-else` や `switch` 文のように、データの変換、分類、集計、並び替えなど、様々な場面で活用できる強力な機能です。

**主な用途:**
*   **`SELECT`句**: 条件に基づいて値を変換し、新しい列を作成する。
*   **`ORDER BY`句**: 独自のルールでデータの並び順を制御する。
*   **集計関数との組み合わせ**: 条件に合うデータだけを集計する（クロス集計）。
*   **`WHERE`句**: 条件によってフィルタリングのロジックを動的に変更する。


### 2. CASE式の基本構文

CASE式には「シンプルCASE式」と「検索CASE式」の2つの書き方があります。

#### シンプルCASE式
特定の**1つの列の値**が何かによって処理を分岐させる場合に使用します。
```sql
CASE 列名
  WHEN 値1 THEN 結果1
  WHEN 値2 THEN 結果2
  ...
  [ELSE デフォルト結果]
END
```

#### 検索CASE式
より複雑な**条件式**（比較演算子や論理演算子など）を用いて分岐させる、より柔軟な形式です。
```sql
CASE
  WHEN 条件式1 THEN 結果1
  WHEN 条件式2 THEN 結果2
  ...
  [ELSE デフォルト結果]
END
```

**重要なポイント:**
1.  **評価順序**: `WHEN`句は上から順に評価され、最初に真 (TRUE) となった結果が返されます。**記述順序が非常に重要です。**
2.  **ELSE句**: `ELSE`を省略し、どの条件にも合致しない場合、結果は `NULL` となります。
3.  **型の一致**: `THEN` や `ELSE` で返す値のデータ型はすべて一致（または暗黙的に変換可能）している必要があります。
    *    `THEN 100` (数値) と `ELSE '未設定'` (文字列) を混ぜることはできません。


---

### 3. 利用シーン別の活用例

#### A. `SELECT`句での利用

`SELECT`句でCASE式を使うと、既存のデータから新しい意味を持つ列を動的に作成できます。

##### ① データの変換と分類
役職（`position`）に基づいて、新しい「役割（`role`）」列を作成します。

```sql
WITH employees AS (
  SELECT '山田 太郎' AS name, '部長' AS position UNION ALL
  SELECT '鈴木 花子', '課長' UNION ALL
  SELECT '佐藤 次郎', 'リーダー' UNION ALL
  SELECT '田中 三郎', '一般' UNION ALL
  SELECT '高橋 四郎', NULL
)
SELECT
  name,
  position,
  CASE position
    WHEN '部長' THEN '経営層'
    WHEN '課長' THEN '管理職'
    WHEN 'リーダー' THEN '管理職'
    ELSE 'メンバー'
  END AS role
FROM
  employees;
```
*`position`が`NULL`の場合、どの`WHEN`にも合致しないため`ELSE`の「メンバー」が適用されます。*

##### ② 単一行関数との組み合わせ
CASE式の中で関数（`LENGTH`, `LEFT`, `ROUND`など）を使い、データを柔軟に整形します。

```sql
WITH products AS (
  SELECT 'プレミアムノートパソコン 2024モデル' AS name, 1550.75 AS price UNION ALL
  SELECT 'マウス', 25.40 UNION ALL
  SELECT NULL, 10.00
)
SELECT
  CASE 
    WHEN name IS NULL THEN '（名称未設定）'
    WHEN LENGTH(name) > 10 THEN LEFT(name, 10) || '...'
    ELSE name
  END AS display_name,
  CASE 
    WHEN price >= 1000 THEN FLOOR(price) -- 高額商品は切り捨て
    ELSE ROUND(price, 1)                -- 安価なものは小数第1位まで
  END AS adjusted_price
FROM
  products;
```

##### ③ 関数の引数としてCASE式を使う
関数の「中身」にCASE式を書くことで、計算ルール自体を動的に変更できます。

```sql
WITH sales_data AS (
  SELECT 'おにぎり' AS item, '食品' AS category, 120 AS price UNION ALL
  SELECT 'ビール' AS item, '酒類' AS category, 250 AS price
)
SELECT
  item,
  ROUND(
    CASE
      WHEN category = '食品' THEN price * 1.08 -- 軽減税率
      ELSE price * 1.10                       -- 標準税率
    END,
    0
  ) AS tax_included_price
FROM
  sales_data;
```
*`ROUND`関数を一箇所書くだけで済むため、後からの修正（例：切り捨てに変更）が容易になります。*

---

#### B. `ORDER BY`句での利用：並び順のカスタマイズ

独自の優先順位に基づいてデータをソートします。

**例：役職順（部長＞課長＞リーダー＞一般）で並べる**
```sql
WITH employees AS (
  SELECT '山田 太郎' AS name, '部長' AS position, '2023-01-15'::date AS hire_date UNION ALL
  SELECT '鈴木 花子', '課長', '2023-03-15'::date AS hire_date UNION ALL
  SELECT '佐藤 次郎', 'リーダー', '2023-05-15'::date AS hire_date UNION ALL
  SELECT '田中 三郎', '一般', '2023-06-15'::date AS hire_date UNION ALL
  SELECT '高橋 四郎', NULL, '2023-07-15'::date AS hire_date
)
SELECT name, position
FROM employees
ORDER BY
  CASE position
    WHEN '部長'   THEN 1
    WHEN '課長'   THEN 2
    WHEN 'リーダー' THEN 3
    WHEN '一般'   THEN 4
    ELSE 5
  END,
  hire_date; -- 同じ役職内では入社日順
```

**補足：ソートの仕組み**
`ORDER BY`はCASE式が返した「値」を比較します。数値を返せば数値順、文字列を返せば辞書順で並びます。
```sql
-- 文字列を返してソートする例（辞書順で制御）
ORDER BY
  CASE position
    WHEN '部長' THEN 'A'
    WHEN '課長' THEN 'B'
    ELSE 'Z'
  END;
```

---

#### C. Group Byや集計関数との組み合わせ
##### ① クロス集計（集計関数内での使用）
「行」のデータを「列」に変換して比較しやすくします。
```SQL
WITH sales AS (
  SELECT '2023-01-10'::date AS sale_date, '食品' AS category, 1000 AS amount UNION ALL
  SELECT '2023-01-15'::date, '書籍', 2000 UNION ALL
  SELECT '2023-01-20'::date, '食品', 1500 UNION ALL
  SELECT '2023-02-05'::date, '家電', 50000 UNION ALL
  SELECT '2023-02-12'::date, '書籍', 3000 UNION ALL
  SELECT '2023-02-18'::date, '食品', 1200
)
SELECT
	TO_CHAR(sale_date, 'YYYY-MM') AS sale_month,
	SUM(
		CASE
			WHEN category = '食品' THEN amount
		END
	) AS food_amount,
	SUM(
		CASE
			WHEN category = '書籍' THEN amount
		END
	) AS book_amount,
	SUM(
		CASE
			WHEN category = '家電' THEN amount
		END
	) AS appliances_amount
FROM
	sales
GROUP BY
	TO_CHAR(sale_date, 'YYYY-MM')
```

**PostgreSQL限定：`FILTER`句**
PostgreSQLでは、より簡潔に記述できる専用構文があります。
```sql
-- (WITH句は上記と同じ)
SELECT
  TO_CHAR(sale_date, 'YYYY-MM') AS sale_month,
  SUM(amount) FILTER (WHERE category = '食品') AS food_sales,
  SUM(amount) FILTER (WHERE category = '書籍') AS book_sales,
  SUM(amount) FILTER (WHERE category = '家電') AS electronics_sales
FROM
  sales
GROUP BY
  sale_month
ORDER BY
  sale_month;
```

##### ② group byでの使用
都道府県ごとの人口を地方別の人口に集計しなおす例です。
```sql
WITH population AS (
  SELECT '東京' AS pref, 14000000 AS population UNION ALL
  SELECT '埼玉', 7000000 UNION ALL
  SELECT '徳島', 700000 UNION ALL
  SELECT '愛媛', 1000000
)

SELECT
	CASE
		WHEN pref = '愛媛' THEN '四国'
		WHEN pref = '徳島' THEN '四国'
		WHEN pref = '東京' THEN '関東'
		WHEN pref = '埼玉' THEN '関東'
	END as 地方, 
	sum(population)
FROM
	population
GROUP BY
	CASE
		WHEN pref = '愛媛' THEN '四国'
		WHEN pref = '徳島' THEN '四国'
		WHEN pref = '東京' THEN '関東'
		WHEN pref = '埼玉' THEN '関東'
	END
```


---

#### D. `WHERE`句での利用：動的なフィルタリング (⚠️注意点あり)

条件によってフィルタリングを切り替えますが、**パフォーマンス面から推奨されません。**

**例：会員ランクによる表示制御（非推奨）**
```sql
WITH customer_purchases AS (
  SELECT 'ゴールド会員' AS rank, '商品A' AS item, 5000 AS amount UNION ALL
  SELECT 'ゴールド会員', '商品B', 1000 UNION ALL
  SELECT 'シルバー会員', '商品C', 4000 UNION ALL
  SELECT 'シルバー会員', '商品D', 2000
)
SELECT
  rank, item, amount
FROM
  customer_purchases
WHERE
  CASE
    WHEN rank = 'ゴールド会員' THEN TRUE -- ゴールド会員は常に表示
    WHEN rank = 'シルバー会員' THEN amount >= 3000 -- シルバー会員は金額でフィルタ
    ELSE FALSE
  END;
```

**推奨される書き方（AND/ORを利用）**
インデックスが効きやすく、可読性も高いです。
```sql
-- (WITH句は上記と同じ)
SELECT
  rank, item, amount
FROM
  customer_purchases
WHERE
  (rank = 'ゴールド会員')
  OR
  (rank = 'シルバー会員' AND amount >= 3000);
```

**アンチパターン：複雑なネスト**
CASE式を重ねると、どこまでが一つの条件か分からなくなります。論理演算子（AND/OR）で条件ブロックを明確に分けるのがクリーンなSQLのコツです。

**条件式が複雑になった例**
下記どちらも同じ条件ですが、読みにくいですよね。。。

case式を使った書き方：
```SQL
-- どこからどこまでが一つの条件か、非常に分かりにくい
WHERE
  CASE
    WHEN stock_quantity >= 100 THEN ( -- 在庫条件
      CASE
        WHEN price > 500 THEN ( -- 価格条件
          CASE
            WHEN category = '家電' THEN TRUE -- カテゴリ条件
            ELSE FALSE
          END
        )
        ELSE FALSE
      END
    )
    ELSE TRUE -- それ以外はすべて表示
  END;
```
普通の書き方：
```SQL
-- 条件ブロックが明確で、追加や修正も容易
WHERE
  (stock_quantity >= 100 AND price > 500 AND category = '家電')
  OR
  (stock_quantity < 100);
```

---
#### E. update文での使用
updateでもよく使用します。
例：
```SQL
UPDATE orders
SET status_name = CASE 
    WHEN status_code = 1 THEN '注文済み'
    WHEN status_code = 2 THEN '発送準備中'
    WHEN status_code = 3 THEN '発送済み'
    ELSE '不明'
END;
```
例：
```SQL
UPDATE products
SET price = CASE 
    WHEN category = '食品' THEN price * 0.9  -- 10%引き
    WHEN category = '家電' THEN price * 0.8  -- 20%引き
    ELSE price                               -- それ以外は据え置き
END;
```

値の入れ替えなんかにも便利です。
例：
```SQL
UPDATE categories
SET sort_order = CASE 
    WHEN id = 1 THEN 2
    WHEN id = 2 THEN 1
    ELSE sort_order
END
WHERE id IN (1, 2);
```