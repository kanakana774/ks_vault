### ⚠️ 重要：学習上の注意
本問題集では、学習目的で `DELETE` 文による**物理削除**（レコードを完全に消す操作）を行います。
しかし、実際のテーブルには**外部キー制約**が設定されているため、親テーブル（顧客や商品）をいきなり削除しようとしてもエラーになります。

**「子テーブル（参照している側）から先に削除する」**というデータベースの整合性を保つためのルールを意識して解答してください。

---

## 問題 1: 特定の商品を削除する
- **目的**: 外部キー制約がある場合の基本的な削除手順（子 → 親）を理解する。

### 問題:
`products_mst` テーブルから、商品名が **「セラミックフライパン」** の情報を削除してください。

### 解答:

**❌ よくある間違い**
いきなり商品テーブルから削除しようとするとエラーになります。
```sql
DELETE FROM products_mst WHERE product_name = 'セラミックフライパン';
-- エラー: 注文明細テーブル(order_details_trn)で参照されているため削除できません
```

**⭕️ 正しい手順 (模範解答)**
まず、この商品が含まれている注文明細を削除し、その後に商品を削除します。

```sql
-- 1. 注文明細(子)を削除（サブクエリを使用する例）
DELETE FROM order_details_trn
WHERE product_id = (SELECT product_id FROM products_mst WHERE product_name = 'セラミックフライパン');

-- 2. 商品(親)を削除
DELETE FROM products_mst
WHERE product_name = 'セラミックフライパン';
```

> **補足**: 実務では物理削除を行わず、`deleted_at` に日時を入れる「論理削除」を行うのが一般的です。

---

## 問題 2: 在庫がゼロの商品を全て削除する
- **目的**: 条件に合致する複数の行を、整合性を保ちながら一括削除する方法を理解する。

### 問題:
`products_mst` テーブルから、在庫数(`stock_quantity`)が **0 個** の商品を全て削除してください。

### 解答:

**⭕️ 正しい手順 (模範解答)**
		在庫0の商品IDを特定し、明細 → 商品の順で削除します。

```sql
-- 1. 在庫0の商品が含まれる注文明細を削除
DELETE FROM order_details_trn
WHERE product_id IN (SELECT product_id FROM products_mst WHERE stock_quantity = 0);

-- 2. 在庫0の商品自体を削除
DELETE FROM products_mst
WHERE stock_quantity = 0;
```

---

## 問題 3: 特定のカテゴリに属する商品をまとめて削除する
- **目的**: `WHERE` 句でカテゴリを指定し、関連する複数の子レコードと親レコードを処理する。

### 問題:
`products_mst` テーブルから、カテゴリ(`category`)が **'Food'** の全ての商品を削除してください。

### 解答:

**⭕️ 正しい手順 (模範解答)**

```sql
-- 1. 'Food'カテゴリ商品の注文明細を削除
DELETE FROM order_details_trn
WHERE product_id IN (SELECT product_id FROM products_mst WHERE category = 'Food');

-- 2. 'Food'カテゴリの商品を削除
DELETE FROM products_mst
WHERE category = 'Food';
```

---

## 問題 4: 長期間利用がない顧客を削除する
- **目的**: 3階層のテーブル（明細 → 注文 → 顧客）の削除順序と、日付比較を理解する。

### 問題:
`customers_mst` テーブルから、**2023年3月1日より前** に登録された顧客の情報を削除してください。

### 解答:
顧客(`customers_mst`)を消すには、その顧客の注文(`orders_trn`)を消す必要があり、注文を消すには注文明細(`order_details_trn`)を消す必要があります。

**⭕️ 正しい手順 (模範解答)**
最も深い階層（孫テーブル）から順に削除します。

```sql
-- 対象: 2023-03-01 より前の顧客 (customer_id: 1, 2 が該当)

-- 1. [孫] 対象顧客の注文に紐づく「注文明細」を削除
DELETE FROM order_details_trn
WHERE order_id IN (
    SELECT order_id 
    FROM orders_trn 
    WHERE customer_id IN (SELECT customer_id FROM customers_mst WHERE created_date < '2023-03-01')
);

-- 2. [子] 対象顧客の「注文」を削除
DELETE FROM orders_trn
WHERE customer_id IN (SELECT customer_id FROM customers_mst WHERE created_date < '2023-03-01');

-- 3. [親] 「顧客」自体を削除
DELETE FROM customers_mst
WHERE created_date < '2023-03-01';
```

---

## 問題 5: メモが設定されていない商品を削除する
- **目的**: `IS NULL` を用いた削除条件と、リレーションの解消。

### 問題:
`products_mst` テーブルから、メモ(`memo`)が **NULL** である商品を全て削除してください。

### 解答:

**⭕️ 正しい手順 (模範解答)**

```sql
-- 1. メモがNULLの商品の注文明細を削除
DELETE FROM order_details_trn
WHERE product_id IN (SELECT product_id FROM products_mst WHERE memo IS NULL);

-- 2. メモがNULLの商品を削除
DELETE FROM products_mst
WHERE memo IS NULL;
```

---

## 問題 6: 特定の顧客IDに関連するデータを削除する
- **目的**: 特定のIDを指定して、関連データを手動でクリーンアップする手順を確実に遂行する能力を養う。

### 問題:
**customer_id が 4** の顧客（山田 恵美）について、以下の手順でデータを削除してください。
1. この顧客が購入した全ての「注文明細」
2. この顧客の「注文履歴」
3. この顧客の「顧客情報」

### 解答:

**⭕️ 正しい手順 (模範解答)**
サブクエリを使わず、IDを指定して確実に消していく手順です。

```sql
-- 1. まず、削除対象の顧客に関連する注文の order_id を確認（実務的な手順）
-- SELECT order_id FROM orders_trn WHERE customer_id = 4;
-- 結果: 5 が該当すると確認できたと仮定

-- 2. 注文明細(order_details_trn)を削除
DELETE FROM order_details_trn
WHERE order_id IN (SELECT order_id FROM orders_trn WHERE customer_id = 4);
-- または確認したIDを使って: DELETE FROM order_details_trn WHERE order_id = 5;

-- 3. 注文(orders_trn)を削除
DELETE FROM orders_trn
WHERE customer_id = 4;

-- 4. 顧客(customers_mst)を削除
DELETE FROM customers_mst
WHERE customer_id = 4;
```

> **補足**: `ON DELETE CASCADE` オプションが設定されているテーブルであれば、親を消すだけで子も自動で消えますが、危険な操作になりうるため、意図して手動削除を行うケースも多々あります。

---

## 問題 7: 【極めて危険！】 全ての注文データを削除する
- **目的**: `WHERE` 句なしの `DELETE` の危険性と、全件削除時における外部キー制約の影響を理解する。

### 問題:
`orders_trn` テーブルの **全ての注文情報** を削除してください。
※実務環境では絶対に行わないでください。

### 解答:

**❌ 実行できない例**
```sql
DELETE FROM orders_trn;
-- エラー: 注文明細(order_details_trn)に残っているデータがあるため削除できません。
```

**⭕️ 正しい手順 (模範解答)**
全件削除であっても、参照されている子テーブルから消す必要があります。

```sql
-- 1. 注文明細を全て削除
DELETE FROM order_details_trn;

-- 2. 注文を全て削除
DELETE FROM orders_trn;
```

> **レビュー時の指導ポイント**:
> `DELETE` は物理的にデータを消してしまうため、復元が困難です。実務では `deleted_at` カラムに日付を入れる `UPDATE` 文（論理削除）を使うことがほとんどであることを併せて指導すること！