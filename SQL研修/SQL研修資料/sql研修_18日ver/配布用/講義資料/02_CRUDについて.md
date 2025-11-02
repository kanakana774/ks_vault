# **SQL 基礎：データ操作（CRUD）とエイリアスの活用**

## **1. 導入：CRUD とは**

SQLにおける**CRUD**とは、データベースを操作するための基本的な4つの機能の頭文字をとった言葉です。

*   **C**reate（作成）: データの新規追加 (`INSERT`)
*   **R**ead（読み取り）: データの検索・取得 (`SELECT`)
*   **U**pdate（更新）: 既存データの変更 (`UPDATE`)
*   **D**elete（削除）: データの削除 (`DELETE`)

これらの操作は、あらゆるデータベースアプリケーションの根幹をなします。

---

## **2. データの作成 (Create)：`INSERT`**

テーブルに新しい行（レコード）を追加します。

### **基本構文**

#### **構文1：全ての列に値を指定**
テーブル定義の列順に合わせて値を指定します。

```sql
INSERT INTO テーブル名 VALUES (値1, 値2, ...);
```

#### **構文2：特定の列に値を指定（推奨）**
列名を指定するため、順序を気にする必要がなく、コードが読みやすくなります。指定されなかった列にはデフォルト値や`NULL`が設定されます。

```sql
INSERT INTO テーブル名 (列名1, 列名2, ...) VALUES (値1, 値2, ...);
```

### **使用例**

`products`テーブルを例に説明します。

```sql
CREATE TABLE products (
    product_id SERIAL PRIMARY KEY,       -- 商品ID (自動採番)
    product_name VARCHAR(255) NOT NULL,  -- 商品名
    price NUMERIC(10, 2) DEFAULT 0.00,   -- 価格
    stock_quantity INTEGER DEFAULT 0     -- 在庫数
);
```

**例1：特定の列に値を指定**
`product_id`は自動採番されるため、指定する必要はありません。

```sql
INSERT INTO products (product_name, price, stock_quantity)
VALUES ('Laptop', 1200.00, 50);
```

**例2：一部の列のみ値を指定**
`stock_quantity`を省略すると、`DEFAULT`で設定された `0` が自動的に格納されます。

```sql
INSERT INTO products (product_name, price)
VALUES ('Mouse', 25.50);
```

---

## **3. データの読み取り (Read)：`SELECT`**

データベースからデータを取得する最も重要なコマンドです。

### **基本構文**

```sql
SELECT 列名1, 列名2, ...
FROM テーブル名
[AS テーブルのエイリアス]
[WHERE 条件式]
[ORDER BY 列名 [ASC|DESC]]
[LIMIT 件数 [OFFSET 開始位置]];
```

### **3.1. 取得する列の指定**

**1. 特定の列を指定**
必要な列名をカンマ区切りで指定します。

```sql
SELECT product_name, price FROM products;
```

**2. 全ての列を指定 (`*`)**
アスタリスク(`*`)を使うと全ての列を取得できます。ただし、意図しないデータ取得やパフォーマンス低下を招く可能性があるため、開発時や一時的な確認以外では、必要な列を明示的に指定することが推奨されます。

```sql
SELECT * FROM products;
```

### **3.2. エイリアス（別名）の使用：`AS`**

`AS`キーワードを使うことで、列名やテーブル名に別名（エイリアス）を付けることができます。これにより、クエリの結果が分かりやすくなったり、クエリ自体が書きやすくなります。`AS`は省略可能です。

**1. 列のエイリアス**
取得結果の列名を、分かりやすい日本語などに変更できます。計算結果に名前を付ける際にも便利です。

```sql
-- product_nameを「商品名」、priceを「価格」という別名で取得
SELECT
  product_name AS "商品名",
  price AS "価格"
FROM products;
```

**2. テーブルのエイリアス**
テーブル名が長い場合や、複数のテーブルを結合する（JOIN）際に、クエリを簡潔に記述するために使用します。

```sql
-- productsテーブルに p というエイリアスを付けて列を指定
SELECT p.product_name, p.price
FROM products AS p
WHERE p.price > 1000;
```

### **3.3. 条件による絞り込み：`WHERE`句**

特定の条件に合致する行だけを取得します。

```sql
-- 価格が1000より大きい商品を取得
SELECT product_name, price
FROM products
WHERE price > 1000;
```

### **3.4. 結果の並び替え：`ORDER BY`句**

取得した結果を指定した列の値で並び替えます。

*   **`ASC`**: 昇順（小さい順、デフォルト）
*   **`DESC`**: 降順（大きい順）

```sql
-- 価格が高い順に並び替え
SELECT product_name, price
FROM products
ORDER BY price DESC;
```
複数の列をキーとして並び替えることも可能です。
```sql
-- 在庫数が多い順、同じ在庫数なら価格が安い順に並び替え
SELECT product_name, price, stock_quantity
FROM products
ORDER BY stock_quantity DESC, price ASC;
```

### **3.5. 取得件数の制限：`LIMIT`句 / `OFFSET`句**

取得する行数を制限したり、開始位置を指定したりします。Webアプリケーションのページネーション機能などでよく利用されます。

```sql
-- 価格が高い商品トップ3を取得
SELECT product_name, price
FROM products
ORDER BY price DESC
LIMIT 3;

-- 4番目から2件の商品を取得 (2ページ目のようなイメージ)
SELECT product_name, price
FROM products
ORDER BY product_name ASC
LIMIT 2 OFFSET 3;
```
※ `TOP`句（SQL Server）や`ROWNUM`（Oracle）など、データベースシステムによって構文が異なる場合があります。

---

## **4. データの更新 (Update)：`UPDATE`**

既存のテーブルデータを変更します。

### **基本構文**
```sql
UPDATE テーブル名
SET 列名1 = 新しい値1, 列名2 = 新しい値2, ...
[WHERE 条件式];
```

### **使用例**

**例1：特定の行の価格を更新**
```sql
UPDATE products
SET price = 1100.00
WHERE product_name = 'Laptop';
```
**例2：既存の値を元に計算して更新**
```sql
-- Mouseの価格を10%引き、在庫を1つ減らす
UPDATE products
SET price = price * 0.9, stock_quantity = stock_quantity - 1
WHERE product_name = 'Mouse';
```

> **【最重要】`WHERE`句の指定を忘れずに！**
> `WHERE`句を省略すると、**テーブルの全ての行が更新されてしまいます**。意図しないデータ更新を防ぐため、`UPDATE`文を実行する前には必ず`WHERE`句の条件を確認してください。

---

## **5. データの削除 (Delete)：`DELETE`**

テーブルから行を削除します。

### **基本構文**
```sql
DELETE FROM テーブル名
[WHERE 条件式];
```

### **使用例**
在庫が0の商品を削除します。

```sql
DELETE FROM products
WHERE stock_quantity = 0;
```

> **【最重要】`WHERE`句の指定を忘れずに！**
> `UPDATE`と同様に、`WHERE`句を省略すると**テーブルの全てのデータが削除されてしまいます**。データ削除は元に戻せない非常に危険な操作のため、条件指定には細心の注意を払ってください。