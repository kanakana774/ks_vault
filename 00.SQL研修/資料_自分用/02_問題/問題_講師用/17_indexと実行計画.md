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

#### **解答解説**
- **インデックスなし**: `Seq Scan`。全件（10万行）を走査します。
- **インデックスあり**: `Bitmap Heap Scan` が選ばれるはずです。
    - **なぜ Index Scan ではないのか？**: 挿入時に `ORDER BY RANDOM()` をしたため、`City_123` のデータがテーブル内のあちこちのページに分散しています。
    - `Index Scan` だと「インデックスを見てテーブルへ飛ぶ」を200回繰り返す（ランダムアクセス多発）ため、PostgreSQLは「先にインデックスで場所を全部リストアップして、ページ番号順に並べ替えてからまとめて読みに行く」 `Bitmap Scan` を選択します。

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

#### **解答解説**
- **1と2**: `Index Scan` が使われます。インデックスの第一キーである `product_name` が条件に含まれているためです。
- **3**: `Seq Scan` になります。複合インデックスは「名前で並んでいて、同じ名前の中だけで価格順に並んでいる」ため、名前が不明だと価格の並びを利用できないからです（電話帳で「苗字」がわからず「名前」だけで探すのと同じ）。

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

#### **解答解説**
- **クエリ1**: `Index Only Scan`。必要なデータが全てインデックス内にあるため、テーブル本体（Heap）へのアクセスが不要になります。`Buffers: shared hit` の数が劇的に少ないことを確認してください。
- **クエリ2**: `Index Scan`。`*`を指定しており、インデックスにない `status` や `order_id` を取得するために、結局テーブル本体を見に行く必要があるため、Only Scanにはなりません。

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

#### **解答解説**
- インデックスは「加工前の値」で並んでいます。`LOWER()` 関数を通すと値が変わってしまうため、通常のインデックスは使えません。
- `LOWER(列名)` そのものをインデックス化することで、計算済みの値がインデックスに格納され、高速検索が可能になります。

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

#### **解答解説**
- **Nested Loop**: 
    1. まず `customers` から `customer_id = 1234` の1行を見つけます（Index Scan）。
    2. その `customer_id` を使って、`orders` テーブルのインデックス `idx_orders_customer_date` を検索します。
- **ポイント**: 外部テーブル（customers）が十分に絞り込まれており、内部テーブル（orders）の結合キーにインデックスがある場合、この方式が最も効率的になります。もし両方のテーブルが巨大で絞り込みがない場合は、PostgreSQLは `Hash Join` などに切り替えます。
	- 試しに、プローブ側の結合列にindexがない場合を考えてみる。
		- `Hash Join`の場合（n+m行読み込み）
			- ビルド側n行として、n行を全件読み込みし、hash関数にかけ、hashテーブルをwork_memに作成する。
			- その後、プローブ側をm行として、m行を全件読み込みつつ、各行に対し、hash関数を適用し、ビルド側と結合できる行があれば結合する。
		- `Nested Loop`の場合（最大n×m行読み込み）
			- ビルド側n行として、1行ごとにプローブ側m行を読み込み、結合条件を比較する。
			- このため、結合条件の片側にindexが貼られていない場合は、`Nested Loop`が選ばれにくい。