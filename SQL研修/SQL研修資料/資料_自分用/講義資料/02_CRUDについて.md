# **SQL 基礎：基本操作（CRUD）と演算子**

## **導入：CRUD とは**

SQL における**CRUD**とは、データベースの情報を操作する基本的な 4 つの機能の頭文字を取ったものです。

- **C**reate (作成): データの新規追加 (INSERT)
- **R**ead (読み取り): データの検索・取得 (SELECT)
- **U**pdate (更新): 既存データの変更 (UPDATE)
- **D**elete (削除): データの削除 (DELETE)

これらの操作は、あらゆるデータベースアプリケーションの根幹をなすものです。

## **データの追加：INSERT**

テーブルに新しい行（レコード）を追加する際に使用します。

### **基本構文**

#### **1. 全ての列に値を指定する場合**

列名を省略できますが、テーブル定義時の列順に値を指定する必要があります。

```SQL
INSERT INTO テーブル名 VALUES (値 1, 値 2, 値 3, ...);
```

#### **2. 特定の列に値を指定する場合**

推奨される記法です。値を指定しない列には、DEFAULT 値が設定されるか、NULL が許可されていれば NULL が格納されます。

```SQL
INSERT INTO テーブル名 (列名 1, 列名 2, ...) VALUES (値 1, 値 2, ...);
```

### **例**

products テーブルを以下のように作成していると仮定します。

```SQL
CREATE TABLE products (
 product_id SERIAL PRIMARY KEY,
 product_name VARCHAR(255) NOT NULL,
 price NUMERIC(10, 2) DEFAULT 0.00,
 stock_quantity INTEGER DEFAULT 0
);
```

#### **例 1: 全ての列に値を指定（product_id は SERIAL なので省略可能）**

```SQL
INSERT INTO products (product_name, price, stock_quantity)
VALUES ('Laptop', 1200.00, 50);
```

※product_id は SERIAL 型なので自動採番され、INSERT 文で値を指定する必要はありません。

#### **例 2: 特定の列にのみ値を指定**

stock_quantity には DEFAULT 値の 0 が設定されます。

```SQL
INSERT INTO products (product_name, price)
VALUES ('Mouse', 25.50);
```

## **データの検索：SELECT**

データベースからデータを取得する際に使用する最も重要なコマンドです。取得する列、条件、並び順、取得件数などを指定できます。

### **基本構文**

```SQL
SELECT 列名 1, 列名 2, ...
FROM テーブル名
[WHERE 条件式]
[ORDER BY 列名 [ASC|DESC] [, ...]]
[LIMIT 件数 [OFFSET 開始位置]];
```

### **列の指定**

#### **1. 特定の列を指定**

取得したい列名をカンマ区切りで指定します。

```SQL
SELECT product_name, price
FROM products;
```

#### **2. 全ての列を指定 (\*)**

テーブルの全ての列を取得します。手軽ですが、実際に必要な列だけを指定する方がパフォーマンス面で推奨されます。

```SQL
SELECT *
FROM products;
```

### **条件による絞り込み：WHERE 句**

特定の条件に合致する行のみを取得します。ここで様々な演算子が活用されます。

```SQL
SELECT product_name, price
FROM products
WHERE price > 1000; -- 価格が 1000 より大きい商品
```

### **結果の並び替え：ORDER BY 句**

取得した結果を 1 つ以上の列の値に基づいて並び替えます。

- **ASC (Ascending)**: 昇順（小さい方から大きい方へ）。デフォルト。
- **DESC (Descending)**: 降順（大きい方から小さい方へ）。

```SQL
SELECT product_name, price
FROM products
ORDER BY price DESC; -- 価格が高い順に並び替え
```

複数列で並び替えることも可能です。

```SQL
SELECT product_name, price, stock_quantity
FROM products
ORDER BY stock_quantity DESC, price ASC; -- 在庫数が多い順、同じ在庫数なら価格が安い順
```

### **取得件数の制限：LIMIT 句 / OFFSET 句**

取得する行の数を制限したり、特定の位置からデータを取得したりします。

- **LIMIT 件数**: 指定した件数分だけ行を取得します。
- **OFFSET 開始位置**: 指定した開始位置（0 から始まる）から行を取得します。ページネーションなどで利用されます。

```SQL
-- 価格が高い商品トップ 3 を取得
SELECT product_name, price
FROM products
ORDER BY price DESC
LIMIT 3;
```

```SQL
-- 4 番目から 2 件の商品を取得 (ページネーションの 2 ページ目などに利用)
SELECT product_name, price
FROM products
ORDER BY product_name ASC
LIMIT 2 OFFSET 3;
```

※RDBMS によっては、LIMIT OFFSET の代わりに TOP 句（SQL Server）や ROWNUM（Oracle）など、異なる構文を使用する場合があります。

## **データの更新：UPDATE**

既存のテーブルのデータを変更する際に使用します。

### **基本構文**

```SQL
UPDATE テーブル名
SET 列名 1 = 新しい値 1, 列名 2 = 新しい値 2, ...
[WHERE 条件式];
```

### **例**

#### **例 1: 特定の条件に合致する行を更新**

product_name が'Laptop'の商品価格を 1100.00 に更新します。

```SQL
UPDATE products
SET price = 1100.00
WHERE product_name = 'Laptop';
```

#### **例 2: 複数の列を同時に更新**

```SQL
UPDATE products
SET price = price * 0.9, stock_quantity = stock_quantity - 1
WHERE product_name = 'Mouse';
```

price = price \* 0.9 のように、既存の値を元に計算して更新することも可能です。

#### **注意！`WHERE`句を忘れると…**

`WHERE`句を指定しない場合、**テーブルの全ての行が更新されてしまいます**。本番環境での実行は極めて危険ですので、`UPDATE`文を書く際は必ず`WHERE`句の指定を意識しましょう。

## データの削除：`DELETE`

テーブルから行を削除する際に使用します。

### 基本構文

```sql
DELETE FROM テーブル名
[WHERE 条件式];
```

### **例**

#### **例 1: 特定の条件に合致する行を削除**

stock_quantity が 0 の商品を削除します。

```SQL
DELETE FROM products
WHERE stock_quantity = 0;
```

#### **注意！WHERE 句を忘れると…**

UPDATE と同様に、WHERE 句を指定しない場合、**テーブルの全ての行が削除されてしまいます**。これも本番環境では非常に危険な操作です。
