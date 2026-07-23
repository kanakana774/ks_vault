### **事前準備：テーブル作成**

まず、新しくDBを作り、以下のテーブルを作成してください。

```sql
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS products;

CREATE TABLE customers (
    customer_id INT PRIMARY KEY,
    name TEXT,
    email TEXT,
    city TEXT,
    join_date TIMESTAMP
);

CREATE TABLE products (
    product_id INT PRIMARY KEY,
    product_name TEXT,
    category_id INT,
    price INT
);

CREATE TABLE orders (
    order_id INT PRIMARY KEY,
    customer_id INT,
    order_date TIMESTAMP,
    status TEXT
);
```

---

### **問題 1: 単一列インデックスと Bitmap Scan**
- **目的**: インデックスの有無による違いと、物理配置がバラバラな際の `Bitmap Scan` を理解する。

#### **データ作成**
10万件のデータを、`city` が物理的にバラバラに並ぶように挿入します。
```sql
INSERT INTO customers (customer_id, name, email, city, join_date)
SELECT
    gs AS customer_id,
    'Customer_' || gs AS name,
    'user' || gs || '@example.com' AS email,
    'City_' || (gs % 500) AS city, -- 500種類の都市（1都市あたり200件 = 選択率0.2%）
    NOW() - (gs || ' minutes')::interval
FROM
    GENERATE_SERIES(1, 100000) AS gs
ORDER BY RANDOM(); -- 物理配置をバラバラにする

ANALYZE customers;
```

#### **演習**
1. `city = 'City_123'` を検索するクエリの実行計画を確認してください。
2. `city` 列にインデックスを作成し、再度確認してください。

```sql
-- 1. インデックスなし
EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM customers WHERE city = 'City_123';

-- 2. インデックス作成
CREATE INDEX idx_customers_city ON customers (city);
ANALYZE customers;

-- 3. インデックスあり
EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM customers WHERE city = 'City_123';
```

#### **解答**
```
なぜその実行計画が選ばれたのか考察してください
```

---

### **問題 2: 複合インデックスと左側プレフィックス**
- **目的**: 複合インデックスにおいて、列の順番が検索にどう影響するかを理解する。

#### **データ作成**
```sql
INSERT INTO products (product_id, product_name, category_id, price)
SELECT
    gs AS product_id,
    'Product_' || gs % 10000 AS product_name,
    (gs % 10) + 1 AS category_id,
    (RANDOM() * 10000)::int AS price
FROM
    GENERATE_SERIES(1, 100000) AS gs;

-- 複合インデックス作成（name, priceの順）
CREATE INDEX idx_products_name_price ON products (product_name, price);
ANALYZE products;
```

#### **演習**
以下の3つのクエリの実行計画を比較してください。
1. `WHERE product_name = 'Product_500' AND price > 5000`
2. `WHERE product_name = 'Product_500'`
3. `WHERE price > 5000`

```sql
EXPLAIN ANALYZE SELECT * FROM products WHERE product_name = 'Product_500' AND price > 5000;
EXPLAIN ANALYZE SELECT * FROM products WHERE product_name = 'Product_500';
EXPLAIN ANALYZE SELECT * FROM products WHERE price > 5000;
```

#### **解答**
```
なぜその実行計画が選ばれたのか考察してください
```


---

### **問題 3: Index Only Scan と Visibility Map**
- **目的**: テーブル本体を見に行かない最強の高速化と、その条件を理解する。

#### **データ作成**
```sql
INSERT INTO orders (order_id, customer_id, order_date, status)
SELECT
    gs AS order_id,
    (gs % 10000) + 1 AS customer_id,
    NOW() - (gs || ' seconds')::interval AS order_date,
    'COMPLETED' AS status
FROM
    GENERATE_SERIES(1, 200000) AS gs;

-- カバリングインデックス
CREATE INDEX idx_orders_customer_date ON orders (customer_id) INCLUDE (order_date);

-- 重要：VACUUMを実行してVisibility Mapを更新する
VACUUM ANALYZE orders;
```

#### **演習**
1. `customer_id = 500` の `order_date` だけを取得するクエリの計画を確認してください。
2. `SELECT *` に変えると計画はどう変わりますか？

```sql
-- 1. Index Only Scan
EXPLAIN (ANALYZE, BUFFERS) SELECT customer_id, order_date FROM orders WHERE customer_id = 500;

-- 2. Index Scan / Bitmap Scan
EXPLAIN (ANALYZE, BUFFERS) SELECT * FROM orders WHERE customer_id = 500;
```

#### **解答**
```
なぜその実行計画が選ばれたのか考察してください
```


---

### **問題 4: 関数インデックス**
- **目的**: 検索条件に演算や関数を使う場合の注意点を理解する。

#### **演習**
1. 以下のクエリを実行し、`Seq Scan` になることを確認してください。
2. インデックスを作成して `Index Scan` に変えてください。

```sql
-- 1. インデックスはあるが効かない例（既存の idx_products_name_price は Product_... という大文字始まり）
EXPLAIN ANALYZE SELECT * FROM products WHERE LOWER(product_name) = 'product_500';

-- 2. 関数インデックスの作成
CREATE INDEX idx_products_name_lower ON products (LOWER(product_name));
ANALYZE products;

-- 3. 再確認
EXPLAIN ANALYZE SELECT * FROM products WHERE LOWER(product_name) = 'product_500';
```

#### **解答**
```
なぜその実行計画が選ばれたのか考察してください
```


---

### **問題 5: Nested Loop Join の挙動**
- **目的**: 結合において、インデックスがどう利用されるかを確認する。

#### **演習**
以下の結合クエリを実行し、内部でどのような処理が行われているか考察してください。

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT
    c.name,
    o.order_date
FROM
    customers c
JOIN
    orders o ON c.customer_id = o.customer_id
WHERE
    c.customer_id = 1234;
```

#### **解答**
```
なぜその実行計画が選ばれたのか考察してください
```
