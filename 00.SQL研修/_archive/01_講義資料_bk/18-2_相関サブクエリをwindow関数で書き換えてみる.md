相関サブクエリの3パターンをwindow関数で書き換えてみる。
## 相関サブクエリの思考パターン

### 思考パターン1: 「存在チェック」を「絞り込み条件」に使う (`WHERE` + `EXISTS`)

**【例】少なくとも1つの注文がある顧客を取得**

**事前データ (customers, orders):**
`customers`

| customer_id | customer_name |
| :--- | :--- |
| 1 | Alice |
| 2 | Bob |
| 3 | Charlie |

`orders`

| order_id | customer_id |
| :--- | :--- |
| 101 | 1 |
| 102 | 3 |

```sql
WITH customers AS (
  SELECT 1 AS customer_id, 'Alice' AS customer_name UNION ALL
  SELECT 2, 'Bob' UNION ALL
  SELECT 3, 'Charlie'
),
orders AS (
  SELECT 101 AS order_id, 1 AS customer_id UNION ALL
  SELECT 102, 3
)
SELECT customer_name
FROM customers AS c
WHERE EXISTS (
   SELECT 1
   FROM orders AS o
   WHERE o.customer_id = c.customer_id
);
```

**実行結果:**

| customer_name |
| :--- |
| Alice |
| Charlie |

**window関数での書き換え:**
```SQL
WITH customers AS (
  SELECT 1 AS customer_id, 'Alice' AS customer_name UNION ALL
  SELECT 2, 'Bob' UNION ALL
  SELECT 3, 'Charlie'
),
orders AS (
  SELECT 101 AS order_id, 1 AS customer_id UNION ALL
  SELECT 102, 3
)
SELECT DISTINCT customer_name
FROM (
  SELECT 
    c.customer_name,
    COUNT(o.order_id) OVER(PARTITION BY c.customer_id) as order_count
  FROM customers AS c
  LEFT JOIN orders AS o ON c.customer_id = o.customer_id
) AS t
WHERE order_count > 0;
```

⇒これはあまり意味がないですね。。。内部でjoinしてるので、joinで解決したほうがシンプルです。逆に複雑になってしまうので、existsのままがいいです。
パフォーマンス面でもexistsはsemi join（全結合しない）ので優れてます。

---

### 思考パターン2: 「取得した値との比較」を「絞り込み条件」に使う (`WHERE` + 比較演算子)

**【例】各カテゴリで最も価格が高い商品を取得**

**事前データ (products):**

| product_name | price | category_id |
| :--- | ---: | ---: |
| Laptop | 1200.00 | 1 |
| Mouse | 25.50 | 1 |
| Headset | 150.00 | 2 |
| Webcam | 90.00 | 2 |

```sql
WITH products AS (
  SELECT 'Laptop' AS product_name, 1200.00 AS price, 1 AS category_id UNION ALL
  SELECT 'Mouse', 25.50, 1 UNION ALL
  SELECT 'Headset', 150.00, 2 UNION ALL
  SELECT 'Webcam', 90.00, 2
)
SELECT
   p1.product_name,
   p1.price,
   p1.category_id
FROM
   products AS p1
WHERE
   p1.price = (
      SELECT MAX(p2.price)
      FROM products AS p2
      WHERE p2.category_id = p1.category_id
   );
```

**実行結果:**

| product_name | price | category_id |
| :--- | ---: | ---: |
| Laptop | 1200.00 | 1 |
| Headset | 150.00 | 2 |

**window関数での書き換え:**
```SQL
WITH products AS (
  SELECT 'Laptop' AS product_name, 1200.00 AS price, 1 AS category_id UNION ALL
  SELECT 'Mouse', 25.50, 1 UNION ALL
  SELECT 'Headset', 150.00, 2 UNION ALL
  SELECT 'Webcam', 90.00, 2
)

SELECT
	sub.product_name,
	sub.price,
	sub.category_id
FROM
	(
		SELECT
			*,
			RANK() OVER (
				PARTITION BY
					category_id
				ORDER BY
					price DESC
			) AS rank_per_category
		FROM
			products
	) AS sub
WHERE
	sub.rank_per_category = 1
```

⇒結局、カテゴリごとに分割して集計という手順を行っていてる。こういった**Top-N分析**がwindow関数では得意。
**Top-N分析**：上位N個（例：トップ10）の項目を抽出するデータ分析

---



### 思考パターン3: 「取得した値」を「新しい列」として使う (`SELECT` + スカラーサブクエリ)

※スカラーサブクエリとは、1つの値（スカラ値）を返すサブクエリのことです。

**【例】各商品に、その商品が属するカテゴリの平均価格を併記する**

**事前データ (products):**

| product_name | price | category_id |
| :--- | ---: | ---: |
| Laptop | 1200.00 | 1 |
| Mouse | 100.00 | 1 |
| Headset | 150.00 | 2 |

```sql
WITH products AS (
  SELECT 'Laptop' AS product_name, 1200.00 AS price, 1 AS category_id UNION ALL
  SELECT 'Mouse', 100.00, 1 UNION ALL
  SELECT 'Headset', 150.00, 2
)
SELECT
   p.product_name,
   p.price,
   (
      SELECT AVG(price)
      FROM products
      WHERE category_id = p.category_id
   ) AS avg_category_price
FROM
   products AS p;
```

**実行結果:**

| product_name | price | avg_category_price |
| :--- | ---: | ---: |
| Laptop | 1200.00 | 650.00 |
| Mouse | 100.00 | 650.00 |
| Headset | 150.00 | 150.00 |

**window関数での書き換え:**

```SQL
WITH products AS (
  SELECT 'Laptop' AS product_name, 1200.00 AS price, 1 AS category_id UNION ALL
  SELECT 'Mouse', 100.00, 1 UNION ALL
  SELECT 'Headset', 150.00, 2
)

SELECT
	product_name,
	price,
	ROUND(
		AVG(price) OVER (
			PARTITION BY
				category_id
		)
	) AS avgprice_per_category
FROM
	products;
```

⇒こちらも、window関数のほうがよりモダンな書き方




