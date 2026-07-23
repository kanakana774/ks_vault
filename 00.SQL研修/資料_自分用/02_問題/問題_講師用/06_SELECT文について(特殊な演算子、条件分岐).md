## 問題 1: 特定の範囲内の価格を持つ商品を検索する
- **目的**: `WHERE` 句と `BETWEEN` 演算子を使用して、数値範囲内のデータを効率的に絞り込む方法を理解する。

### 問題:
`products_mst` テーブルから、価格が **5,000 円以上 10,000 円以下** の商品の情報を取得してください。

### 解答:
```sql
SELECT *
FROM products_mst
WHERE price BETWEEN 5000 AND 10000;
```

**別解 (AND 演算子を使用)**
```sql
SELECT *
FROM products_mst
WHERE price >= 5000 AND price <= 10000;
```

---

## 問題 2: 複数のカテゴリの商品を検索する
- **目的**: `WHERE` 句と `IN` 演算子を使用して、指定した複数の値の「いずれか」に一致するデータを絞り込む方法を理解する。

### 問題:
`products_mst` テーブルから、カテゴリが **'Books'** または **'Food'** の商品の情報を取得してください。

### 解答:
```sql
SELECT *
FROM products_mst
WHERE category IN ('Books', 'Food');
```

**別解 (OR 演算子を使用)**
```sql
SELECT *
FROM products_mst
WHERE category = 'Books' OR category = 'Food';
```
> **解説**: 項目が増える場合、`OR` を連ねるよりも `IN` を使う方がSQLがスッキリして読みやすくなります。

---

## 問題 3: 特定の文字列を含む商品名を検索する
- **目的**: `WHERE` 句と `LIKE` 演算子（`%` ワイルドカード）を使用して、部分一致検索を行う方法を理解する。

### 問題:
`products_mst` テーブルから、商品名(`product_name`)に **「ワイヤレス」** という文字列が含まれる商品を全て取得してください。

### 解答:
```sql
SELECT *
FROM products_mst
WHERE product_name LIKE '%ワイヤレス%';
```

---

## 問題 4: 特定の条件を満たす顧客と注文を組み合わせる
- **目的**: 複数の `WHERE` 条件を `AND` や `OR` で組み合わせ、結果を `ORDER BY` で並び替える複合的なクエリを作成する。

### 問題:
`customers_mst` テーブルから、以下の条件を全て満たす顧客の情報を取得してください。

1. 登録日が **2023年3月1日以降**
2. メールアドレスに **「example.com」** が含まれる

結果は **登録日が新しい順** に表示してください。

### 解答:
```sql
SELECT customer_name, email, created_date
FROM customers_mst
WHERE created_date >= '2023-03-01' 
  AND email LIKE '%example.com%'
ORDER BY created_date DESC;
```

---

## 問題 5: 特定の件数のみ取得する (LIMIT)
- **目的**: `LIMIT` 句を使用して、取得する行の数を制限する方法を理解する。

### 問題:
`products_mst` テーブルから、価格が高い順に並べた際の **上位 3 件** の商品名と価格を取得してください。

### 解答:
```sql
SELECT product_name, price
FROM products_mst
ORDER BY price DESC
LIMIT 3;
```

---

## 問題 6: 特定の開始位置からデータを取得する (OFFSET)
- **目的**: `OFFSET` 句と `LIMIT` 句を組み合わせて、特定の開始位置からデータを取得する方法（ページネーションの基礎）を理解する。

### 問題:
`products_mst` テーブルから、価格が高い順に並べた際に、**4 番目から 2 件** （つまり、4位と5位）の商品名と価格を取得してください。

### 解答:
```sql
SELECT product_name, price
FROM products_mst
ORDER BY price DESC
LIMIT 2 OFFSET 3;
```
> **解説**: `OFFSET 3` は「最初の3件を飛ばす」という意味なので、結果として4件目からデータが取得されます。

---

## 問題 7: LIKE 演算子とワイルドカードの応用
- **目的**: ワイルドカード（`_` アンダースコア）を含めた `LIKE` 演算子のパターンマッチング能力を深める。

### 問題:
`products_mst` テーブルから、商品名の **2文字目が「ー」（長音記号）** である商品を全て取得してください。
（例：「コーヒー」など）

### 解答:
```sql
SELECT *
FROM products_mst
WHERE product_name LIKE '_ー%';
```
> **解説**: `%` は0文字以上の任意の文字列を表しますが、`_` は「任意の1文字」を表します。

---

## 問題 8: 特定期間の顧客の登録情報検索
- **目的**: 日付型データの範囲検索と並び替えを正確に行う。

### 問題:
`customers_mst` テーブルから、**2023年3月1日 ～ 2023年7月31日** の間に登録された顧客の、顧客名と登録日を取得してください。
結果は **登録日が古い順** に表示してください。

### 解答:
```sql
SELECT customer_name, created_date
FROM customers_mst
WHERE created_date BETWEEN '2023-03-01' AND '2023-07-31'
ORDER BY created_date ASC;
```

---

## 問題 9: 特定のカテゴリに属さない商品の在庫数を調整する
- **目的**: `NOT IN` 演算子を使用して、指定した複数の条件に合致「しない」行を更新対象とする。

### 問題:
`products_mst` テーブルで、カテゴリが **'Electronics' と 'Books' 以外** の商品の在庫数(`stock_quantity`)を、現在の値から **10個 増加** させてください。

### 解答:
```sql
UPDATE products_mst
SET stock_quantity = stock_quantity + 10
WHERE category NOT IN ('Electronics', 'Books');
```

---

## 問題 10: 特定の顧客グループの登録日を今日の最新日付に更新する
- **目的**: 特定の ID リストを `IN` 句で指定し、日付型カラムをシステム日付関数（`CURRENT_DATE`）で更新する。

### 問題:
`customers_mst` テーブルで、**customer_id が 2, 5, 7** の顧客の登録日(`created_date`)を、**今日の最新日付** に更新してください。

### 解答:
```sql
UPDATE customers_mst
SET created_date = CURRENT_DATE
WHERE customer_id IN (2, 5, 7);
```

---

## 問題 11: 特定のキーワードを含む商品の価格を再設定する
- **目的**: `LIKE` 演算子を `WHERE` 句で使用して、文字列の部分一致で更新対象を絞り込む。

### 問題:
`products_mst` テーブルで、商品名に **「コーヒー」または「はちみつ」** という文字列が含まれる商品の価格を、一律 **1,500.00 円** に更新してください。

### 解答:
```sql
UPDATE products_mst
SET price = 1500.00
WHERE product_name LIKE '%コーヒー%' 
   OR product_name LIKE '%はちみつ%';
```
> **注意**: `WHERE product_name LIKE '%コーヒー%' OR '%はちみつ%'` と書くのは間違いです。`OR` の後ろにも完全な条件式を書く必要があります。

---

## 問題 12: 特定の商品名パターンに合致する商品を削除する
- **目的**: `LIKE` 演算子を `WHERE` 句で使用して、文字列の部分一致で削除対象を絞り込む。

### 問題:
`products_mst` テーブルから、商品名に **「充電器」** という文字列が含まれる商品を全て削除してください。

※なお、外部キー制約がある場合、本来は子テーブルから削除する必要がありますが、ここでは `WHERE` 句の書き方の学習として、商品テーブルに対する削除文のみ記述してください。

### 解答:
```sql
DELETE FROM products_mst
WHERE product_name LIKE '%充電器%';
```